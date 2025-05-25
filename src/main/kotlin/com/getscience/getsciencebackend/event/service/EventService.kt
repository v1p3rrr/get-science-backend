package com.getscience.getsciencebackend.event.service

import com.getscience.getsciencebackend.event.data.dto.EventFilterMetadataResponse
import com.getscience.getsciencebackend.event.data.dto.EventFilterRequest
import com.getscience.getsciencebackend.event.data.dto.EventRequest
import com.getscience.getsciencebackend.event.data.dto.EventResponse
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.data.model.FileEvent
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import org.springframework.data.domain.Page
import com.getscience.getsciencebackend.event.data.model.EventModerationStatus
import org.springframework.web.multipart.MultipartFile

/**
 * Сервис для управления мероприятиями.
 * Предоставляет методы для создания, обновления, удаления мероприятий,
 * а также управления связанными с мероприятиями данными: файлами, фильтрами, экспортом.
 */
interface EventService {
    /**
     * Создает новое мероприятие.
     * 
     * @param eventRequest информация о создаваемом мероприятии
     * @param email email пользователя, создающего мероприятие
     * @return true, если мероприятие успешно создано, иначе false
     */
    fun createEvent(eventRequest: EventRequest, email: String): Long
    
    /**
     * Создает новое мероприятие с прикрепленными файлами.
     * 
     * @param eventRequest информация о создаваемом мероприятии
     * @param fileEventRequestList список метаданных загружаемых файлов
     * @param files список файлов для загрузки
     * @param email email пользователя, создающего мероприятие
     * @return true, если мероприятие успешно создано, иначе false
     */
    fun createEventWithFiles(eventRequest: EventRequest, fileEventRequestList: List<FileEventRequest>, files: List<MultipartFile>, email: String): Long
    
    /**
     * Обновляет существующее мероприятие.
     * 
     * @param eventId идентификатор мероприятия
     * @param eventRequest обновленная информация о мероприятии
     * @param email email пользователя, обновляющего мероприятие
     * @return true, если мероприятие успешно обновлено, иначе false
     */
    fun updateEvent(eventId: Long, eventRequest: EventRequest, email: String): Long
    
    /**
     * Обновляет существующее мероприятие с обновлением прикрепленных файлов.
     * 
     * @param eventId идентификатор мероприятия
     * @param eventRequest обновленная информация о мероприятии
     * @param fileEventRequestList список метаданных загружаемых файлов
     * @param files список файлов для загрузки
     * @param email email пользователя, обновляющего мероприятие
     * @return true, если мероприятие успешно обновлено, иначе false
     */
    fun updateEventWithFiles(eventId: Long, eventRequest: EventRequest, fileEventRequestList: List<FileEventRequest>, files: List<MultipartFile>, email: String): Long
    
    /**
     * Получает информацию о мероприятии по его идентификатору.
     * 
     * @param eventId идентификатор мероприятия
     * @return информация о мероприятии
     */
    fun getEventById(eventId: Long): EventResponse
    
    /**
     * Получает полную информацию о мероприятии, включая данные о рецензентах и со-организаторах.
     * 
     * @param eventId идентификатор мероприятия
     * @return полная информация о мероприятии
     */
    fun getEventWithReviewersAndCoownersById(eventId: Long): EventResponse
    
    /**
     * Удаляет мероприятие.
     * 
     * @param eventId идентификатор мероприятия
     * @param email email пользователя, удаляющего мероприятие
     * @return true, если мероприятие успешно удалено, иначе false
     */
    fun deleteEvent(eventId: Long, email: String): Boolean
    
    /**
     * Получает список всех мероприятий.
     * 
     * @return список мероприятий
     */
    fun getAllEvents(): List<EventResponse>
    
    /**
     * Получает постраничный список всех мероприятий.
     * 
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список мероприятий
     */
    fun getAllEventsPaged(page: Int, size: Int): Page<EventResponse>
    
    /**
     * Получает постраничный список мероприятий пользователя.
     * 
     * @param email email пользователя
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список мероприятий пользователя
     */
    fun getEventsByJwtPaged(email: String, page: Int, size: Int): Page<EventResponse>
    
    /**
     * Получает постраничный список мероприятий организатора.
     * 
     * @param organizerId идентификатор организатора
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список мероприятий организатора
     */
    fun getEventsByOrganizerPaged(organizerId: Long, page: Int, size: Int): Page<EventResponse>
    
    /**
     * Поиск событий по одиночным значениям фильтров (старый метод)
     * 
     * @param type тип мероприятия
     * @param theme тема мероприятия
     * @param location место проведения
     * @param format формат мероприятия
     * @param title заголовок мероприятия (для поиска по частичному совпадению)
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список отфильтрованных мероприятий
     */
    fun searchEventsPaged(type: String?, theme: String?, location: String?, format: String?, title: String?, page: Int, size: Int): Page<EventResponse>
    
    /**
     * Поиск событий по множественным значениям фильтров
     * 
     * @param filterRequest объект с параметрами фильтрации
     * @return постраничный список отфильтрованных мероприятий
     */
    fun searchEventsWithMultipleFilters(filterRequest: EventFilterRequest): Page<EventResponse>
    
    /**
     * Получение метаданных для построения фильтров 
     * (списки уникальных тем, типов и форматов мероприятий)
     * 
     * @return объект с метаданными для фильтрации
     */
    fun getEventFilterMetadata(): EventFilterMetadataResponse
    
    /**
     * Получает список рекомендаций мероприятий на основе указанного мероприятия.
     * 
     * @param eventId идентификатор мероприятия, для которого формируются рекомендации
     * @param limit максимальное количество рекомендаций
     * @return список рекомендуемых мероприятий
     */
    fun getRecommendations(eventId: Long, limit: Int): List<EventResponse>
    
    /**
     * Добавляет файл к мероприятию.
     * 
     * @param eventId идентификатор мероприятия
     * @param fileEventRequest метаданные добавляемого файла
     * @param email email пользователя, добавляющего файл
     * @return информация о добавленном файле
     */
    fun addFileToEvent(eventId: Long, fileEventRequest: FileEventRequest, email: String): FileEvent
    
    /**
     * Получает список файлов, связанных с мероприятием.
     * 
     * @param eventId идентификатор мероприятия
     * @return список файлов мероприятия
     */
    fun getFilesByEvent(eventId: Long): List<FileEventResponse>
    
    /**
     * Получает информацию о файле по его идентификатору.
     * 
     * @param fileId идентификатор файла
     * @return информация о файле
     */
    fun getFileEventById(fileId: Long): FileEventResponse
    
    /**
     * Получает список мероприятий, организатором которых является пользователь.
     * 
     * @param email email пользователя
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список мероприятий пользователя
     */
    fun getMyEvents(email: String, page: Int, size: Int): Page<EventResponse>
    
    /**
     * Получает список мероприятий, в которых пользователь является со-организатором.
     * 
     * @param email email пользователя
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список мероприятий
     */
    fun getCoownerEvents(email: String, page: Int, size: Int): Page<EventResponse>
    
    /**
     * Получает список мероприятий, в которых пользователь является рецензентом.
     * 
     * @param email email пользователя
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список мероприятий
     */
    fun getReviewerEvents(email: String, page: Int, size: Int): Page<EventResponse>
    
    /**
     * Получает список мероприятий, прошедших модерацию.
     * 
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список мероприятий
     */
    fun getModerationApprovedEvents(page: Int, size: Int): Page<EventResponse>
    
    /**
     * Получает список мероприятий, не прошедших модерацию.
     * 
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return постраничный список мероприятий
     */
    fun getModerationNonApprovedEvents(page: Int, size: Int): Page<EventResponse>
    
    /**
     * Обновляет статус модерации мероприятия.
     * 
     * @param eventId идентификатор мероприятия
     * @param moderationStatus новый статус модерации
     * @param email email пользователя-модератора
     * @return true, если статус успешно обновлен, иначе false
     */
    fun updateEventModerationStatus(eventId: Long, moderationStatus: EventModerationStatus, email: String): Boolean
    
    /**
     * Добавляет материал к мероприятию.
     * 
     * @param eventId идентификатор мероприятия
     * @param uploaderEmail email пользователя, загружающего материал
     * @param file загружаемый файл
     * @param name название материала
     * @param category категория материала
     * @param description описание материала
     * @return информация о добавленном материале
     */
    fun addEventMaterial(eventId: Long, uploaderEmail: String, file: MultipartFile, name: String, category: String, description: String): FileEventResponse
    
    /**
     * Экспортирует событие в формате iCalendar
     *
     * @param eventId ID события для экспорта
     * @return Байтовый массив с содержимым файла iCalendar
     */
    fun exportEventToCalendar(eventId: Long): ByteArray
    
    /**
     * Экспортирует список участников события в формате Excel
     *
     * @param eventId ID события для экспорта
     * @param email Email пользователя, запрашивающего экспорт
     * @return Байтовый массив с содержимым Excel-файла
     */
    fun exportEventParticipantsToExcel(eventId: Long, email: String): ByteArray
}
