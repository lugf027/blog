package io.github.lugf027.blog.service

import io.github.lugf027.blog.entity.BlogImage
import io.github.lugf027.blog.repository.BlogImageRepository
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
    private val blogImageRepository: BlogImageRepository
) {
    private val uploadDir = "uploads/images/"
    
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

    fun uploadImage(file: MultipartFile): BlogImage {
        // Create upload directory if not exists
        val directory = File(uploadDir)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        // Generate unique filename
        val originalFilename = file.originalFilename ?: "image"
        val extension = originalFilename.substring(originalFilename.lastIndexOf("."))
        val filename = "${UUID.randomUUID()}$extension"
        
        // Save file
        val filePath = Paths.get(uploadDir + filename)
        Files.write(filePath, file.bytes)

        // Save to database
        val image = BlogImage(
            fileName = originalFilename,
            filePath = "/api/images/$filename",
            fileSize = file.size,
            contentType = file.contentType
        )

        return blogImageRepository.save(image)
    }

    /**
     * 从 URL 下载图片并保存到服务器
     * @param imageUrl 图片 URL
     * @return 保存后的 BlogImage 实体
     */
    fun uploadImageFromUrl(imageUrl: String): BlogImage {
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
        val directory = File(uploadDir)
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
        
        // 获取 Content-Type 并确定扩展名
        val contentType = response.headers()
            .firstValue("Content-Type")
            .orElse("image/jpeg")
            .split(";")[0]
            .trim()
            .lowercase()
        
        val extension = contentTypeToExtension[contentType] 
            ?: getExtensionFromUrl(imageUrl) 
            ?: ".jpg"  // 默认使用 .jpg
        
        // 验证是否为支持的图片格式
        if (extension !in supportedExtensions) {
            throw IllegalArgumentException("不支持的图片格式: $extension")
        }
        
        // 生成唯一文件名
        val filename = "${UUID.randomUUID()}$extension"
        
        // 保存文件
        val filePath = Paths.get(uploadDir + filename)
        Files.write(filePath, imageBytes)
        
        // 从 URL 中提取原始文件名
        val originalFilename = getFilenameFromUrl(imageUrl) ?: "image$extension"
        
        // 保存到数据库
        val image = BlogImage(
            fileName = originalFilename,
            filePath = "/api/images/$filename",
            fileSize = imageBytes.size.toLong(),
            contentType = contentType
        )
        
        return blogImageRepository.save(image)
    }
    
    /**
     * 批量从 URL 下载图片
     * @param imageUrls 图片 URL 列表
     * @return UploadResult 包含成功的映射和失败的URL列表
     */
    data class BatchUploadResult(
        val mappings: MutableMap<String, String> = mutableMapOf(),
        val failed: MutableList<String> = mutableListOf()
    )
    
    fun uploadImagesFromUrls(imageUrls: List<String>): BatchUploadResult {
        val result = BatchUploadResult()
        
        for (url in imageUrls) {
            try {
                val image = uploadImageFromUrl(url)
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

    fun getImage(filename: String): ByteArray {
        val filePath = Paths.get(uploadDir + filename)
        return Files.readAllBytes(filePath)
    }
}
