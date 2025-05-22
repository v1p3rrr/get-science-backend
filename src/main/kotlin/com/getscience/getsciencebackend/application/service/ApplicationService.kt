package com.getscience.getsciencebackend.application.service

import com.getscience.getsciencebackend.application.data.dto.*
import org.springframework.web.multipart.MultipartFile

/**
 * Сервис для работы с заявками на участие в мероприятиях.
 * Предоставляет методы для создания, обновления, получения и удаления заявок.
 */
interface ApplicationService {
    /**
     * Создает новую заявку на участие в мероприятии.
     * @param applicationRequest данные заявки
     * @param fileApplicationFileMetadataDTO метаданные загружаемых файлов
     * @param files файлы, прикрепляемые к заявке
     * @param email email пользователя, создающего заявку
     * @return информация о созданной заявке
     */
    fun createApplication(applicationRequest: ApplicationRequest, fileApplicationFileMetadataDTO: List<ApplicationFileMetadataDTO>, files: List<MultipartFile>, email: String): ApplicationResponse
    
    /**
     * Обновляет существующую заявку.
     * @param applicationId идентификатор заявки
     * @param applicationRequest обновленные данные заявки
     * @param fileApplicationFileMetadataDTO метаданные загружаемых файлов
     * @param files файлы, прикрепляемые к заявке
     * @param email email пользователя, обновляющего заявку
     * @return информация об обновленной заявке
     */
    fun updateApplication(applicationId: Long, applicationRequest: ApplicationRequest, fileApplicationFileMetadataDTO: List<ApplicationFileMetadataDTO>, files: List<MultipartFile>, email: String): ApplicationResponse
    
    /**
     * Обновляет заявку организатором мероприятия.
     * @param applicationId идентификатор заявки
     * @param applicationRequest обновленные данные заявки
     * @param email email организатора
     * @return информация об обновленной заявке
     */
    fun updateApplicationByOrganizer(applicationId: Long, applicationRequest: ApplicationRequest, email: String): ApplicationResponse
    
    /**
     * Получает информацию о заявке по идентификатору.
     * @param applicationId идентификатор заявки
     * @param email email запрашивающего пользователя
     * @return информация о заявке
     */
    fun getApplicationById(applicationId: Long, email: String): ApplicationResponse
    
    /**
     * Получает детальную информацию о заявке с данными заявителя.
     * @param applicationId идентификатор заявки
     * @param email email запрашивающего пользователя
     * @return детальная информация о заявке с данными заявителя
     */
    fun getApplicationDetailWithApplicant(applicationId: Long, email: String): ApplicationDetailWithApplicantResponse
    
    /**
     * Получает детальную информацию о заявке с данными организатора.
     * @param applicationId идентификатор заявки
     * @param email email запрашивающего пользователя
     * @return детальная информация о заявке с данными организатора
     */
    fun getApplicationDetailWithOrganizer(applicationId: Long, email: String): ApplicationDetailWithOrganizerResponse
    
    /**
     * Получает список всех заявок для указанного мероприятия.
     * @param eventId идентификатор мероприятия
     * @return список заявок
     */
    fun getApplicationsByEvent(eventId: Long): List<ApplicationResponse>
    
    /**
     * Получает список всех заявок для указанного профиля.
     * @param profileId идентификатор профиля
     * @return список заявок
     */
    fun getApplicationsByProfile(profileId: Long): List<ApplicationResponse>
    
    /**
     * Получает список всех заявок для мероприятий, организованных пользователем с указанным email.
     * @param email email организатора
     * @return список заявок
     */
    fun getApplicationsByOrganizer(email: String): List<ApplicationResponse>
    
    /**
     * Получает список всех заявок, поданных пользователем с указанным email.
     * @param email email заявителя
     * @return список заявок
     */
    fun getApplicationsByApplicant(email: String): List<ApplicationResponse>
    
    /**
     * Удаляет заявку по идентификатору.
     * @param applicationId идентификатор заявки
     * @param email email пользователя, удаляющего заявку
     * @return результат операции удаления
     */
    fun deleteApplication(applicationId: Long, email: String): Boolean
}