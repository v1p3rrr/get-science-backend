package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.data.model.FileEvent
import java.net.URL
import org.springframework.web.multipart.MultipartFile

/**
 * Сервис для управления файлами мероприятий.
 * Предоставляет методы для создания, загрузки, получения, скачивания, обновления 
 * и удаления файлов, связанных с мероприятиями.
 */
interface FileEventService {
    /**
     * Создает запись о файле мероприятия в базе данных.
     * 
     * @param fileEventRequest данные о файле мероприятия
     * @param event мероприятие, к которому привязывается файл
     * @return созданная запись о файле
     */
    fun createFileEvent(fileEventRequest: FileEventRequest, event: Event): FileEvent
    
    /**
     * Загружает файлы в S3 хранилище и создает записи о них в базе данных.
     * 
     * @param files список файлов для загрузки
     * @param event мероприятие, к которому привязываются файлы
     * @param fileEventRequestList метаданные файлов
     * @return список созданных записей о файлах
     */
    fun uploadFiles(files: List<MultipartFile>, event: Event, fileEventRequestList: List<FileEventRequest>): List<FileEvent>
    
    /**
     * Получает список файлов для указанного мероприятия.
     * 
     * @param eventId идентификатор мероприятия
     * @return список файлов мероприятия
     */
    fun getFilesByEvent(eventId: Long): List<FileEventResponse>
    
    /**
     * Получает метаданные файла мероприятия по его идентификатору.
     * 
     * @param fileId идентификатор файла
     * @return метаданные файла
     */
    fun getFileEventById(fileId: Long): FileEventResponse
    
    /**
     * Генерирует presigned URL для скачивания файла мероприятия.
     * 
     * @param fileId идентификатор файла
     * @return временный URL для доступа к файлу в S3
     */
    fun getPresignedUrlForFile(fileId: Long): URL
    
    /**
     * Скачивает файл мероприятия.
     * 
     * @param fileId идентификатор файла
     * @return содержимое файла в виде массива байтов
     */
    fun downloadFile(fileId: Long): ByteArray
    
    /**
     * Получает метаданные файла мероприятия.
     * 
     * @param fileId идентификатор файла
     * @return метаданные файла
     */
    fun getFileEvent(fileId: Long): FileEvent
    
    /**
     * Удаляет файл мероприятия.
     * Удаляет как запись в базе данных, так и сам файл из S3 хранилища.
     * 
     * @param fileId идентификатор файла
     * @param email email пользователя для проверки прав доступа
     * @return true, если удаление прошло успешно, иначе false
     */
    fun deleteFileEvent(fileId: Long, email: String): Boolean
    
    /**
     * Обновляет метаданные файла мероприятия.
     * При изменении имени файла переименовывает файл в S3 хранилище.
     * 
     * @param fileId идентификатор файла
     * @param updatedFileRequest обновленные данные о файле
     * @param email email пользователя для проверки прав доступа
     * @return обновленные метаданные файла
     */
    fun updateFileEvent(fileId: Long, updatedFileRequest: FileEventRequest, email: String): FileEventResponse
}