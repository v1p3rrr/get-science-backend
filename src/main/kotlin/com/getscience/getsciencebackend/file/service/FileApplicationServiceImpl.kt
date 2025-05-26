package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import com.getscience.getsciencebackend.application.data.dto.ApplicationFileMetadataDTO
import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.application.repository.ApplicationRepository
import com.getscience.getsciencebackend.event.repository.DocRequiredRepository
import com.getscience.getsciencebackend.file.data.dto.FileApplicationRequest
import com.getscience.getsciencebackend.file.data.dto.FileApplicationResponse
import com.getscience.getsciencebackend.file.data.model.FileApplication
import com.getscience.getsciencebackend.file.repository.FileApplicationRepository
import com.getscience.getsciencebackend.util.encryption.DecryptionService
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.net.URL
import java.sql.Timestamp

@Service
@Transactional
class FileApplicationServiceImpl(
    private val fileApplicationRepository: FileApplicationRepository,
    private val applicationRepository: ApplicationRepository,
    private val docRequiredRepository: DocRequiredRepository,
    private val s3Service: S3Service,
    private val decryptionService: DecryptionService
) : FileApplicationService {

    val log = KotlinLogging.logger {}

    @LogBusinessOperation(operationType = "APP_FILE_UPLOAD", description = "Загрузка файлов для заявки")
    @Transactional
    @CacheEvict(value = ["fileApplications", "fileApplicationsByApplication"], allEntries = true)
    override fun uploadFiles(
        files: List<MultipartFile>,
        application: Application,
        fileApplicationFileMetadataDTO: List<ApplicationFileMetadataDTO>
    ): List<FileApplication> {
        if (files.isEmpty()) {
            throw IllegalArgumentException("No files to upload")
        }
        val keyPrefix = "applications/${application.applicationId}"
        val fileNamesAndKeys =
            s3Service.uploadFiles(files, fileApplicationFileMetadataDTO.map { it.isEncrypted }, keyPrefix)

        val fileApplications: List<FileApplication> = fileNamesAndKeys.withIndex().map { (i, fileInfo) ->
            FileApplication(
                fileName = fileInfo.fileName,
                filePath = fileInfo.fileKey,
                uploadDate = Timestamp(System.currentTimeMillis()),
                fileType = files[i].contentType ?: "",
                isEncryptionEnabled = fileApplicationFileMetadataDTO[i].isEncrypted,
                docRequired = docRequiredRepository.findById(fileApplicationFileMetadataDTO[i].docRequiredId).orElse(null),
                application = application
            )
        }

        return fileApplicationRepository.saveAll(fileApplications)
    }

    @LogBusinessOperation(operationType = "APP_FILE_CREATE_METADATA", description = "Создание метаданных файла заявки")
    @CacheEvict(value = ["fileApplications", "fileApplicationsByApplication"], allEntries = true)
    @CachePut(value = ["fileApplications"], key = "#result.fileId", unless = "#result == null")
    override fun createFileApplication(
        fileApplicationRequest: FileApplicationRequest,
        applicationId: Long,
        email: String

    ): FileApplication {
        val application = applicationRepository.findById(applicationId).orElseThrow {
            RuntimeException("Application not found")
        }

        if (application.profile.account.email != email && application.event.organizer.account.email != email) {
            throw IllegalArgumentException("Unauthorized access")
        }

        val fileApplication = fileApplicationRequest.toEntity(application)
        return fileApplicationRepository.save(fileApplication)
    }

    @LogBusinessOperation(operationType = "APP_FILE_GET_BY_APPLICATION", description = "Получение файлов по ID заявки")
    @Cacheable(value = ["fileApplicationsByApplication"], key = "#applicationId")
    override fun getFileApplicationsByApplication(applicationId: Long, email: String): List<FileApplicationResponse> {
        val application =
            applicationRepository.findById(applicationId).orElseThrow { RuntimeException("Application not found") }

        if (application.profile.account.email != email && application.event.organizer.account.email != email) {
            throw IllegalArgumentException("Unauthorized access")
        }
        val fileApplications = fileApplicationRepository.findByApplicationApplicationId(applicationId)
        return fileApplications.map { FileApplicationResponse.fromEntity(it) }
    }

    @LogBusinessOperation(operationType = "APP_FILE_DELETE", description = "Удаление файла заявки")
    @Transactional
    @CacheEvict(value = ["fileApplications", "fileApplicationsByApplication"], allEntries = true)
    override fun deleteFileApplication(fileId: Long, email: String): Boolean {
        val fileApplication = verifyFileAccessRightsAndGetFileApplication(fileId, email)
        
        // Удаляем файл из S3
        val s3DeleteSuccess = s3Service.deleteFile(fileApplication.filePath)
        if (!s3DeleteSuccess) {
            log.warn("Failed to delete file from S3: ${fileApplication.filePath}")
        }
        
        // Удаляем запись из базы данных
        fileApplicationRepository.deleteById(fileId)
        
        return s3DeleteSuccess
    }
    
    @LogBusinessOperation(operationType = "APP_FILES_DELETE_BY_DOC_REQ", description = "Удаление файлов заявки по ID требуемого документа")
    @Transactional
    @CacheEvict(value = ["fileApplications", "fileApplicationsByApplication"], allEntries = true)
    override fun deleteFileApplicationsByDocRequiredId(applicationId: Long, docRequiredId: Long, email: String): Boolean {
        val application = applicationRepository.findById(applicationId)
            .orElseThrow { RuntimeException("Application not found") }
        
        if (application.profile.account.email != email && application.event.organizer.account.email != email) {
            throw IllegalArgumentException("Unauthorized access")
        }
        
        // Находим все файлы заявки с указанным docRequiredId
        val filesToDelete = application.fileApplications.filter { 
            it.docRequired != null && it.docRequired.docRequiredId == docRequiredId 
        }
        
        var allDeletedSuccessfully = true
        
        // Удаляем каждый файл
        filesToDelete.forEach { file ->
            val s3DeleteSuccess = s3Service.deleteFile(file.filePath)
            if (!s3DeleteSuccess) {
                log.warn("Failed to delete file from S3: ${file.filePath}")
                allDeletedSuccessfully = false
            }
            fileApplicationRepository.deleteById(file.fileId)
        }
        
        return allDeletedSuccessfully
    }

    @LogBusinessOperation(operationType = "APP_FILE_GET_PRESIGNED_URL", description = "Получение presigned URL для файла заявки")
    override fun getPresignedUrlForFile(fileId: Long, email: String): URL {
        val fileApplication = verifyFileAccessRightsAndGetFileApplication(fileId, email)

        return s3Service.generatePresignedUrl(fileApplication.filePath)
    }

    @LogBusinessOperation(operationType = "APP_FILE_GET_APPLICATION", description = "Получение заявки по ID файла")
    override fun getApplication(fileId: Long, email: String): Application {
        verifyFileAccessRightsAndGetFileApplication(fileId, email)
        return applicationRepository.findByFileApplicationsFileId(fileId) ?: throw RuntimeException("File not found")
    }

    @LogBusinessOperation(operationType = "APP_FILE_GET_METADATA", description = "Получение метаданных файла заявки по ID")
    @Cacheable(value = ["fileApplicationsById"], key = "#fileId")
    override fun getFileApplication(fileId: Long, email: String): FileApplication {
        verifyFileAccessRightsAndGetFileApplication(fileId, email)
        return fileApplicationRepository.findById(fileId).orElseThrow { RuntimeException("File not found") }
    }

    @LogBusinessOperation(operationType = "APP_FILE_DOWNLOAD", description = "Скачивание файла заявки")
    override  fun downloadFile(fileId: Long, email: String): ByteArray {
        val fileApplication = verifyFileAccessRightsAndGetFileApplication(fileId, email)

        var file = s3Service.downloadFile(fileApplication.filePath)

        if (fileApplication.isEncryptionEnabled) {
            file = decryptionService.decryptFile(file)
        }

        return file
    }

    /**
     * Проверяет права доступа пользователя к файлу заявки и возвращает метаданные файла.
     * Доступ разрешен только владельцу заявки и организатору мероприятия.
     * 
     * @param fileId идентификатор файла
     * @param email email пользователя для проверки прав доступа
     * @return метаданные файла, если доступ разрешен
     * @throws RuntimeException если файл не найден
     * @throws IllegalArgumentException если доступ запрещен
     */
    private fun verifyFileAccessRightsAndGetFileApplication(fileId: Long, email: String): FileApplication {
        val fileApplication =
            fileApplicationRepository.findById(fileId).orElseThrow { RuntimeException("File not found") }
        if (fileApplication.application.profile.account.email != email && fileApplication.application.event.organizer.account.email != email) {
            throw IllegalArgumentException("Unauthorized access")
        }
        return fileApplication
    }
}