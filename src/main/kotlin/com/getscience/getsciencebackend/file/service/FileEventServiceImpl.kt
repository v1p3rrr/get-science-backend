package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.data.model.FileEvent
import com.getscience.getsciencebackend.file.repository.FileEventRepository
import mu.KotlinLogging
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URL
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import org.springframework.cache.annotation.CacheEvict

@Service
@Transactional
class FileEventServiceImpl(
    private val fileEventRepository: FileEventRepository,
    private val s3Service: S3Service
) : FileEventService {

    private val logger = KotlinLogging.logger {}

    @LogBusinessOperation(operationType = "EVENT_FILE_CREATE_METADATA", description = "Создание метаданных файла мероприятия")
    @CacheEvict(value = ["fileEvents", "fileEventsByEvent"], allEntries = true)
    @CachePut(value = ["fileEvents"], key = "#result.id", unless = "#result == null")
    override fun createFileEvent(fileEventRequest: FileEventRequest, event: Event): FileEvent {
        val fileEvent = fileEventRequest.toEntity(event)
        return fileEventRepository.save(fileEvent)
    }

    @LogBusinessOperation(operationType = "EVENT_FILE_GET_BY_EVENT", description = "Получение файлов по ID мероприятия")
    @Cacheable(value = ["fileEventsByEvent"], key = "#eventId")
    override fun getFilesByEvent(eventId: Long): List<FileEventResponse> {
        val fileEvents = fileEventRepository.findByEventEventId(eventId)
        return fileEvents.map { FileEventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_FILE_GET_METADATA", description = "Получение метаданных файла мероприятия по ID")
    @Cacheable(value = ["fileEvents"], key = "#fileId")
    override fun getFileEventById(fileId: Long): FileEventResponse {
        val fileEvent = fileEventRepository.findById(fileId).orElseThrow {
            RuntimeException("FileEvent not found")
        }
        return FileEventResponse.fromEntity(fileEvent, s3Service)
    }

    @LogBusinessOperation(operationType = "EVENT_FILE_GET_PRESIGNED_URL", description = "Получение presigned URL для файла мероприятия")
    override fun getPresignedUrlForFile(fileId: Long): URL {
        val fileEvent = getFileEvent(fileId)
        return s3Service.generatePresignedUrl(fileEvent.filePath)
    }

    @LogBusinessOperation(operationType = "EVENT_FILE_DOWNLOAD", description = "Скачивание файла мероприятия")
    override fun downloadFile(fileId: Long): ByteArray {
        val fileEvent = getFileEvent(fileId)
        return s3Service.downloadFile(fileEvent.filePath)
    }

    override fun getFileEvent(fileId: Long): FileEvent {
        return fileEventRepository.findById(fileId).orElseThrow { RuntimeException("File not found") }
    }

    @LogBusinessOperation(operationType = "EVENT_FILES_UPLOAD", description = "Загрузка файлов для мероприятия")
    @Transactional
    @CacheEvict(value = ["fileEvents", "fileEventsByEvent"], allEntries = true)
    override fun uploadFiles(
        files: List<MultipartFile>,
        event: Event,
        fileEventRequestList: List<FileEventRequest>
    ): List<FileEvent> {
        if (files.isEmpty()) {
            throw IllegalArgumentException("No files to upload")
        }
        val keyPrefix = "events/${event.eventId}/materials"

        val fileNamesToSet = fileEventRequestList.withIndex().map { (i, fileRequest) -> fileRequest.fileName + getFileExtension(files[i].originalFilename) }.toList()

        val fileNamesAndKeys =
            s3Service.uploadFiles(files, fileEventRequestList.map { false }, keyPrefix, fileNamesToSet)

        val fileEvents: List<FileEvent> = fileNamesAndKeys.withIndex().map { (i, fileInfo) ->
            FileEvent(
                fileName = fileInfo.fileName,
                filePath = fileInfo.fileKey,
                uploadDate = Timestamp(System.currentTimeMillis()),
                fileType = files[i].contentType ?: "",
                fileKindName = fileEventRequestList[i].fileKindName,
                category = fileEventRequestList[i].category,
                description = fileEventRequestList[i].description,
                event = event
            )
        }

        return fileEventRepository.saveAll(fileEvents)
    }

    @LogBusinessOperation(operationType = "EVENT_FILE_DELETE", description = "Удаление файла мероприятия")
    @Transactional
    @CacheEvict(value = ["fileEvents", "fileEventsByEvent"], allEntries = true)
    override fun deleteFileEvent(fileId: Long, email: String): Boolean {
        val fileEvent = checkRightsAndGetFileEvent(fileId, email)

        // Удаляем файл из S3
        val s3DeleteSuccess = s3Service.deleteFile(fileEvent.filePath)
        if (!s3DeleteSuccess) {
            logger.warn("Failed to delete file from S3: ${fileEvent.filePath}")
        }

        // Удаляем запись из базы данных
        fileEventRepository.deleteById(fileId)

        return s3DeleteSuccess
    }

    @LogBusinessOperation(operationType = "EVENT_FILE_UPDATE_METADATA", description = "Обновление метаданных файла мероприятия")
    @Transactional
    @CacheEvict(value = ["fileEvents", "fileEventsByEvent"], allEntries = true)
    override fun updateFileEvent(fileId: Long, updatedFileRequest: FileEventRequest, email: String): FileEventResponse {
        val fileEvent = checkRightsAndGetFileEvent(fileId, email)

        var updatedFileEvent: FileEvent

        updatedFileEvent = fileEvent.copy(
            fileName = updatedFileRequest.fileName,
            category = updatedFileRequest.category,
            description = updatedFileRequest.description,
            fileKindName = updatedFileRequest.fileKindName)

        if (fileEvent.fileName != updatedFileRequest.fileName) {
            val newFilePath = s3Service.renameFile(fileEvent.filePath, updatedFileRequest.fileName)
            if (newFilePath != null) {
                updatedFileEvent = updatedFileEvent.copy(filePath = newFilePath)
            }
        }

        val savedFileEvent = fileEventRepository.save(updatedFileEvent)
        return FileEventResponse.fromEntity(savedFileEvent, s3Service)
    }

    private fun checkRightsAndGetFileEvent(fileId: Long, email: String): FileEvent {
        val fileEvent = fileEventRepository.findById(fileId).orElseThrow {
            RuntimeException("FileEvent not found")
        }

        val event = fileEvent.event
        val organizer = event.organizer

        if (organizer.account.email != email && !event.coowners.any { it.account.email == email }) {
            throw IllegalArgumentException("Unauthorized access")
        }
        return fileEvent
    }

    private fun getFileExtension(originalFilename: String?): String {
        if (originalFilename.isNullOrBlank()) return ""
        val index = originalFilename.lastIndexOf('.')
        return if (index != -1 && index < originalFilename.length - 1) {
            originalFilename.substring(index).lowercase()
        } else {
            ""
        }
    }

}
