package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.file.data.model.FileApplication
import com.getscience.getsciencebackend.util.encryption.EncryptionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.file.Paths
import java.sql.Timestamp
import java.time.Duration
import java.util.*

@Service
class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val encryptionService: EncryptionService,
) {

    @Value("\${S3_BUCKET_NAME}")
    lateinit var bucketName: String

     fun uploadFiles(
        files: List<MultipartFile>,
        application: Application,
        isEncryptionEnabled: Boolean
    ): List<FileApplication> {
        return files.map { file ->
            val fileName = file.originalFilename ?: UUID.randomUUID().toString()
            val key = "applications/${application.applicationId}/${fileName}"
            val putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).build()

            val bytes = if (isEncryptionEnabled) encryptionService.encryptBytes(file.bytes) else file.bytes
            val requestUrl = s3Client.utilities().getUrl { it.bucket(bucketName).key(key) }

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes))

            val fileApplication = FileApplication(
                fileName = fileName,
                filePath = key,
                uploadDate = Timestamp(System.currentTimeMillis()),
                fileType = file.contentType ?: "",
                isEncryptionEnabled = isEncryptionEnabled,
                application = application
            )

            fileApplication
        }

    }

    fun generatePresignedUrl(fileKey: String): URL {
        val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(fileKey).build()

        val getObjectPresignRequest = GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(getObjectRequest).build()

        return s3Presigner.presignGetObject(getObjectPresignRequest).url()
    }

     fun downloadFile(fileKey: String): ByteArray  {
        val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(fileKey).build()

        val byteArrayOutputStream = ByteArrayOutputStream()
        s3Client.getObject(getObjectRequest, ResponseTransformer.toOutputStream(byteArrayOutputStream))
        return byteArrayOutputStream.toByteArray()
    }
}