package com.getscience.getsciencebackend.chat.config

import com.getscience.getsciencebackend.security.JWTService
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component

/**
 * Перехватчик для аутентификации WebSocket-соединений через STOMP.
 * Извлекает и проверяет JWT токен из заголовка Authorization при установке соединения.
 * При успешной валидации токена устанавливает аутентификацию пользователя для WebSocket-сессии.
 */
@Component
class AuthChannelInterceptor(
    private val jwtService: JWTService,
    private val userDetailsService: UserDetailsService
) : ChannelInterceptor {

    private val logger = LoggerFactory.getLogger(AuthChannelInterceptor::class.java)

    /**
     * Перехватывает сообщения перед отправкой, проверяет аутентификацию при установлении соединения STOMP.
     * Извлекает JWT токен из заголовка, проверяет его валидность и устанавливает аутентификацию пользователя.
     * 
     * @param message сообщение для обработки
     * @param channel канал сообщений
     * @return исходное сообщение или null, если обработка завершилась с ошибкой
     */
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor != null && StompCommand.CONNECT == accessor.command) {
            val authorizationHeader = accessor.getFirstNativeHeader("Authorization")
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                val jwt = authorizationHeader.substring(7)
                try {
                    val username = jwtService.extractUsername(jwt)
                    val userDetails: UserDetails = userDetailsService.loadUserByUsername(username)
                    if (jwtService.validateToken(jwt, userDetails)) {
                        val authentication = UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.authorities
                        )
                        accessor.user = authentication
                        logger.debug("WebSocket STOMP CONNECT authenticated for user: $username")
                    } else {
                        logger.warn("WebSocket STOMP CONNECT authentication failed: Invalid token for user $username")
                    }
                } catch (e: Exception) {
                    logger.error("WebSocket STOMP CONNECT authentication failed", e)
                }
            }
        }
        return message
    }
} 