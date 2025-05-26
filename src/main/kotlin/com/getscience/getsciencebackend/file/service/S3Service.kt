package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import com.getscience.getsciencebackend.file.data.dto.S3UploadResult
import com.getscience.getsciencebackend.util.encryption.EncryptionService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.ByteArrayOutputStream
import java.net.URL
import java.time.Duration
import java.util.*

/**
 * Сервис для взаимодействия с Amazon S3 хранилищем.
 * Предоставляет методы для загрузки, скачивания, удаления файлов в S3 бакете,
 * а также генерации временных ссылок для доступа к файлам.
 * Поддерживает шифрование файлов при загрузке.
 */
@Service
class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val encryptionService: EncryptionService
) {

    private val logger = KotlinLogging.logger {}

    @Value("\${S3_BUCKET_NAME}")
    lateinit var bucketName: String

    /**
     * Загружает несколько файлов в S3 хранилище.
     * Поддерживает шифрование файлов перед загрузкой.
     *
     * @param files список файлов для загрузки
     * @param encryptionRequirementsList список флагов, определяющих необходимость шифрования для каждого файла
     * @param keyPrefix префикс ключа для файлов в S3 (определяет путь/директорию)
     * @param fileNames опциональный список имен файлов, которые нужно использовать вместо оригинальных
     * @return список результатов загрузки с именами файлов и их ключами в S3
     */
    @LogBusinessOperation(operationType = "S3_FILES_UPLOAD", description = "Загрузка нескольких файлов в S3")
    fun uploadFiles(
        files: List<MultipartFile>,
        encryptionRequirementsList: List<Boolean>,
        keyPrefix: String,
        fileNames: List<String>? = null
    ): List<S3UploadResult> {
        return files.withIndex().map { (i, file) ->
            val fileName = fileNames?.get(i) ?: file.originalFilename ?: UUID.randomUUID().toString()
            val key = "${keyPrefix}/${fileName}"
            val putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).build()

            val bytes = if (encryptionRequirementsList[i]) encryptionService.encryptBytes(file.bytes) else file.bytes

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes))

            S3UploadResult(fileName, key)
        }
    }

    /**
     * Генерирует предподписанный URL для временного доступа к файлу в S3.
     * При необходимости добавляет заголовок Content-Disposition для скачивания файла с указанным именем.
     *
     * @param fileKey ключ файла в S3
     * @param originalFileName опциональное оригинальное имя файла, которое будет использовано при скачивании
     * @return временный URL для доступа к файлу (действителен 10 минут)
     */
    @LogBusinessOperation(operationType = "S3_GENERATE_PRESIGNED_URL", description = "Генерация presigned URL для файла в S3")
    fun generatePresignedUrl(fileKey: String, originalFileName: String? = null): URL {
        // Добавляем параметры запроса для Content-Disposition
        val getObjectRequestBuilder = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(fileKey)
        
        // Если указано имя файла, добавляем заголовок Content-Disposition через параметры запроса
        if (!originalFileName.isNullOrBlank()) {
            val sanitizedFilename = sanitizeFileName(originalFileName)


            // В SDK v2 заголовки ответа задаются через query параметры в URL
            getObjectRequestBuilder.responseContentDisposition("attachment; filename=\"$sanitizedFilename\"")
        }
        
        val getObjectRequest = getObjectRequestBuilder.build()
        
        val getObjectPresignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(getObjectPresignRequest).url()
    }

    /**
     * Скачивает файл из S3 хранилища.
     *
     * @param fileKey ключ файла в S3
     * @return содержимое файла в виде массива байтов
     */
    @LogBusinessOperation(operationType = "S3_FILE_DOWNLOAD", description = "Скачивание файла из S3")
    fun downloadFile(fileKey: String): ByteArray  {
        val getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(fileKey).build()

        val byteArrayOutputStream = ByteArrayOutputStream()
        s3Client.getObject(getObjectRequest, ResponseTransformer.toOutputStream(byteArrayOutputStream))
        return byteArrayOutputStream.toByteArray()
    }

    /**
     * Загружает аватар пользователя в S3 хранилище.
     *
     * @param file файл аватара
     * @param profileId идентификатор профиля пользователя
     * @return ключ загруженного файла в S3
     */
    @LogBusinessOperation(operationType = "S3_AVATAR_UPLOAD", description = "Загрузка аватара в S3")
    fun uploadAvatar(file: MultipartFile, profileId: Long): String {
        val fileName = "${UUID.randomUUID()}_${file.originalFilename ?: "avatar"}"
        val key = "avatars/$profileId/$fileName"
        val putObjectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).build()

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.bytes))
        
        return key
    }

    /**
     * Загружает материал мероприятия в S3 хранилище.
     *
     * @param file файл материала
     * @param eventId идентификатор мероприятия
     * @param fileName имя файла
     * @return ключ загруженного файла в S3
     */
    @LogBusinessOperation(operationType = "S3_EVENT_MATERIAL_UPLOAD", description = "Загрузка материала мероприятия в S3")
    fun uploadEventMaterial(file: MultipartFile, eventId: Long, fileName: String): String {
        val originalFileName = file.originalFilename ?: fileName
        
        val fileExtension = getFileExtension(originalFileName, file.contentType)
        
        val sanitizedFileName = sanitizeFileName(originalFileName)
        val key = "events/$eventId/materials/${UUID.randomUUID()}-$sanitizedFileName"
        
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build()

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.bytes))
        return key
    }

    /**
     * Получает расширение файла из имени или типа содержимого.
     *
     * @param fileName имя файла
     * @param contentType тип содержимого файла
     * @return расширение файла или null, если его невозможно определить
     */
    private fun getFileExtension(fileName: String, contentType: String?): String? {
        val extFromName = fileName.substringAfterLast('.', "")
        if (extFromName.isNotBlank()) {
            return extFromName
        }
        
        return when (contentType) {
            "video/mp4" -> "mp4"
            "application/pdf" -> "pdf"
            "application/msword" -> "doc"
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
            "application/vnd.ms-excel" -> "xls"
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            else -> null
        }
    }

    /**
     * Очищает имя файла от недопустимых символов.
     *
     * @param fileName имя файла для очистки
     * @return очищенное имя файла
     */
    private fun sanitizeFileName(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0 && lastDot < fileName.length - 1) {
            val name = fileName.substring(0, lastDot)
            val ext = fileName.substring(lastDot)
            name.replace("[^A-Za-z0-9._-]".toRegex(), "_") + ext.replace("[^A-Za-z0-9.]".toRegex(), "")
        } else {
            fileName.replace("[^A-Za-z0-9._-]".toRegex(), "_")
        }
    }

    /**
     * Удаляет файл из S3 хранилища.
     *
     * @param fileKey ключ файла в S3
     * @return true, если файл успешно удален, иначе false
     */
    @LogBusinessOperation(operationType = "S3_FILE_DELETE", description = "Удаление файла из S3")
    fun deleteFile(fileKey: String): Boolean {
        return try {
            logger.info("Deleting file from S3: $fileKey")
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build()
            
            s3Client.deleteObject(deleteObjectRequest)
            logger.info("Successfully deleted file from S3: $fileKey")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete file from S3: $fileKey", e)
            false
        }
    }

    /**
     * ТЕХНИЧЕСКАЯ ФУНКЦИЯ. НЕ ИСПОЛЬЗОВАТЬ В ПРОДАКШЕНЕ!
     * Полностью очищает S3 бакет от всех файлов.
     * Данная функция предназначена исключительно для технического обслуживания
     * при полной очистке данных и переустановке сервера.
     * Использование приведет к необратимой потере всех файлов.
     *
     * @return Количество удаленных файлов или -1 в случае ошибки
     */
    @LogBusinessOperation(operationType = "S3_FILES_PURGE_ALL", description = "Полная очистка S3 бакета (техническая операция)")
    fun purgeAllFiles(): Int {
        try {
            var deletedCount = 0
            var continuationToken: String? = null
            
            do {
                val listRequest = if (continuationToken == null) {
                    software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .build()
                } else {
                    software.amazon.awssdk.services.s3.model.ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .continuationToken(continuationToken)
                        .build()
                }
                
                val response = s3Client.listObjectsV2(listRequest)
                continuationToken = response.nextContinuationToken()
                
                val objects = response.contents()
                if (objects.isNotEmpty()) {
                    // Удаляем объекты пакетом
                    val deleteRequest = software.amazon.awssdk.services.s3.model.Delete.builder()
                        .objects(objects.map { 
                            software.amazon.awssdk.services.s3.model.ObjectIdentifier.builder()
                                .key(it.key())
                                .build() 
                        })
                        .build()
                    
                    val deleteObjectsRequest = software.amazon.awssdk.services.s3.model.DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(deleteRequest)
                        .build()
                    
                    val deleteResult = s3Client.deleteObjects(deleteObjectsRequest)
                    deletedCount += deleteResult.deleted().size
                }
            } while (continuationToken != null)
            
            return deletedCount
        } catch (e: Exception) {
            return -1
        }
    }

    /**
     * Переименовывает файл в S3 хранилище путем копирования с новым именем и удаления старого.
     *
     * @param oldFileKey ключ существующего файла в S3
     * @param newFileName новое имя файла
     * @return ключ переименованного файла или null, если переименование не удалось
     */
    @LogBusinessOperation(operationType = "S3_FILE_RENAME", description = "Переименование файла в S3")
    fun renameFile(oldFileKey: String, newFileName: String): String? {
        try {
            // Получаем директорию из старого ключа
            val directory = oldFileKey.substringBeforeLast("/")
            val sanitizedNewFileName = sanitizeFileName(newFileName)
            val newFileKey = "$directory/$sanitizedNewFileName"
            
            // Если ключи совпадают, значит имя не изменилось
            if (oldFileKey == newFileKey) {
                return oldFileKey
            }
            
            // Скачиваем содержимое файла
            val fileData = downloadFile(oldFileKey)
            
            // Загружаем с новым именем
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(newFileKey)
                .build()
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData))
            
            // Удаляем старый файл
            deleteFile(oldFileKey)
            
            return newFileKey
        } catch (e: Exception) {
            return null
        }
    }
}