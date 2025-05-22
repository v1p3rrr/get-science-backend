package com.getscience.getsciencebackend.file.controller

import com.getscience.getsciencebackend.file.data.dto.FileApplicationRequest
import com.getscience.getsciencebackend.file.data.dto.FileApplicationResponse
import com.getscience.getsciencebackend.file.service.FileApplicationService
import com.getscience.getsciencebackend.file.service.FileEventService
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/files")
class FileController(
    private val fileApplicationService: FileApplicationService,
    private val fileEventService: FileEventService
) {

    /**
     * Создает метаданные файла заявки.
     *
     * @param fileApplicationRequest данные о файле заявки
     * @param applicationId идентификатор заявки
     * @return метаданные созданного файла
     */
    @PostMapping("/applications")
    fun createFileApplication(
        @RequestBody fileApplicationRequest: FileApplicationRequest,
        @RequestParam applicationId: Long
    ): ResponseEntity<FileApplicationResponse> {
        val email = getEmailFromToken()
        val fileApplication = fileApplicationService.createFileApplication(fileApplicationRequest, applicationId, email)
        return ResponseEntity.ok(FileApplicationResponse.fromEntity(fileApplication))
    }

    /**
     * Получает список файлов для указанной заявки.
     *
     * @param applicationId идентификатор заявки
     * @return список файлов заявки
     */
    @GetMapping("/applications/{applicationId}")
    fun getFileApplicationsByApplication(
        @PathVariable applicationId: Long
    ): ResponseEntity<List<FileApplicationResponse>> {
        val email = getEmailFromToken()
        val fileApplications = fileApplicationService.getFileApplicationsByApplication(applicationId, email)
        return ResponseEntity.ok(fileApplications)
    }

    /**
     * Генерирует presigned URL для скачивания файла заявки.
     * Позволяет получить временную ссылку для прямого доступа к файлу в S3.
     *
     * @param fileId идентификатор файла
     * @return URL для скачивания файла
     */
    @GetMapping("/applications/download/{fileId}/direct-link")
    fun getPresignedUrlForApplicationFile(
        @PathVariable fileId: Long
    ): ResponseEntity<String> {
        val email = getEmailFromToken()
        val url = fileApplicationService.getPresignedUrlForFile(fileId, email)
        return ResponseEntity.ok(url.toString())
    }

    /**
     * Скачивает файл заявки.
     * В случае если файл зашифрован, он будет расшифрован перед отправкой.
     *
     * @param fileId идентификатор файла
     * @return содержимое файла
     */
    @GetMapping("/applications/download/{fileId}")
    fun downloadApplicationFile(
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

    /**
     * Генерирует presigned URL для скачивания файла мероприятия.
     * Позволяет получить временную ссылку для прямого доступа к файлу в S3.
     *
     * @param fileId идентификатор файла
     * @return URL для скачивания файла
     */
    @GetMapping("/events/download/{fileId}/direct-link")
    fun getPresignedUrlForEventFile(
        @PathVariable fileId: Long
    ): ResponseEntity<String> {
        val url = fileEventService.getPresignedUrlForFile(fileId)
        return ResponseEntity.ok(url.toString())
    }

    /**
     * Скачивает файл мероприятия.
     *
     * @param fileId идентификатор файла
     * @return содержимое файла
     */
    @GetMapping("/events/download/{fileId}")
    fun downloadEventFile(
        @PathVariable fileId: Long
    ): ResponseEntity<ByteArrayResource> {
        val fileEvent = fileEventService.getFileEvent(fileId)
        val fileData = fileEventService.downloadFile(fileEvent.fileId)

        val resource = ByteArrayResource(fileData)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fileEvent.fileType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileEvent.fileName}\"")
            .body(resource)
    }

    /**
     * Удаляет файл заявки.
     * Удаляет как метаданные файла из базы данных, так и сам файл из S3 хранилища.
     *
     * @param fileId идентификатор файла
     * @return результат операции удаления
     */
    @DeleteMapping("/applications/{fileId}")
    fun deleteFile(
        @PathVariable fileId: Long
    ): ResponseEntity<Boolean> {
        val email = getEmailFromToken()
        val result = fileApplicationService.deleteFileApplication(fileId, email)
        return ResponseEntity.ok(result)
    }

    /**
     * Удаляет все файлы заявки, связанные с указанным требуемым документом.
     *
     * @param applicationId идентификатор заявки
     * @param docRequiredId идентификатор требуемого документа
     * @return результат операции удаления
     */
    @DeleteMapping("/applications/{applicationId}/doc-required/{docRequiredId}")
    fun deleteFilesByDocRequiredId(
        @PathVariable applicationId: Long,
        @PathVariable docRequiredId: Long
    ): ResponseEntity<Boolean> {
        val email = getEmailFromToken()
        val result = fileApplicationService.deleteFileApplicationsByDocRequiredId(applicationId, docRequiredId, email)
        return ResponseEntity.ok(result)
    }

    /**
     * Извлекает email пользователя из текущего токена аутентификации.
     *
     * @return email пользователя
     */
    private fun getEmailFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        return email
    }
}
