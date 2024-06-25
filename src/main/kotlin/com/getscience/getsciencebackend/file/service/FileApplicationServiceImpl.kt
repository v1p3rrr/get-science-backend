package com.getscience.getsciencebackend.file.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.application.repository.ApplicationRepository
import com.getscience.getsciencebackend.file.data.dto.FileApplicationRequest
import com.getscience.getsciencebackend.file.data.dto.FileApplicationResponse
import com.getscience.getsciencebackend.file.data.model.FileApplication
import com.getscience.getsciencebackend.file.repository.FileApplicationRepository
import com.getscience.getsciencebackend.util.encryption.DecryptionService
import mu.KotlinLogging
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.net.URL

@Service
@Transactional
class FileApplicationServiceImpl(
    private val fileApplicationRepository: FileApplicationRepository,
    private val applicationRepository: ApplicationRepository,
    private val s3Service: S3Service,
    private val decryptionService: DecryptionService
) : FileApplicationService {

    val log = KotlinLogging.logger {}

    override  fun uploadFiles(
        files: List<MultipartFile>,
        application: Application,
        isEncryptionEnabled: Boolean
    ): List<FileApplication> {
        if (files.isEmpty()) {
            throw IllegalArgumentException("No files to upload")
        }

        val fileApplications = s3Service.uploadFiles(files, application, isEncryptionEnabled)
        return fileApplicationRepository.saveAll(fileApplications)
    }

    @CachePut(value = ["fileApplications"], key = "#result.fileId", condition = "#result != null")
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

    @Cacheable(value = ["fileApplicationsByApplication"], key = "#applicationId", condition = "#result != null")
    override fun getFileApplicationsByApplication(applicationId: Long, email: String): List<FileApplicationResponse> {
        val application =
            applicationRepository.findById(applicationId).orElseThrow { RuntimeException("Application not found") }

        if (application.profile.account.email != email && application.event.organizer.account.email != email) {
            throw IllegalArgumentException("Unauthorized access")
        }
        val fileApplications = fileApplicationRepository.findByApplicationApplicationId(applicationId)
        return fileApplications.map { FileApplicationResponse.fromEntity(it) }
    }

    //todo deletefile

    override fun getPresignedUrlForFile(fileId: Long, email: String): URL {
        val fileApplication = verifyFileAccessRightsAndGetFileApplication(fileId, email)

        return s3Service.generatePresignedUrl(fileApplication.filePath)
    }

    override fun getApplication(fileId: Long, email: String): Application {
        verifyFileAccessRightsAndGetFileApplication(fileId, email)
        return applicationRepository.findByFileApplicationsFileId(fileId) ?: throw RuntimeException("File not found")
    }

    @Cacheable(value = ["fileApplicationsById"], key = "#fileId", condition = "#result != null")
    override fun getFileApplication(fileId: Long, email: String): FileApplication {
        verifyFileAccessRightsAndGetFileApplication(fileId, email)
        return fileApplicationRepository.findById(fileId).orElseThrow { RuntimeException("File not found") }
    }

    override  fun downloadFile(fileId: Long, email: String): ByteArray {
        val fileApplication = verifyFileAccessRightsAndGetFileApplication(fileId, email)

        var file = s3Service.downloadFile(fileApplication.filePath)

        if (fileApplication.isEncryptionEnabled) {
            file = decryptionService.decryptFile(file)
        }

        return file
    }

    fun verifyFileOwner(fileId: Long, email: String): FileApplication {
        val fileApplication =
            fileApplicationRepository.findById(fileId).orElseThrow { RuntimeException("File not found") }
        if (fileApplication.application.profile.account.email != email) {
            throw IllegalArgumentException("Unauthorized access")
        }
        return fileApplication;
    }

    fun verifyFileAccessRightsAndGetFileApplication(fileId: Long, email: String): FileApplication {
        val fileApplication =
            fileApplicationRepository.findById(fileId).orElseThrow { RuntimeException("File not found") }
        if (fileApplication.application.profile.account.email != email && fileApplication.application.event.organizer.account.email != email) {
            throw IllegalArgumentException("Unauthorized access")
        }
        return fileApplication;
    }
}