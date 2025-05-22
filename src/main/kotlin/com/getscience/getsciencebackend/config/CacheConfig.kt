package com.getscience.getsciencebackend.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import java.time.Duration
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.SimpleCacheErrorHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext

/**
 * Конфигурация кэширования с использованием Redis.
 * Настраивает RedisCacheManager для хранения кэшированных данных и обработчик ошибок кэша.
 * Обеспечивает сериализацию объектов Kotlin с сохранением типов через Jackson.
 */
@Configuration
class CacheConfig(
    private val redisConnectionFactory: RedisConnectionFactory
) : CachingConfigurerSupport() {

    private val logger = LoggerFactory.getLogger(CacheConfig::class.java)

    /**
     * Создает и настраивает менеджер кэша Redis.
     * Устанавливает время жизни кэш-записей 20 минут, отключает кэширование null-значений
     * и настраивает Jackson сериализатор с поддержкой типов Kotlin.
     *
     * @return настроенный менеджер кэша Redis
     */
    @Bean
    override fun cacheManager(): RedisCacheManager {
        val redisObjectMapper: ObjectMapper =
        jacksonObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            )

        val serializer = GenericJackson2JsonRedisSerializer(redisObjectMapper)
        val cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(20)) // time to live for cache entries
            .disableCachingNullValues()
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(cacheConfiguration)
            .build()
    }

    /**
     * Создает пользовательский обработчик ошибок кэша, который логирует ошибки,
     * но не прерывает работу приложения при проблемах с Redis.
     *
     * @return настроенный обработчик ошибок кэша
     */
    @Bean
    override fun errorHandler(): CacheErrorHandler {
        return object : SimpleCacheErrorHandler() {
            override fun handleCacheGetError(exception: RuntimeException, cache: org.springframework.cache.Cache, key: Any) {
                // Просто логируем, но не выбрасываем ошибку
                logger.warn("Redis GET error in cache='{}', key='{}': {}", cache.name, key, exception.message)
            }
            override fun handleCachePutError(exception: RuntimeException, cache: org.springframework.cache.Cache, key: Any, value: Any?) {
                logger.warn("Redis PUT error in cache='{}', key='{}': {}", cache.name, key, exception.message)
            }
            override fun handleCacheEvictError(exception: RuntimeException, cache: org.springframework.cache.Cache, key: Any) {
                logger.warn("Redis EVICT error in cache='{}', key='{}': {}", cache.name, key, exception.message)
            }
            override fun handleCacheClearError(exception: RuntimeException, cache: org.springframework.cache.Cache) {
                logger.warn("Redis CLEAR error in cache='{}: {}", cache.name, exception.message)
            }
        }
    }
}