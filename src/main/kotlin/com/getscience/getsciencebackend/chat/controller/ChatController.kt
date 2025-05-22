package com.getscience.getsciencebackend.chat.controller

import com.getscience.getsciencebackend.chat.data.dto.ChatMessageRequest
import com.getscience.getsciencebackend.chat.data.dto.ChatMessageResponse
import com.getscience.getsciencebackend.chat.data.dto.ChatResponse
import com.getscience.getsciencebackend.chat.service.ChatService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.security.Principal

/**
 * Контроллер для управления чатами и сообщениями.
 * Содержит обработчики WebSocket-сообщений и REST-эндпоинты для работы с чатами.
 */
@RestController
@RequestMapping("/api/v1/chats") // Общий префикс для REST эндпоинтов
class ChatController(
    private val chatService: ChatService,
    private val simpMessagingTemplate: SimpMessagingTemplate
) {

    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    /**
     * Обрабатывает отправку сообщения через WebSocket.
     * Проверяет, что пользователь аутентифицирован и является участником чата.
     * 
     * @param chatId идентификатор чата
     * @param chatMessageRequest содержимое сообщения
     * @param headerAccessor доступ к заголовкам STOMP сообщения
     * @throws IllegalArgumentException если пользователь не аутентифицирован
     */
    @MessageMapping("/chat/{chatId}/sendMessage") // Path for WebSocket, not REST
    fun handleWebSocketSendMessage(
        @DestinationVariable chatId: Long,
        @Payload chatMessageRequest: ChatMessageRequest,
        headerAccessor: SimpMessageHeaderAccessor
    ) {
        val userEmail: String? = when (val sessionUser = headerAccessor.user) {
            is UsernamePasswordAuthenticationToken -> {
                val principal = sessionUser.principal
                if (principal is UserDetails) {
                    principal.username
                } else {
                    sessionUser.name
                }
            }
            is Principal -> sessionUser.name
            else -> SecurityContextHolder.getContext().authentication?.name
        }

        if (userEmail == null) {
            logger.warn("Unauthorized WebSocket access attempt to chat $chatId: user principal not found.")
            throw IllegalArgumentException("Unauthorized: User principal not found")
        }

        if (!chatService.isUserParticipantInChat(chatId, userEmail)) {
            simpMessagingTemplate.convertAndSendToUser(userEmail, "/queue/errors", "Access denied to chat $chatId via WebSocket")
            return
        }
        chatService.sendMessage(chatId, userEmail, chatMessageRequest)
    }

    // --- REST Endpoints ---

    /**
     * Находит чат по идентификатору события.
     * 
     * @param eventId идентификатор события
     * @param principal информация о пользователе
     * @return чат, связанный с указанным событием, или 404 если чат не найден
     */
    @GetMapping("/event/{eventId}/find")
    @PreAuthorize("isAuthenticated()")
    fun findChatByEvent(@PathVariable eventId: Long, principal: Principal): ResponseEntity<ChatResponse> {
        val chatResponse = chatService.findChatByEvent(eventId, principal.name)
        return if (chatResponse != null) {
            ResponseEntity.ok(chatResponse)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Создает чат для события (если не существует) и отправляет сообщение.
     * 
     * @param eventId идентификатор события
     * @param messageRequest содержимое сообщения
     * @param principal информация о пользователе
     * @return отправленное сообщение с кодом 201 (Created)
     */
    @PostMapping("/event/{eventId}/message")
    @PreAuthorize("isAuthenticated()")
    fun createChatAndSendMessage(
        @PathVariable eventId: Long,
        @RequestBody messageRequest: ChatMessageRequest,
        principal: Principal
    ): ResponseEntity<ChatMessageResponse> {
        val chat = chatService.getOrCreateChat(eventId, principal.name)
        val messageResponse = chatService.sendMessage(chat.id, principal.name, messageRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(messageResponse)
    }

    /**
     * Получает список сообщений чата с пагинацией.
     * 
     * @param chatId идентификатор чата
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @param principal информация о пользователе
     * @return страница с сообщениями чата
     */
    @GetMapping("/{chatId}/messages")
    @PreAuthorize("@chatServiceImpl.isUserParticipantInChat(#chatId, principal.name)")
    fun getMessages(
        @PathVariable chatId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        principal: Principal
    ): ResponseEntity<Page<ChatMessageResponse>> {
        val userEmail = principal.name
        val messagesPage = chatService.getMessages(chatId, userEmail, page, size)
        return ResponseEntity.ok(messagesPage)
    }

    /**
     * Получает список чатов пользователя с пагинацией.
     * 
     * @param page номер страницы (начиная с 0)
     * @param size размер страницы
     * @param principal информация о пользователе
     * @return страница с чатами пользователя
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    fun getMyChats(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        principal: Principal
    ): ResponseEntity<Page<ChatResponse>> {
        val userEmail = principal.name
        val chatsPage = chatService.getChatsForUser(userEmail, page, size)
        return ResponseEntity.ok(chatsPage)
    }

    /**
     * Отмечает чат как прочитанный для текущего пользователя.
     * 
     * @param chatId идентификатор чата
     * @param principal информация о пользователе
     * @return пустой ответ с кодом 200 (OK)
     */
    @PostMapping("/{chatId}/read")
    @PreAuthorize("@chatServiceImpl.isUserParticipantInChat(#chatId, principal.name)")
    fun markChatAsRead(@PathVariable chatId: Long, principal: Principal): ResponseEntity<Void> {
        val userEmail = principal.name
        chatService.markChatAsRead(chatId, userEmail)
        return ResponseEntity.ok().build()
    }

    /**
     * Получает список профилей участников чата.
     * 
     * @param chatId идентификатор чата
     * @param principal информация о пользователе
     * @return список профилей участников чата
     */
    @GetMapping("/{chatId}/participants")
    @PreAuthorize("@chatServiceImpl.isUserParticipantInChat(#chatId, principal.name)")
    fun getChatParticipantProfiles(@PathVariable chatId: Long, principal: Principal): ResponseEntity<List<com.getscience.getsciencebackend.user.data.dto.ProfileResponse>> {
        val userEmail = principal.name
        val profiles = chatService.getChatParticipantProfiles(chatId, userEmail)
        return ResponseEntity.ok(profiles)
    }

    /**
     * Получает количество непрочитанных чатов пользователя.
     * 
     * @param principal информация о пользователе
     * @return количество непрочитанных чатов
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    fun getUnreadChatsCount(principal: Principal): ResponseEntity<Int> {
        val count = chatService.getUnreadChatsCount(principal.name)
        return ResponseEntity.ok(count)
    }

    /**
     * Получает подробную информацию о чате.
     * 
     * @param chatId идентификатор чата
     * @param principal информация о пользователе
     * @return подробная информация о чате
     */
    @GetMapping("/{chatId}/details")
    @PreAuthorize("@chatServiceImpl.isUserParticipantInChat(#chatId, principal.name)")
    fun getChatDetails(@PathVariable chatId: Long, principal: Principal): ResponseEntity<ChatResponse> {
        val chatResponse = chatService.getChatDetails(chatId, principal.name)
        return ResponseEntity.ok(chatResponse)
    }
} 