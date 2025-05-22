package com.getscience.getsciencebackend.user.data.model.token

enum class TokenType {
    /**
     * Токен для подтверждения email пользователя после регистрации
     */
    EMAIL_VERIFICATION,
    
    /**
     * Токен для сброса и установки нового пароля пользователя
     */
    PASSWORD_RESET
}