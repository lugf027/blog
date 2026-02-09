package io.github.lugf027.blog.controller

import io.github.lugf027.blog.entity.BlogImage
import io.github.lugf027.blog.service.ImageService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/api")
class ImageController(
    private val imageService: ImageService
) {
    @PostMapping("/upload")
    fun uploadImage(@RequestParam("file") file: MultipartFile): ResponseEntity<Map<String, Any>> {
        return try {
            val image = imageService.uploadImage(file)
            val response = mapOf(
                "success" to true,
                "url" to image.filePath,
                "id" to (image.id ?: 0L),
            )
            ResponseEntity.ok(response)
        } catch (e: IOException) {
            val response = mapOf(
                "success" to false,
                "message" to "Upload failed: ${e.message}"
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * 从 URL 下载图片并上传到服务器
     * 请求体: { "url": "https://example.com/image.jpg" }
     */
    @PostMapping("/upload/from-url")
    fun uploadImageFromUrl(@RequestBody request: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val imageUrl = request["url"] ?: return ResponseEntity.badRequest().body(
            mapOf("success" to false, "message" to "缺少 url 参数")
        )
        
        return try {
            val image = imageService.uploadImageFromUrl(imageUrl)
            val response = mapOf(
                "success" to true,
                "url" to image.filePath,
                "id" to (image.id ?: 0L),
                "originalUrl" to imageUrl
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            val response = mapOf(
                "success" to false,
                "message" to (e.message ?: "参数错误")
            )
            ResponseEntity.badRequest().body(response)
        } catch (e: Exception) {
            val response = mapOf(
                "success" to false,
                "message" to "下载图片失败: ${e.message}"
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }
    
    /**
     * 批量从 URL 下载图片并上传到服务器
     * 请求体: { "urls": ["https://example.com/image1.jpg", "https://example.com/image2.jpg"] }
     */
    @PostMapping("/upload/from-urls")
    fun uploadImagesFromUrls(@RequestBody request: Map<String, List<String>>): ResponseEntity<Map<String, Any>> {
        val imageUrls = request["urls"] ?: return ResponseEntity.badRequest().body(
            mapOf("success" to false, "message" to "缺少 urls 参数")
        )
        
        if (imageUrls.isEmpty()) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "urls 列表不能为空")
            )
        }
        
        // 限制批量上传数量
        if (imageUrls.size > 20) {
            return ResponseEntity.badRequest().body(
                mapOf("success" to false, "message" to "单次最多上传 20 张图片")
            )
        }
        
        return try {
            val result = imageService.uploadImagesFromUrls(imageUrls)
            val response = mapOf(
                "success" to true,
                "mappings" to result.mappings,
                "failed" to result.failed,  // 返回下载失败的 URL 列表
                "total" to imageUrls.size,
                "uploaded" to result.mappings.count { it.key != it.value }
            )
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            val response = mapOf(
                "success" to false,
                "message" to "批量下载图片失败: ${e.message}"
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
        }
    }

    @GetMapping("/images/{filename}")
    fun getImage(@PathVariable filename: String): ResponseEntity<ByteArray> {
        return try {
            val image = imageService.getImage(filename)
            val headers = HttpHeaders()
            
            // Determine content type based on file extension
            val contentType = when {
                filename.lowercase().endsWith(".png") -> "image/png"
                filename.lowercase().endsWith(".gif") -> "image/gif"
                filename.lowercase().endsWith(".webp") -> "image/webp"
                else -> "image/jpeg"
            }
            
            headers.contentType = MediaType.parseMediaType(contentType)
            ResponseEntity(image, headers, HttpStatus.OK)
        } catch (e: IOException) {
            ResponseEntity.notFound().build()
        }
    }
}
