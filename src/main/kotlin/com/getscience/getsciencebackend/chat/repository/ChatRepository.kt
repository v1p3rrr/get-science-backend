package com.getscience.getsciencebackend.chat.repository

import com.getscience.getsciencebackend.chat.data.model.Chat
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.user.data.model.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Репозиторий для работы с чатами.
 * Предоставляет методы для поиска и управления чатами.
 */
@Repository
interface ChatRepository : JpaRepository<Chat, Long> {
    
    /**
     * Находит чат по идентификатору события и идентификатору инициатора.
     * 
     * @param eventId идентификатор события
     * @param initiatorId идентификатор профиля инициатора
     * @return чат или null, если чат не найден
     */
    fun findByEventEventIdAndInitiatorProfileId(eventId: Long, initiatorId: Long): Chat?

    /**
     * Находит чат по объектам события и инициатора.
     * 
     * @param event объект события
     * @param initiator объект профиля инициатора
     * @return чат или null, если чат не найден
     */
    fun findByEventAndInitiator(event: Event, initiator: Profile): Chat?

    /**
     * Находит чат по идентификатору события.
     * 
     * @param eventId идентификатор события
     * @return чат или null, если чат не найден
     */
    fun findByEventEventId(eventId: Long): Chat?

    /**
     * Находит чаты, в которых пользователь является активным участником.
     * Сортирует результаты по времени последнего сообщения (сначала новые).
     * 
     * @param profileId идентификатор профиля пользователя
     * @param pageable параметры пагинации
     * @return страница с чатами
     */
    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.profile.profileId = :profileId AND p.isActive = true ORDER BY c.lastMessageTimestamp DESC")
    fun findByActiveParticipantProfileId(profileId: Long, pageable: Pageable): Page<Chat>
} 