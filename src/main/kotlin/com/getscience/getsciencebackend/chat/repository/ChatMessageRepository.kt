package com.getscience.getsciencebackend.chat.repository

import com.getscience.getsciencebackend.chat.data.model.ChatMessage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Date

/**
 * Репозиторий для работы с сообщениями чата.
 * Предоставляет методы для получения, подсчета и удаления сообщений.
 */
@Repository
interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    
    /**
     * Находит все сообщения в чате с сортировкой по времени (сначала новые) и пагинацией.
     * 
     * @param chatId идентификатор чата
     * @param pageable параметры пагинации и сортировки
     * @return страница сообщений чата
     */
    fun findByChatIdOrderByTimestampDesc(chatId: Long, pageable: Pageable): Page<ChatMessage>
    
    /**
     * Находит последнее сообщение в чате.
     * 
     * @param chatId идентификатор чата
     * @return последнее сообщение или null, если сообщений нет
     */
    fun findFirstByChatIdOrderByTimestampDesc(chatId: Long): ChatMessage?
    
    /**
     * Подсчитывает количество сообщений в чате после указанной даты/времени.
     * 
     * @param chatId идентификатор чата
     * @param timestamp дата/время, после которой нужно считать сообщения
     * @return количество сообщений
     */
    fun countByChatIdAndTimestampAfter(chatId: Long, timestamp: Date): Int
    
    /**
     * Подсчитывает количество сообщений в чате после указанной даты/времени, 
     * исключая сообщения от указанного пользователя.
     * Используется для подсчета непрочитанных сообщений.
     * 
     * @param chatId идентификатор чата
     * @param timestamp дата/время последнего прочтения
     * @param senderProfileId идентификатор пользователя, чьи сообщения исключаются
     * @return количество непрочитанных сообщений
     */
    fun countByChatIdAndTimestampAfterAndSenderProfileIdNot(chatId: Long, timestamp: Date, senderProfileId: Long): Int
    
    /**
     * Удаляет все сообщения в указанном чате.
     * 
     * @param chatId идентификатор чата
     * @return количество удаленных сообщений
     */
    @Modifying
    @Query("DELETE FROM ChatMessage cm WHERE cm.chat.id = :chatId")
    fun deleteByChatId(@Param("chatId") chatId: Long): Int
} 