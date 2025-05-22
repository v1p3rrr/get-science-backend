package com.getscience.getsciencebackend.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Конфигурация WebClient для выполнения HTTP-запросов к внешним API.
 * Предоставляет настроенные экземпляры WebClient для использования в сервисах.
 */
@Configuration
class WebClientConfig {

    private val logger = LoggerFactory.getLogger(WebClientConfig::class.java)

    /**
     * Создает и настраивает WebClient для взаимодействия с сервисом Photon.
     * Photon - это API геокодирования на основе OpenStreetMap.
     * Добавляет логирование для отслеживания запросов.
     *
     * @param builder базовый построитель WebClient
     * @return настроенный WebClient для запросов к Photon API
     */
    @Bean("photonWebClient")
    fun photonWebClient(builder: WebClient.Builder): WebClient =
        builder
            .baseUrl("https://photon.komoot.io").filter { request, next ->
      logger.info(">> Запрос к Photon: ${request.method()} ${request.url()}")
      next.exchange(request)
    }
    .build()
}