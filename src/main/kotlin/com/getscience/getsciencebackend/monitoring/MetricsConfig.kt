package com.getscience.getsciencebackend.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Конфигурация для метрик Prometheus
 * 
 * Этот класс настраивает кастомные метрики для мониторинга приложения
 * через Prometheus и Grafana.
 */
@Configuration
class MetricsConfig {

    /**
     * Создает таймер для измерения времени выполнения HTTP запросов.
     *
     * @param registry реестр метрик Micrometer
     * @return таймер для HTTP запросов
     */
    @Bean
    fun httpRequestTimer(registry: MeterRegistry): Timer {
        return Timer.builder("http.server.requests")
            .description("Время выполнения HTTP запросов")
            .register(registry)
    }

    /**
     * Создает счетчик для подсчета общего количества HTTP запросов.
     *
     * @param registry реестр метрик Micrometer
     * @return счетчик HTTP запросов
     */
    @Bean
    fun httpRequestCounter(registry: MeterRegistry): Counter {
        return registry.counter("http.server.requests.total", "type", "all")
    }

    /**
     * Создает счетчик для подсчета ошибок при выполнении HTTP запросов.
     *
     * @param registry реестр метрик Micrometer
     * @return счетчик ошибок HTTP запросов
     */
    @Bean
    fun httpRequestErrorCounter(registry: MeterRegistry): Counter {
        return Counter.builder("http.server.requests.errors")
            .description("Количество ошибок HTTP запросов")
            .register(registry)
    }

    /**
     * Создает счетчик для подсчета бизнес-операций.
     *
     * @param registry реестр метрик Micrometer
     * @return счетчик бизнес-операций
     */
    @Bean
    fun businessOperationsCounter(registry: MeterRegistry): Counter {
        return Counter.builder("app.business.operations")
            .description("Количество бизнес-операций")
            .tag("operation_type", "all")
            .register(registry)
    }

    /**
     * Создает метрику типа Gauge для отслеживания размера кэша.
     * Работает с Redis-кэшем, подсчитывая количество ключей в кэше.
     *
     * @param registry реестр метрик Micrometer
     * @param cacheManager менеджер кэша приложения
     * @param redisConnectionFactory фабрика подключений к Redis
     * @return метрика типа Gauge для размера кэша
     */
    @Bean
    fun cacheSizeGauge(registry: MeterRegistry, cacheManager: CacheManager, redisConnectionFactory: RedisConnectionFactory): Gauge {
        val redisTemplate = RedisTemplate<String, Any>().apply {
            connectionFactory = redisConnectionFactory
            keySerializer = StringRedisSerializer()
            afterPropertiesSet()
        }

        return Gauge.builder("app.cache.size", cacheManager) { manager -> 
            manager.getCache("default")?.let { cache ->
                when (cache) {
                    is RedisCache -> {
                        val keys = redisTemplate.keys("${cache.name}:*")
                        keys.size.toDouble()
                    }
                    else -> 0.0
                }
            } ?: 0.0
        }
            .description("Размер кэша")
            .register(registry)
    }

    /**
     * Создает счетчик для подсчета ошибок работы с кэшем.
     *
     * @param registry реестр метрик Micrometer
     * @return счетчик ошибок кэша
     */
    @Bean
    fun cacheErrorCounter(registry: MeterRegistry): Counter {
        return Counter.builder("app.cache.errors")
            .description("Количество ошибок кэша")
            .register(registry)
    }

    /**
     * Создает таймер для измерения времени выполнения вызовов внешних сервисов.
     *
     * @param registry реестр метрик Micrometer
     * @return таймер для вызовов внешних сервисов
     */
    @Bean
    fun externalServiceTimer(registry: MeterRegistry): Timer {
        return Timer.builder("app.external.service.time")
            .description("Время выполнения внешних сервисов")
            .register(registry)
    }

    /**
     * Создает счетчик для подсчета ошибок при вызове внешних сервисов.
     *
     * @param registry реестр метрик Micrometer
     * @return счетчик ошибок внешних сервисов
     */
    @Bean
    fun externalServiceErrorCounter(registry: MeterRegistry): Counter {
        return Counter.builder("app.external.service.errors")
            .description("Количество ошибок внешних сервисов")
            .register(registry)
    }
}