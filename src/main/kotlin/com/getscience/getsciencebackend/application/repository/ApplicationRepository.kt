package com.getscience.getsciencebackend.application.repository

import com.getscience.getsciencebackend.application.data.model.Application
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Репозиторий для работы с сущностью Application.
 * Предоставляет базовые методы CRUD и дополнительные методы поиска заявок.
 */
@Repository
interface ApplicationRepository : JpaRepository<Application, Long> {
    /**
     * Находит все заявки для указанного мероприятия.
     * @param eventId идентификатор мероприятия
     * @return список заявок
     */
    fun findByEventEventId(eventId: Long): List<Application>
    
    /**
     * Находит все заявки для указанного профиля.
     * @param profileId идентификатор профиля
     * @return список заявок
     */
    fun findByProfileProfileId(profileId: Long): List<Application>
    
    /**
     * Находит все заявки для мероприятий, организованных пользователем с указанным email.
     * @param email email организатора
     * @return список заявок
     */
    fun findByEventOrganizerAccountEmail(email: String): List<Application>
    
    /**
     * Находит все заявки, поданные пользователем с указанным email.
     * @param email email заявителя
     * @return список заявок
     */
    fun findByProfileAccountEmail(email: String): List<Application>
    
    /**
     * Находит заявку, содержащую файл с указанным идентификатором.
     * @param fileId идентификатор файла
     * @return заявка или null, если не найдена
     */
    fun findByFileApplicationsFileId(fileId: Long): Application?
}