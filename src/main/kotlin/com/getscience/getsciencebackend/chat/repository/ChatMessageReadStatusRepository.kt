package com.getscience.getsciencebackend.chat.repository

import com.getscience.getsciencebackend.chat.data.model.ChatMessageReadStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Репозиторий для работы со статусами прочтения сообщений в чатах.
 * Предоставляет методы для отслеживания прочитанных сообщений пользователями.
 */
@Repository
interface ChatMessageReadStatusRepository : JpaRepository<ChatMessageReadStatus, Long> {
    
    /**
     * Находит статус прочтения чата для указанного пользователя.
     * 
     * @param chatId идентификатор чата
     * @param profileId идентификатор профиля пользователя
     * @return статус прочтения или null, если статус не найден
     */
    fun findByChatIdAndProfileProfileId(chatId: Long, profileId: Long): ChatMessageReadStatus?
    
    /**
     * Находит все статусы прочтения для указанного чата.
     * 
     * @param chatId идентификатор чата
     * @return список статусов прочтения
     */
    fun findByChatId(chatId: Long): List<ChatMessageReadStatus>
} 