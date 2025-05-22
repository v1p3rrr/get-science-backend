package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.application.data.dto.ApplicationFileMetadataDTO
import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.file.data.dto.FileApplicationRequest
import com.getscience.getsciencebackend.file.data.dto.FileApplicationResponse
import com.getscience.getsciencebackend.file.data.model.FileApplication
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import org.springframework.web.multipart.MultipartFile
import java.net.URL

/**
 * Сервис для управления файлами заявок.
 * Предоставляет методы для загрузки, получения, скачивания и удаления файлов,
 * связанных с заявками на участие в мероприятиях.
 */
interface FileApplicationService {
    /**
     * Загружает файлы в S3 хранилище и создает записи о них в базе данных.
     * 
     * @param files список файлов для загрузки
     * @param application заявка, к которой привязываются файлы
     * @param fileApplicationFileMetadataDTO метаданные файлов (включая информацию о шифровании)
     * @return список созданных записей о файлах
     */
    fun uploadFiles(files: List<MultipartFile>, application: Application, fileApplicationFileMetadataDTO: List<ApplicationFileMetadataDTO>): List<FileApplication>
    
    /**
     * Создает запись о файле заявки в базе данных.
     * 
     * @param fileApplicationRequest данные о файле заявки
     * @param applicationId идентификатор заявки
     * @param email email пользователя для проверки прав доступа
     * @return созданная запись о файле
     */
    fun createFileApplication(fileApplicationRequest: FileApplicationRequest, applicationId: Long, email: String): FileApplication
    
    /**
     * Получает список файлов для указанной заявки.
     * 
     * @param applicationId идентификатор заявки
     * @param email email пользователя для проверки прав доступа
     * @return список файлов заявки
     */
    fun getFileApplicationsByApplication(applicationId: Long, email: String): List<FileApplicationResponse>
    
    /**
     * Генерирует presigned URL для скачивания файла заявки.
     * 
     * @param fileId идентификатор файла
     * @param email email пользователя для проверки прав доступа
     * @return временный URL для доступа к файлу в S3
     */
    fun getPresignedUrlForFile(fileId: Long, email: String): URL
    
    /**
     * Скачивает файл заявки.
     * Расшифровывает файл, если он был зашифрован при загрузке.
     * 
     * @param fileId идентификатор файла
     * @param email email пользователя для проверки прав доступа
     * @return содержимое файла в виде массива байтов
     */
    fun downloadFile(fileId: Long, email: String): ByteArray
    
    /**
     * Получает заявку, к которой привязан файл.
     * 
     * @param fileId идентификатор файла
     * @param email email пользователя для проверки прав доступа
     * @return заявка, к которой привязан файл
     */
    fun getApplication(fileId: Long, email: String): Application
    
    /**
     * Получает метаданные файла заявки.
     * 
     * @param fileId идентификатор файла
     * @param email email пользователя для проверки прав доступа
     * @return метаданные файла
     */
    fun getFileApplication(fileId: Long, email: String): FileApplication
    
    /**
     * Удаляет файл заявки.
     * Удаляет как запись в базе данных, так и сам файл из S3 хранилища.
     * 
     * @param fileId идентификатор файла
     * @param email email пользователя для проверки прав доступа
     * @return true, если удаление прошло успешно, иначе false
     */
    fun deleteFileApplication(fileId: Long, email: String): Boolean
    
    /**
     * Удаляет все файлы заявки, связанные с указанным требуемым документом.
     * 
     * @param applicationId идентификатор заявки
     * @param docRequiredId идентификатор требуемого документа
     * @param email email пользователя для проверки прав доступа
     * @return true, если все файлы были успешно удалены, иначе false
     */
    fun deleteFileApplicationsByDocRequiredId(applicationId: Long, docRequiredId: Long, email: String): Boolean
}