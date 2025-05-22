package com.getscience.getsciencebackend.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

/**
 * Конфигурация Redis для хранения и доступа к данным.
 * Настраивает подключение к Redis и шаблоны для работы с данными.
 * Обеспечивает сериализацию объектов Kotlin с сохранением типов через Jackson.
 */
@Configuration
@EnableRedisRepositories
class RedisConfig(
    private val redisConnectionFactory: RedisConnectionFactory,
) {

    /**
     * Создает и настраивает шаблон Redis для работы с данными.
     * Настраивает сериализаторы для ключей и значений, обеспечивая совместимость с Kotlin
     * и сохранение информации о типах при сериализации.
     *
     * @return настроенный шаблон Redis для работы с данными
     */
    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> {
        val redisObjectMapper: ObjectMapper =
        jacksonObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            )
        return RedisTemplate<String, Any>().apply {
            connectionFactory = redisConnectionFactory
            val ser = GenericJackson2JsonRedisSerializer(redisObjectMapper)
            keySerializer = StringRedisSerializer()
            valueSerializer = ser
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = ser
            afterPropertiesSet()
        }
    }
}