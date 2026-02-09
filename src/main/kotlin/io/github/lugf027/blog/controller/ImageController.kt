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
