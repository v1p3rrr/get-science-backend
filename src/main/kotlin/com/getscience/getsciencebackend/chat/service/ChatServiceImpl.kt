package com.getscience.getsciencebackend.chat.service

import com.getscience.getsciencebackend.chat.data.dto.ChatMessageRequest
import com.getscience.getsciencebackend.chat.data.dto.ChatMessageResponse
import com.getscience.getsciencebackend.chat.data.dto.ChatResponse
import com.getscience.getsciencebackend.chat.data.model.Chat
import com.getscience.getsciencebackend.chat.data.model.ChatMessage
import com.getscience.getsciencebackend.chat.data.model.ChatParticipant
import com.getscience.getsciencebackend.chat.data.model.ChatMessageReadStatus
import com.getscience.getsciencebackend.chat.repository.ChatMessageRepository
import com.getscience.getsciencebackend.chat.repository.ChatParticipantRepository
import com.getscience.getsciencebackend.chat.repository.ChatRepository
import com.getscience.getsciencebackend.chat.repository.ChatMessageReadStatusRepository
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.file.service.S3Service
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import com.getscience.getsciencebackend.user.data.dto.ProfileResponse
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date
import org.springframework.data.jpa.repository.Modifying
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation

@Service
class ChatServiceImpl(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatParticipantRepository: ChatParticipantRepository,
    private val profileRepository: ProfileRepository,
    private val eventRepository: EventRepository,
    private val simpMessagingTemplate: SimpMessagingTemplate,
    private val chatMessageReadStatusRepository: ChatMessageReadStatusRepository,
    private val s3Service: S3Service
) : ChatService {

    private val logger = LoggerFactory.getLogger(ChatServiceImpl::class.java)

    /**
     * Получает существующий чат для события или создает новый.
     * Добавляет организатора, совладельцев и рецензентов события в качестве участников.
     * 
     * @param eventId идентификатор события
     * @param initiatorEmail email инициатора чата
     * @return данные чата с информацией о непрочитанных сообщениях
     * @throws IllegalArgumentException если профиль инициатора или событие не найдены
     */
    @LogBusinessOperation(operationType = "CHAT_GET_OR_CREATE", description = "Получение или создание чата для события")
    @Transactional
    override fun getOrCreateChat(eventId: Long, initiatorEmail: String): ChatResponse {
        val initiator = profileRepository.findByAccountEmail(initiatorEmail)
            ?: throw IllegalArgumentException("Initiator profile not found for email: $initiatorEmail")
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        var chat = chatRepository.findByEventEventIdAndInitiatorProfileId(eventId, initiator.profileId)
        var unreadCount = 0

        if (chat == null) {
            chat = Chat(event = event, initiator = initiator, lastMessageTimestamp = Date())
            chat = chatRepository.save(chat)
            // Добавляем инициатора как участника
            addParticipantIfNotExists(chat, initiator)
            // Добавляем организатора, совладельцев и ревьюеров
            updateChatParticipantsInternal(chat, event.organizer, event.coowners.toList(), event.reviewers.toList())
            // При создании чата он сразу прочитан для инициатора
            markChatAsRead(chat.id, initiatorEmail)
        } else {
            val readStatus = chatMessageReadStatusRepository.findByChatIdAndProfileProfileId(chat.id, initiator.profileId)
            val lastReadTime = readStatus?.lastReadTimestamp ?: Date(0) // Если нет статуса, считаем все сообщения непрочитанными
            unreadCount = chatMessageRepository.countByChatIdAndTimestampAfterAndSenderProfileIdNot(chat.id, lastReadTime, initiator.profileId)
        }
        val lastMessage = chatMessageRepository.findFirstByChatIdOrderByTimestampDesc(chat.id)
        return ChatResponse.fromEntity(chat, lastMessage, unreadCount)
    }

    /**
     * Отправляет сообщение в указанный чат.
     * Проверяет, что отправитель является участником чата.
     * Рассылает уведомления активным участникам через WebSocket.
     * 
     * @param chatId идентификатор чата
     * @param senderEmail email отправителя
     * @param messageRequest содержимое сообщения
     * @return данные отправленного сообщения
     * @throws IllegalArgumentException если профиль отправителя или чат не найдены
     * @throws SecurityException если отправитель не является участником чата
     */
    @LogBusinessOperation(operationType = "CHAT_SEND_MESSAGE", description = "Отправка сообщения в чат")
    @Transactional
    override fun sendMessage(chatId: Long, senderEmail: String, messageRequest: ChatMessageRequest): ChatMessageResponse {
        val sender = profileRepository.findByAccountEmail(senderEmail)
            ?: throw IllegalArgumentException("Sender profile not found for email: $senderEmail")
        val chat = chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found with id: $chatId") }

        if (!isUserParticipantInChatInternal(chat, sender)) {
            throw SecurityException("User ${sender.profileId} is not a participant in chat $chatId")
        }

        val message = ChatMessage(
            chat = chat,
            sender = sender,
            content = messageRequest.content,
            timestamp = Date()
        )
        val savedMessage = chatMessageRepository.save(message)
        chat.lastMessageTimestamp = savedMessage.timestamp
        chatRepository.save(chat) 

        val messageResponse = ChatMessageResponse.fromEntity(savedMessage)
        logger.info("[CHAT_SEND_MSG] Chat ID: ${chat.id}, Sender ID: ${sender.profileId}, Message ID: ${savedMessage.id}. Broadcasting...")

        // Отправляем сообщение только АКТИВНЫМ участникам через WebSocket
        chat.participants.filter { it.isActive }.forEach { participant ->
            val userDestination = "/queue/chat/${chat.id}/messages"
            val userEmailForWs = participant.profile.account.email
            logger.info("[CHAT_SEND_MSG] Sending to user $userEmailForWs via STOMP user destination: $userDestination")
            simpMessagingTemplate.convertAndSendToUser(
                userEmailForWs, 
                userDestination, 
                messageResponse
            )
        }
        // Также отправляем на общий топик чата, если кто-то подписан напрямую
        val topicDestination = "/topic/chat/${chat.id}/messages"
        logger.info("[CHAT_SEND_MSG] Sending to general topic: $topicDestination")
        simpMessagingTemplate.convertAndSend(topicDestination, messageResponse)

        return messageResponse
    }

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
    @LogBusinessOperation(operationType = "CHAT_GET_MESSAGES", description = "Получение сообщений чата")
    @Transactional
    override fun getMessages(chatId: Long, userEmail: String, page: Int, size: Int): Page<ChatMessageResponse> {
        val user = profileRepository.findByAccountEmail(userEmail)
            ?: throw IllegalArgumentException("User profile not found for email: $userEmail")
        val chat = chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found with id: $chatId") }

        if (!isUserParticipantInChatInternal(chat, user)) {
            throw SecurityException("User ${user.profileId} is not a participant in chat $chatId")
        }

        // Помечаем чат как прочитанный при запросе сообщений
        markChatAsRead(chatId, userEmail)

        val pageable = PageRequest.of(page, size, Sort.by("timestamp").descending())
        return chatMessageRepository.findByChatIdOrderByTimestampDesc(chatId, pageable)
            .map { ChatMessageResponse.fromEntity(it) }
    }

    /**
     * Получает список чатов пользователя с пагинацией.
     * Для каждого чата предоставляет информацию о непрочитанных сообщениях.
     * 
     * @param userEmail email пользователя
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @return страница с чатами пользователя
     * @throws IllegalArgumentException если профиль пользователя не найден
     */
    @LogBusinessOperation(operationType = "CHAT_GET_FOR_USER", description = "Получение чатов пользователя")
    @Transactional(readOnly = true)
    override fun getChatsForUser(userEmail: String, page: Int, size: Int): Page<ChatResponse> {
        val user = profileRepository.findByAccountEmail(userEmail)
            ?: throw IllegalArgumentException("User profile not found for email: $userEmail")
        val pageable = PageRequest.of(page, size) 
        return chatRepository.findByActiveParticipantProfileId(user.profileId, pageable).map { chat ->
            val lastMessage = chatMessageRepository.findFirstByChatIdOrderByTimestampDesc(chat.id)
            val readStatus = chatMessageReadStatusRepository.findByChatIdAndProfileProfileId(chat.id, user.profileId)
            val lastReadTime = readStatus?.lastReadTimestamp ?: Date(0) // Если нет записи, считаем все непрочитанным
            val unreadCount = chatMessageRepository.countByChatIdAndTimestampAfterAndSenderProfileIdNot(
                chat.id, lastReadTime, user.profileId
            )
            ChatResponse.fromEntity(chat, lastMessage, unreadCount)
        }
    }

    /**
     * Обновляет список участников чата на основе данных о событии.
     * Добавляет организатора, совладельцев и рецензентов события.
     * 
     * @param eventId идентификатор события
     * @throws IllegalArgumentException если событие не найдено
     */
    @LogBusinessOperation(operationType = "CHAT_UPDATE_PARTICIPANTS_BY_EVENT", description = "Обновление участников чата по событию")
    @Transactional
    override fun updateChatParticipantsByEventId(eventId: Long) {
        val event = eventRepository.findWithReviewersAndCoownersByEventId(eventId)
            ?: throw IllegalArgumentException("Event not found with id: $eventId for updating chat participants")
        val chat = chatRepository.findByEventEventId(eventId)
            ?: return // Если чата для этого ивента нет, ничего не делаем
        
        updateChatParticipantsInternal(chat, event.organizer, event.coowners.toList(), event.reviewers.toList())
    }

    /**
     * Обновляет список участников чата.
     * Добавляет новых участников и деактивирует отсутствующих в списке (кроме инициатора).
     * 
     * @param chat чат для обновления
     * @param organizer профиль организатора события
     * @param coowners список профилей совладельцев события
     * @param reviewers список профилей рецензентов события
     */
    private fun updateChatParticipantsInternal(chat: Chat, organizer: Profile, coowners: List<Profile>, reviewers: List<Profile>) {
        val currentParticipantProfiles = chat.participants.map { it.profile }
        // Профили, которые должны быть в чате (организатор, все совладельцы, все ревьюеры, инициатор)
        val expectedProfilesInChat = (coowners + reviewers + listOf(organizer) + listOf(chat.initiator)).distinctBy { it.profileId }

        // Профили, которых нужно добавить
        val profilesToAdd = expectedProfilesInChat.filterNot { expectedProfile ->
            currentParticipantProfiles.any { currentProfile -> currentProfile.profileId == expectedProfile.profileId }
        }

        profilesToAdd.forEach { profile ->
            addParticipantIfNotExists(chat, profile)
        }

        // Находим участников, которых больше нет в списке ожидаемых (кроме инициатора)
        val participantsToDeactivate = chat.participants.filter { participant ->
            participant.isActive && // Деактивируем только активных
            participant.profile.profileId != chat.initiator.profileId && // Инициатора не трогаем
            !expectedProfilesInChat.any { expected -> expected.profileId == participant.profile.profileId }
        }
        
        // Помечаем их как неактивных
        participantsToDeactivate.forEach { participant ->
            participant.isActive = false
            logger.info("[CHAT_PARTICIPANT_UPDATE] Deactivating participant ${participant.profile.profileId} in chat ${chat.id}")
        }

        // Сохраняем изменения, если были добавления или деактивации
        if (profilesToAdd.isNotEmpty() || participantsToDeactivate.isNotEmpty()) {
             chatRepository.save(chat) // Сохраняем изменения в participants, если они были
        }
    }

    /**
     * Добавляет пользователя как участника чата, если он еще не является участником.
     * 
     * @param chat чат
     * @param profile профиль пользователя
     */
    private fun addParticipantIfNotExists(chat: Chat, profile: Profile) {
        if (!chatParticipantRepository.existsByChatIdAndProfileProfileId(chat.id, profile.profileId)) {
            val newParticipant = ChatParticipant(chat = chat, profile = profile)
            // Будет сохранено каскадно при сохранении chat
            chat.participants.add(newParticipant)
        }
    }

    /**
     * Проверяет, является ли пользователь участником чата.
     * 
     * @param chatId идентификатор чата
     * @param userEmail email пользователя
     * @return true если пользователь является активным участником чата
     * @throws IllegalArgumentException если профиль пользователя или чат не найдены
     */
    @LogBusinessOperation(operationType = "CHAT_IS_USER_PARTICIPANT", description = "Проверка, является ли пользователь участником чата")
    @Transactional(readOnly = true)
    override fun isUserParticipantInChat(chatId: Long, userEmail: String): Boolean {
         val user = profileRepository.findByAccountEmail(userEmail)
            ?: throw IllegalArgumentException("User profile not found for email: $userEmail")
        val chat = chatRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("Chat not found with id: $chatId") }
        return isUserParticipantInChatInternal(chat, user)
    }

    /**
     * Внутренний метод для проверки, является ли пользователь активным участником чата.
     * 
     * @param chat чат
     * @param user профиль пользователя
     * @return true если пользователь является активным участником чата
     */
    private fun isUserParticipantInChatInternal(chat: Chat, user: Profile): Boolean {
        return chatParticipantRepository.existsByChatIdAndProfileProfileIdAndIsActiveTrue(chat.id, user.profileId)
    }

    /**
     * Отмечает чат как прочитанный для пользователя.
     * Обновляет время последнего прочтения на текущее.
     * 
     * @param chatId идентификатор чата
     * @param userEmail email пользователя
     * @throws IllegalArgumentException если профиль пользователя не найден
     */
    @LogBusinessOperation(operationType = "CHAT_MARK_AS_READ", description = "Отметить чат как прочитанный")
    @Transactional
    override fun markChatAsRead(chatId: Long, userEmail: String) {
        val user = profileRepository.findByAccountEmail(userEmail)
            ?: throw IllegalArgumentException("User profile not found for email: $userEmail")

         val chat = chatRepository.findById(chatId).orElseThrow { IllegalArgumentException("Chat not found") }
         if(!isUserParticipantInChatInternal(chat, user)) return // или throw exception

        var readStatus = chatMessageReadStatusRepository.findByChatIdAndProfileProfileId(chatId, user.profileId)
        if (readStatus == null) {
            readStatus = ChatMessageReadStatus(
                chatId = chatId,
                profile = user,
                lastReadTimestamp = Date()
            )
        } else {
            readStatus.lastReadTimestamp = Date()
        }
        chatMessageReadStatusRepository.save(readStatus)
    }

    /**
     * Получает список профилей участников чата.
     * Проверяет, что запрашивающий пользователь является участником чата.
     * Генерирует presigned URL для аватаров пользователей.
     * 
     * @param chatId идентификатор чата
     * @param currentUserEmail email текущего пользователя
     * @return список профилей участников
     * @throws IllegalArgumentException если профиль пользователя или чат не найдены
     * @throws SecurityException если пользователь не является участником чата
     */
    @LogBusinessOperation(operationType = "CHAT_GET_PARTICIPANT_PROFILES", description = "Получение профилей участников чата")
    @Transactional(readOnly = true)
    override fun getChatParticipantProfiles(chatId: Long, currentUserEmail: String): List<ProfileResponse> {
        val currentUser = profileRepository.findByAccountEmail(currentUserEmail)
            ?: throw IllegalArgumentException("Current user profile not found for email: $currentUserEmail")
        
        val chat = chatRepository.findById(chatId).orElseThrow {
            IllegalArgumentException("Chat not found with id: $chatId")
        }

        if (!isUserParticipantInChatInternal(chat, currentUser)) {
            throw SecurityException("User ${currentUser.profileId} is not an active participant in chat $chatId and cannot view participant profiles.")
        }

        // Возвращаем ВСЕХ участников (активных и неактивных)
        return chat.participants.map { participant -> 
            val profile = participant.profile
            val presignedUrl = profile.avatarUrl?.let { s3Service.generatePresignedUrl(it).toString() }
            // Передаем isActive в fromEntity
            ProfileResponse.fromEntity(profile, presignedUrl, participant.isActive) 
        }
    }

    /**
     * Подсчитывает количество непрочитанных сообщений в чате для пользователя.
     * 
     * @param chat чат
     * @param user профиль пользователя
     * @return количество непрочитанных сообщений
     */
    private fun calculateUnreadMessages(chat: Chat, user: Profile): Int {
        val readStatus = chatMessageReadStatusRepository.findByChatIdAndProfileProfileId(chat.id, user.profileId)
        val lastReadTime = readStatus?.lastReadTimestamp ?: Date(0)
        return chatMessageRepository.countByChatIdAndTimestampAfterAndSenderProfileIdNot(
            chat.id, lastReadTime, user.profileId
        )
    }

    /**
     * Находит чат по идентификатору события для указанного инициатора.
     * 
     * @param eventId идентификатор события
     * @param initiatorEmail email инициатора
     * @return данные чата или null, если чат не найден
     * @throws IllegalArgumentException если профиль инициатора не найден
     * @throws EntityNotFoundException если событие не найдено
     */
    @LogBusinessOperation(operationType = "CHAT_FIND_BY_EVENT", description = "Поиск чата по событию и инициатору")
    @Transactional(readOnly = true)
    override fun findChatByEvent(eventId: Long, initiatorEmail: String): ChatResponse? {
        val initiator = profileRepository.findByAccountEmail(initiatorEmail)
            ?: throw IllegalArgumentException("User profile not found for email: $initiatorEmail")
        val event = eventRepository.findById(eventId)
            .orElseThrow { EntityNotFoundException("Event not found with id: $eventId") }

        val chat = chatRepository.findByEventAndInitiator(event, initiator)

        return chat?.let {
            val unreadCount = calculateUnreadMessages(it, initiator)
            val lastMessage = chatMessageRepository.findFirstByChatIdOrderByTimestampDesc(it.id)
            ChatResponse.fromEntity(it, lastMessage, unreadCount)
        }
    }

    /**
     * Подсчитывает количество чатов с непрочитанными сообщениями для пользователя.
     * 
     * @param userEmail email пользователя
     * @return количество чатов с непрочитанными сообщениями
     * @throws IllegalArgumentException если профиль пользователя не найден
     */
    @LogBusinessOperation(operationType = "CHAT_GET_UNREAD_COUNT_FOR_USER", description = "Получение количества непрочитанных чатов для пользователя")
    @Transactional(readOnly = true)
    override fun getUnreadChatsCount(userEmail: String): Int {
        val user = profileRepository.findByAccountEmail(userEmail)
            ?: throw IllegalArgumentException("User profile not found for email: $userEmail")

        val participantChatEntities = chatParticipantRepository.findByProfileProfileIdAndIsActiveTrue(user.profileId)
        if (participantChatEntities.isEmpty()) {
            return 0
        }

        var unreadChatsCount = 0
        for (participantChatEntity in participantChatEntities) {
            val chat = participantChatEntity.chat
            val readStatus = chatMessageReadStatusRepository.findByChatIdAndProfileProfileId(chat.id, user.profileId)
            val lastReadTime = readStatus?.lastReadTimestamp ?: Date(0)
            val newMessagesInThisChat = chatMessageRepository.countByChatIdAndTimestampAfterAndSenderProfileIdNot(
                chat.id, lastReadTime, user.profileId
            )
            if (newMessagesInThisChat > 0) {
                unreadChatsCount++
            }
        }
        return unreadChatsCount
    }

    /**
     * Получает подробную информацию о чате.
     * Проверяет, что пользователь является активным участником чата.
     * 
     * @param chatId идентификатор чата
     * @param userEmail email пользователя
     * @return подробная информация о чате
     * @throws IllegalArgumentException если профиль пользователя не найден
     * @throws EntityNotFoundException если чат не найден
     * @throws SecurityException если пользователь не является активным участником чата
     */
    @LogBusinessOperation(operationType = "CHAT_GET_DETAILS", description = "Получение деталей чата")
    @Transactional(readOnly = true)
    override fun getChatDetails(chatId: Long, userEmail: String): ChatResponse {
        val user = profileRepository.findByAccountEmail(userEmail)
            ?: throw IllegalArgumentException("User profile not found for email: $userEmail")
        val chat = chatRepository.findById(chatId)
            .orElseThrow { EntityNotFoundException("Chat not found with id: $chatId") }

        if (!isUserParticipantInChatInternal(chat, user)) {
            throw SecurityException("User ${user.profileId} is not an active participant in chat $chatId")
        }

        val lastMessage = chatMessageRepository.findFirstByChatIdOrderByTimestampDesc(chat.id)
        val unreadCount = calculateUnreadMessages(chat, user) // Используем обновленный метод
        
        // Передаем список ID только АКТИВНЫХ участников
        return ChatResponse.fromEntity(chat, lastMessage, unreadCount, true)
    }

    /**
     * Удаляет чат и все связанные данные по идентификатору события.
     * 
     * @param eventId идентификатор события
     * @return true, если чат успешно удален, иначе false
     */
    @LogBusinessOperation(operationType = "CHAT_DELETE_BY_EVENT", description = "Удаление чата по ID события")
    @Transactional
    @Modifying
    override fun deleteChatByEventId(eventId: Long): Boolean {
        try {
            val chat = chatRepository.findByEventEventId(eventId) ?: return false
            
            logger.info("Начало удаления чата с ID: ${chat.id} для события с ID: $eventId")
            
            val readStatusesDeleted = deleteReadStatusesByChatId(chat.id)
            logger.info("Удалено $readStatusesDeleted статусов прочтения для чата с ID: ${chat.id}")
            
            val messagesDeleted = deleteMessagesByChatId(chat.id)
            logger.info("Удалено $messagesDeleted сообщений для чата с ID: ${chat.id}")
            
            // Чат и участники будут удалены автоматически из-за CascadeType.ALL + orphanRemoval = true
            chatRepository.deleteById(chat.id)
            logger.info("Чат с ID: ${chat.id} успешно удален")
            
            return true
        } catch (e: Exception) {
            logger.error("Ошибка при удалении чата для события с ID: $eventId", e)
            return false
        }
    }
    
    /**
     * Удаляет статусы прочтения сообщений для указанного чата.
     * 
     * @param chatId идентификатор чата
     * @return количество удаленных статусов
     */
    private fun deleteReadStatusesByChatId(chatId: Long): Int {
        val readStatuses = chatMessageReadStatusRepository.findByChatId(chatId)
        val count = readStatuses.size
        chatMessageReadStatusRepository.deleteAll(readStatuses)
        return count
    }
    
    /**
     * Удаляет сообщения для указанного чата.
     * 
     * @param chatId идентификатор чата
     * @return количество удаленных сообщений
     */
    private fun deleteMessagesByChatId(chatId: Long): Int {
        return chatMessageRepository.deleteByChatId(chatId)
    }
} 