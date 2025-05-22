package com.getscience.getsciencebackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * Конфигурация шифрования паролей.
 * Предоставляет бин PasswordEncoder для безопасного хранения паролей.
 */
@Configuration
class PasswordEncoderConfig {

    /**
     * Создает экземпляр BCryptPasswordEncoder для шифрования паролей.
     * BCrypt обеспечивает надежное и современное хеширование паролей с солью.
     *
     * @return экземпляр PasswordEncoder для шифрования и проверки паролей
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}