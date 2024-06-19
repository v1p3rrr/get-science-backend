package com.getscience.getsciencebackend.file.controller

import com.getscience.getsciencebackend.file.data.dto.FileApplicationRequest
import com.getscience.getsciencebackend.file.data.dto.FileApplicationResponse
import com.getscience.getsciencebackend.file.service.FileApplicationService
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/file-applications")
class FileApplicationController(
    private val fileApplicationService: FileApplicationService,
) {

    @PostMapping
    fun createFileApplication(
        @RequestBody fileApplicationRequest: FileApplicationRequest,
        @RequestParam applicationId: Long
    ): ResponseEntity<FileApplicationResponse> {
        val email = getEmailFromToken()
        val fileApplication = fileApplicationService.createFileApplication(fileApplicationRequest, applicationId, email)
        return ResponseEntity.ok(FileApplicationResponse.fromEntity(fileApplication))
    }

    @GetMapping("/application/{applicationId}")
    fun getFileApplicationsByApplication(
        @PathVariable applicationId: Long
    ): ResponseEntity<List<FileApplicationResponse>> {
        val email = getEmailFromToken()
        val fileApplications = fileApplicationService.getFileApplicationsByApplication(applicationId, email)
        return ResponseEntity.ok(fileApplications)
    }

    @GetMapping("/{applicationId}/files/{fileId}/direct-link")
    fun getPresignedUrl(
        @PathVariable applicationId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<String> {
        val email = getEmailFromToken()
        val url = fileApplicationService.getPresignedUrlForFile(fileId, email)
        return ResponseEntity.ok(url.toString())
    }

    @GetMapping("/{applicationId}/files/{fileId}")
    fun downloadFile(
        @PathVariable applicationId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<ByteArrayResource> {
        val email = getEmailFromToken()
        val fileApplication = fileApplicationService.getFileApplication(fileId, email)
        val fileData = fileApplicationService.downloadFile(fileApplication.fileId, email)

        val resource = ByteArrayResource(fileData)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fileApplication.fileType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileApplication.fileName}\"")
            .body(resource)
    }

    private fun getEmailFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        return email
    }
}
