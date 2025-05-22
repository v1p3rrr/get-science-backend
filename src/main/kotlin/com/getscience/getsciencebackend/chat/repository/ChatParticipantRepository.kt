package com.getscience.getsciencebackend.chat.repository

import com.getscience.getsciencebackend.chat.data.model.ChatParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Репозиторий для работы с участниками чата.
 * Предоставляет методы для поиска, проверки и управления участниками чатов.
 */
@Repository
interface ChatParticipantRepository : JpaRepository<ChatParticipant, Long> {
    
    /**
     * Находит всех участников указанного чата.
     * 
     * @param chatId идентификатор чата
     * @return список участников чата
     */
    fun findByChatId(chatId: Long): List<ChatParticipant>

    /**
     * Находит все чаты, в которых участвует пользователь.
     * 
     * @param profileId идентификатор профиля пользователя
     * @return список записей об участии в чатах
     */
    fun findByProfileProfileId(profileId: Long): List<ChatParticipant>

    /**
     * Удаляет из чата всех участников, кроме указанных в списке.
     * 
     * @param chatId идентификатор чата
     * @param profileIds список идентификаторов профилей, которые нужно оставить
     */
    @Modifying
    @Query("DELETE FROM ChatParticipant cp WHERE cp.chat.id = :chatId AND cp.profile.profileId NOT IN :profileIds")
    fun deleteByChatIdAndProfileProfileIdNotIn(@Param("chatId") chatId: Long, @Param("profileIds") profileIds: List<Long>)

    /**
     * Проверяет, является ли пользователь участником чата.
     * 
     * @param chatId идентификатор чата
     * @param profileId идентификатор профиля пользователя
     * @return true, если пользователь является участником чата
     */
    fun existsByChatIdAndProfileProfileId(chatId: Long, profileId: Long): Boolean

    /**
     * Находит запись об участии пользователя в чате.
     * 
     * @param chatId идентификатор чата
     * @param profileId идентификатор профиля пользователя
     * @return запись об участии или null, если пользователь не участвует в чате
     */
    fun findByChatIdAndProfileProfileId(chatId: Long, profileId: Long): ChatParticipant?

    /**
     * Проверяет, является ли пользователь активным участником чата.
     * 
     * @param chatId идентификатор чата
     * @param profileId идентификатор профиля пользователя
     * @return true, если пользователь является активным участником чата
     */
    fun existsByChatIdAndProfileProfileIdAndIsActiveTrue(chatId: Long, profileId: Long): Boolean

    /**
     * Находит все чаты, в которых пользователь является активным участником.
     * 
     * @param profileId идентификатор профиля пользователя
     * @return список записей об активном участии в чатах
     */
    fun findByProfileProfileIdAndIsActiveTrue(profileId: Long): List<ChatParticipant>
} 