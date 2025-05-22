package com.getscience.getsciencebackend.chat.service

import com.getscience.getsciencebackend.chat.data.dto.ChatMessageRequest
import com.getscience.getsciencebackend.chat.data.dto.ChatMessageResponse
import com.getscience.getsciencebackend.chat.data.dto.ChatResponse
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import org.springframework.data.domain.Page

/**
 * Сервис для работы с чатами и сообщениями.
 * Предоставляет методы для создания и управления чатами, отправки и получения сообщений,
 * отслеживания прочитанных сообщений и управления участниками чата.
 */
interface ChatService {
    /**
     * Находит существующий чат для события и инициатора или создает новый, если он не существует.
     * Автоматически добавляет организатора, совладельцев и рецензентов события в качестве участников.
     * 
     * @param eventId идентификатор события
     * @param initiatorEmail email инициатора чата
     * @return данные чата с информацией о непрочитанных сообщениях
     * @throws IllegalArgumentException если профиль инициатора или событие не найдены
     */
    fun getOrCreateChat(eventId: Long, initiatorEmail: String): ChatResponse

    /**
     * Находит существующий чат для события и инициатора.
     * 
     * @param eventId идентификатор события
     * @param initiatorEmail email инициатора
     * @return данные чата или null, если чат не найден
     * @throws IllegalArgumentException если профиль инициатора не найден
     * @throws jakarta.persistence.EntityNotFoundException если событие не найдено
     */
    fun findChatByEvent(eventId: Long, initiatorEmail: String): ChatResponse?

    /**
     * Отправляет сообщение в указанный чат.
     * Проверяет, что отправитель является участником чата.
     * Рассылает уведомления участникам через WebSocket.
     * 
     * @param chatId идентификатор чата
     * @param senderEmail email отправителя
     * @param messageRequest содержимое сообщения
     * @return данные отправленного сообщения
     * @throws IllegalArgumentException если профиль отправителя или чат не найдены
     * @throws SecurityException если отправитель не является участником чата
     */
    fun sendMessage(chatId: Long, senderEmail: String, messageRequest: ChatMessageRequest): ChatMessageResponse

    /**
     * Получает сообщения чата с пагинацией.
     * Проверяет, что пользователь является участником чата.
     * Автоматически отмечает чат как прочитанный для пользователя.
     * 
     * @param chatId идентификатор чата
     * @param userEmail email пользователя
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @return страница с сообщениями чата
     * @throws IllegalArgumentException если профиль пользователя или чат не найдены
     * @throws SecurityException если пользователь не является участником чата
     */
    fun getMessages(chatId: Long, userEmail: String, page: Int, size: Int): Page<ChatMessageResponse>

    /**
     * Получает список чатов пользователя с пагинацией.
     * Для каждого чата предоставляет информацию о последнем сообщении и непрочитанных сообщениях.
     * 
     * @param userEmail email пользователя
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @return страница с чатами пользователя
     * @throws IllegalArgumentException если профиль пользователя не найден
     */
    fun getChatsForUser(userEmail: String, page: Int, size: Int): Page<ChatResponse>

    /**
     * Обновляет список участников чата на основе изменений в составе организаторов, совладельцев 
     * или рецензентов события. Добавляет новых участников и деактивирует отсутствующих.
     * 
     * @param eventId идентификатор события
     * @throws IllegalArgumentException если событие не найдено
     */
    fun updateChatParticipantsByEventId(eventId: Long)

    /**
     * Проверяет, является ли пользователь активным участником указанного чата.
     * 
     * @param chatId идентификатор чата
     * @param userEmail email пользователя
     * @return true если пользователь является активным участником чата
     * @throws IllegalArgumentException если профиль пользователя или чат не найдены
     */
    fun isUserParticipantInChat(chatId: Long, userEmail: String): Boolean

    /**
     * Отмечает чат как прочитанный для указанного пользователя.
     * Обновляет время последнего прочтения на текущее.
     * 
     * @param chatId идентификатор чата
     * @param userEmail email пользователя
     * @throws IllegalArgumentException если профиль пользователя не найден
     */
    fun markChatAsRead(chatId: Long, userEmail: String)

    /**
     * Получает профили участников чата.
     * Проверяет, что запрашивающий пользователь является участником чата.
     * Включает информацию о статусе активности каждого участника.
     * 
     * @param chatId идентификатор чата
     * @param currentUserEmail email текущего пользователя
     * @return список профилей участников
     * @throws IllegalArgumentException если профиль пользователя или чат не найдены
     * @throws SecurityException если пользователь не является участником чата
     */
    fun getChatParticipantProfiles(chatId: Long, currentUserEmail: String): List<ProfileResponse>

    /**
     * Получает общее количество чатов с непрочитанными сообщениями для пользователя.
     * 
     * @param userEmail email пользователя
     * @return количество чатов с непрочитанными сообщениями
     * @throws IllegalArgumentException если профиль пользователя не найден
     */
    fun getUnreadChatsCount(userEmail: String): Int

    /**
     * Получает детали конкретного чата для пользователя.
     * Проверяет, что пользователь является активным участником чата.
     * Включает информацию о непрочитанных сообщениях.
     * 
     * @param chatId идентификатор чата
     * @param userEmail email пользователя
     * @return подробная информация о чате
     * @throws IllegalArgumentException если профиль пользователя не найден
     * @throws jakarta.persistence.EntityNotFoundException если чат не найден
     * @throws SecurityException если пользователь не является активным участником чата
     */
    fun getChatDetails(chatId: Long, userEmail: String): ChatResponse
    
    /**
     * Удаляет чат и все связанные с ним данные по идентификатору события.
     * Включает удаление сообщений, статусов прочтения и записей об участниках.
     * 
     * @param eventId идентификатор события
     * @return true если чат был успешно удален, иначе false
     */
    fun deleteChatByEventId(eventId: Long): Boolean
}