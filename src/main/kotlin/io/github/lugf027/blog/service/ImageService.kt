package io.github.lugf027.blog.service

import io.github.lugf027.blog.entity.BlogImage
import io.github.lugf027.blog.repository.BlogImageRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.*

@Service
class ImageService(
    private val blogImageRepository: BlogImageRepository,
    @Value("\${upload.image.path:uploads/images/}")
    private val uploadDir: String
) {
    // 支持的图片扩展名
    private val supportedExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp")
    
    // 从 Content-Type 映射到文件扩展名
    private val contentTypeToExtension = mapOf(
        "image/jpeg" to ".jpg",
        "image/jpg" to ".jpg",
        "image/png" to ".png",
        "image/gif" to ".gif",
        "image/webp" to ".webp"
    )
    
    // 图片文件头魔数（Magic Number）
    // JPEG: FF D8 FF
    // PNG: 89 50 4E 47 0D 0A 1A 0A
    // GIF: 47 49 46 38 (GIF8)
    // WebP: 52 49 46 46 ... 57 45 42 50 (RIFF...WEBP)
    private val jpegMagic = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val pngMagic = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val gifMagic = byteArrayOf(0x47, 0x49, 0x46, 0x38)  // GIF8
    private val webpRiffMagic = byteArrayOf(0x52, 0x49, 0x46, 0x46)  // RIFF
    private val webpWebpMagic = byteArrayOf(0x57, 0x45, 0x42, 0x50)  // WEBP
    
    /**
     * 获取规范化的上传目录路径（确保以 / 结尾）
     */
    private fun getUploadDirPath(): String {
        return if (uploadDir.endsWith("/") || uploadDir.endsWith("\\")) {
            uploadDir
        } else {
            "$uploadDir/"
        }
    }
    
    /**
     * 获取指定文章的图片存储目录路径
     * @param postId 文章ID
     */
    private fun getPostImageDirPath(postId: Long): String {
        return "${getUploadDirPath()}$postId/"
    }
    
    /**
     * 通过文件头魔数验证图片有效性，并返回检测到的图片类型
     * @param imageBytes 图片字节数组
     * @return 图片类型 (如 "image/jpeg") 或 null 如果不是有效图片
     */
    private fun detectImageType(imageBytes: ByteArray): String? {
        if (imageBytes.size < 12) return null
        
        // 检查 JPEG
        if (imageBytes.size >= 3 && 
            imageBytes[0] == jpegMagic[0] && 
            imageBytes[1] == jpegMagic[1] && 
            imageBytes[2] == jpegMagic[2]) {
            return "image/jpeg"
        }
        
        // 检查 PNG
        if (imageBytes.size >= 8) {
            var isPng = true
            for (i in pngMagic.indices) {
                if (imageBytes[i] != pngMagic[i]) {
                    isPng = false
                    break
                }
            }
            if (isPng) return "image/png"
        }
        
        // 检查 GIF
        if (imageBytes.size >= 4) {
            var isGif = true
            for (i in gifMagic.indices) {
                if (imageBytes[i] != gifMagic[i]) {
                    isGif = false
                    break
                }
            }
            if (isGif) return "image/gif"
        }
        
        // 检查 WebP (RIFF....WEBP)
        if (imageBytes.size >= 12) {
            var isRiff = true
            for (i in webpRiffMagic.indices) {
                if (imageBytes[i] != webpRiffMagic[i]) {
                    isRiff = false
                    break
                }
            }
            if (isRiff) {
                var isWebp = true
                for (i in webpWebpMagic.indices) {
                    if (imageBytes[8 + i] != webpWebpMagic[i]) {
                        isWebp = false
                        break
                    }
                }
                if (isWebp) return "image/webp"
            }
        }
        
        return null
    }
    
    /**
     * 验证图片是否完整有效
     * @param imageBytes 图片字节数组
     * @param imageType 图片类型
     * @return true 如果图片有效
     */
    private fun validateImageIntegrity(imageBytes: ByteArray, imageType: String): Boolean {
        // 基本大小检查
        if (imageBytes.size < 100) return false
        
        when (imageType) {
            "image/jpeg" -> {
                // JPEG 文件应该以 FF D9 结尾（EOI marker）
                if (imageBytes.size >= 2) {
                    val lastTwo = imageBytes.takeLast(2)
                    if (lastTwo[0] == 0xFF.toByte() && lastTwo[1] == 0xD9.toByte()) {
                        return true
                    }
                    // 有些 JPEG 文件可能在 EOI 后面有额外数据，向前查找
                    for (i in imageBytes.size - 2 downTo maxOf(0, imageBytes.size - 100)) {
                        if (imageBytes[i] == 0xFF.toByte() && imageBytes[i + 1] == 0xD9.toByte()) {
                            return true
                        }
                    }
                }
                return false
            }
            "image/png" -> {
                // PNG 文件应该以 IEND chunk 结尾: 00 00 00 00 49 45 4E 44 AE 42 60 82
                if (imageBytes.size >= 12) {
                    val iendSignature = byteArrayOf(
                        0x49, 0x45, 0x4E, 0x44,  // IEND
                        0xAE.toByte(), 0x42, 0x60, 0x82.toByte()  // CRC
                    )
                    val lastEight = imageBytes.takeLast(8).toByteArray()
                    return lastEight.contentEquals(iendSignature)
                }
                return false
            }
            "image/gif" -> {
                // GIF 文件应该以 0x3B (;) 结尾
                if (imageBytes.isNotEmpty()) {
                    return imageBytes.last() == 0x3B.toByte()
                }
                return false
            }
            "image/webp" -> {
                // WebP 只检查魔数，完整性较难验证
                return imageBytes.size >= 12
            }
            else -> return false
        }
    }

    fun uploadImage(file: MultipartFile, postId: Long): BlogImage {
        val dirPath = getPostImageDirPath(postId)
        
        // Create upload directory if not exists
        val directory = File(dirPath)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        // Generate unique filename
        val originalFilename = file.originalFilename ?: "image"
        val extension = originalFilename.substring(originalFilename.lastIndexOf("."))
        val filename = "${UUID.randomUUID()}$extension"
        
        // Save file
        val filePath = Paths.get(dirPath + filename)
        Files.write(filePath, file.bytes)

        // Save to database
        val image = BlogImage(
            fileName = originalFilename,
            filePath = "/api/images/$postId/$filename",
            fileSize = file.size,
            contentType = file.contentType
        )

        return blogImageRepository.save(image)
    }

    /**
     * 从 URL 下载图片并保存到服务器
     * @param imageUrl 图片 URL
     * @param postId 文章ID
     * @return 保存后的 BlogImage 实体
     */
    fun uploadImageFromUrl(imageUrl: String, postId: Long): BlogImage {
        val dirPath = getPostImageDirPath(postId)
        
        // 验证 URL 格式
        val uri = try {
            URI.create(imageUrl)
        } catch (e: Exception) {
            throw IllegalArgumentException("无效的图片 URL: $imageUrl")
        }
        
        // 只允许 http 和 https 协议
        if (uri.scheme !in listOf("http", "https")) {
            throw IllegalArgumentException("只支持 http 和 https 协议")
        }
        
        // Create upload directory if not exists
        val directory = File(dirPath)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        // 创建 HTTP 客户端下载图片
        val client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build()
        
        val request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(30))
            .header("User-Agent", "Mozilla/5.0 (compatible; BlogImageFetcher/1.0)")
            .GET()
            .build()
        
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        
        if (response.statusCode() != 200) {
            throw RuntimeException("下载图片失败，HTTP 状态码: ${response.statusCode()}")
        }
        
        val imageBytes = response.body()
        if (imageBytes.isEmpty()) {
            throw RuntimeException("下载的图片内容为空")
        }
        
        // 限制图片大小为 10MB
        if (imageBytes.size > 10 * 1024 * 1024) {
            throw IllegalArgumentException("图片大小超过 10MB 限制")
        }
        
        // 通过文件头魔数检测真实的图片类型
        val detectedType = detectImageType(imageBytes)
            ?: throw RuntimeException("下载的内容不是有效的图片文件（无法识别的文件格式）")
        
        // 验证图片完整性
        if (!validateImageIntegrity(imageBytes, detectedType)) {
            throw RuntimeException("下载的图片文件不完整或已损坏")
        }
        
        // 使用检测到的真实图片类型确定扩展名
        val extension = contentTypeToExtension[detectedType] 
            ?: throw RuntimeException("不支持的图片类型: $detectedType")
        
        // 生成唯一文件名
        val filename = "${UUID.randomUUID()}$extension"
        
        // 保存文件
        val filePath = Paths.get(dirPath + filename)
        Files.write(filePath, imageBytes)
        
        // 从 URL 中提取原始文件名
        val originalFilename = getFilenameFromUrl(imageUrl) ?: "image$extension"
        
        // 保存到数据库
        val image = BlogImage(
            fileName = originalFilename,
            filePath = "/api/images/$postId/$filename",
            fileSize = imageBytes.size.toLong(),
            contentType = detectedType
        )
        
        return blogImageRepository.save(image)
    }
    
    /**
     * 批量从 URL 下载图片
     * @param imageUrls 图片 URL 列表
     * @param postId 文章ID
     * @return UploadResult 包含成功的映射和失败的URL列表
     */
    data class BatchUploadResult(
        val mappings: MutableMap<String, String> = mutableMapOf(),
        val failed: MutableList<String> = mutableListOf()
    )
    
    fun uploadImagesFromUrls(imageUrls: List<String>, postId: Long): BatchUploadResult {
        val result = BatchUploadResult()
        
        for (url in imageUrls) {
            try {
                val image = uploadImageFromUrl(url, postId)
                result.mappings[url] = image.filePath
            } catch (e: Exception) {
                // 记录失败的 URL，让前端可以尝试下载
                result.failed.add(url)
                result.mappings[url] = url  // 保留原 URL
            }
        }
        
        return result
    }
    
    /**
     * 从 URL 中提取文件扩展名
     */
    private fun getExtensionFromUrl(url: String): String? {
        val path = try {
            URI.create(url).path
        } catch (e: Exception) {
            return null
        }
        
        val lastDot = path.lastIndexOf(".")
        if (lastDot == -1) return null
        
        val extension = path.substring(lastDot).lowercase()
        // 去除可能的查询参数
        val cleanExtension = extension.split("?")[0].split("#")[0]
        
        return if (cleanExtension in supportedExtensions) cleanExtension else null
    }
    
    /**
     * 从 URL 中提取文件名
     */
    private fun getFilenameFromUrl(url: String): String? {
        val path = try {
            URI.create(url).path
        } catch (e: Exception) {
            return null
        }
        
        val lastSlash = path.lastIndexOf("/")
        if (lastSlash == -1) return null
        
        val filename = path.substring(lastSlash + 1)
        // 去除可能的查询参数
        return filename.split("?")[0].split("#")[0].ifEmpty { null }
    }

    fun getImage(postId: Long, filename: String): ByteArray {
        val dirPath = getPostImageDirPath(postId)
        val filePath = Paths.get(dirPath + filename)
        return Files.readAllBytes(filePath)
    }
}
