package com.getscience.getsciencebackend.config

import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.protocol.User
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Конфигурация и утилиты для работы с Sentry - системой мониторинга ошибок и производительности.
 * Обеспечивает настройку контекста пользователя и хлебных крошек для отслеживания действий.
 */
@Component
class SentryConfig(
    @Value("\${SENTRY_DSN}") private val dsn: String
) {

    /**
     * Устанавливает контекст пользователя для Sentry.
     * Это позволяет связывать ошибки и события с конкретным пользователем.
     *
     * @param email email пользователя для идентификации в Sentry
     */
    fun setUserContext(email: String) {
        val user = User().apply {
            this.email = email
        }
        Sentry.setUser(user)
    }

    /**
     * Добавляет хлебную крошку в Sentry для отслеживания действий пользователя.
     * Хлебные крошки помогают понять последовательность действий, которые привели к ошибке.
     *
     * @param message текстовое описание действия пользователя
     */
    fun setBreadcrumb(message: String) {
        val breadcrumb = Breadcrumb().apply {
            this.message = message
        }
        Sentry.addBreadcrumb(breadcrumb)
    }
}