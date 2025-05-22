package com.getscience.getsciencebackend.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Конфигурация корутин для обеспечения асинхронной обработки задач.
 * Предоставляет области корутин для использования в других компонентах приложения.
 */
@Configuration
class CoroutineConfig {

    /**
     * Создает основную область корутин для общих асинхронных операций.
     * Использует SupervisorJob для изоляции ошибок и Dispatchers.IO для операций ввода-вывода.
     *
     * @return область корутин для общих асинхронных операций
     */
    @Bean
    fun coroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Создает специализированную область корутин для отправки уведомлений.
     * Использует SupervisorJob для изоляции ошибок и Dispatchers.IO для операций ввода-вывода.
     *
     * @return область корутин для отправки уведомлений
     */
    @Bean
    fun notificationCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


}