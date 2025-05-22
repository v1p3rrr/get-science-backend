package com.getscience.getsciencebackend.chat.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.context.annotation.Lazy

/**
 * Конфигурация WebSocket для чатов.
 * Настраивает STOMP брокер сообщений, регистрирует endpoints и добавляет перехватчик для аутентификации.
 * Используется для обеспечения двусторонней коммуникации в реальном времени между клиентами и сервером.
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    @Lazy private val authChannelInterceptor: AuthChannelInterceptor
) : WebSocketMessageBrokerConfigurer {

    companion object {
        const val WEBSOCKET_PREFIX = "/ws/chat"
    }

    @Value("\${frontend.base-url}")
    private lateinit var frontendBaseUrl: String

    /**
     * Настраивает брокер сообщений для маршрутизации сообщений между клиентами и серверными обработчиками.
     * Определяет префиксы для различных типов сообщений.
     * 
     * @param registry регистр для настройки брокера сообщений
     */
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // Префикс для сообщений от сервера к клиенту (подписки)
        registry.enableSimpleBroker("/topic", "/queue")
        // Префикс для сообщений от клиента к серверу (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app")
        // Префикс для сообщений, адресованных конкретному пользователю
        registry.setUserDestinationPrefix("/user")
    }

    /**
     * Регистрирует STOMP endpoints для подключения WebSocket клиентов.
     * Настраивает политику CORS и поддержку SockJS для совместимости со старыми браузерами.
     * 
     * @param registry регистр для настройки STOMP endpoints
     */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val host = extractHost(frontendBaseUrl)
        // Эндпоинт, к которому будут подключаться клиенты
        registry.addEndpoint(WEBSOCKET_PREFIX)
            .setAllowedOriginPatterns("*")
            .withSockJS() // Для поддержки старых браузеров
    }

    /**
     * Настраивает входящий канал клиентских сообщений.
     * Добавляет перехватчик для аутентификации WebSocket-соединений.
     * 
     * @param registration настройка входящего канала сообщений
     */
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(authChannelInterceptor)
    }

    /**
     * Извлекает имя хоста из полного URL.
     * Удаляет префиксы протокола и конечные слеши.
     * 
     * @param url полный URL, из которого нужно извлечь хост
     * @return строка с именем хоста
     */
    private fun extractHost(url: String): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/") // на случай если с хвостом
    }
} 