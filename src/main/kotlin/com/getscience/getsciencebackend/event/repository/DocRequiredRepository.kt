package com.getscience.getsciencebackend.event.repository

import com.getscience.getsciencebackend.event.data.model.DocRequired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Репозиторий для работы с сущностями требуемых документов.
 * Предоставляет методы для выполнения CRUD-операций и поиска документов по идентификатору мероприятия.
 */
@Repository
interface DocRequiredRepository : JpaRepository<DocRequired, Long> {
    /**
     * Находит все требуемые документы для указанного мероприятия.
     *
     * @param eventId идентификатор мероприятия
     * @return список требуемых документов для мероприятия
     */
    fun findByEventEventId(eventId: Long): List<DocRequired>
}
