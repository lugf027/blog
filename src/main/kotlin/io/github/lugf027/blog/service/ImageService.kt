package io.github.lugf027.blog.service

import io.github.lugf027.blog.entity.BlogImage
import io.github.lugf027.blog.repository.BlogImageRepository
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Service
class ImageService(
    private val blogImageRepository: BlogImageRepository
) {
    private val uploadDir = "uploads/images/"

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

    fun getImage(filename: String): ByteArray {
        val filePath = Paths.get(uploadDir + filename)
        return Files.readAllBytes(filePath)
    }
}
