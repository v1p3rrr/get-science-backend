package com.getscience.getsciencebackend.security

import com.getscience.getsciencebackend.user.data.model.token.RefreshToken
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import org.springframework.stereotype.Service

/**
 * Сервис для управления токенами обновления (refresh tokens).
 * 
 * Предоставляет методы для создания, валидации и удаления токенов обновления,
 * используемых для получения новых JWT-токенов доступа без повторной аутентификации.
 */
@Service
interface RefreshTokenService {
    /**
     * Создает новый токен обновления для указанного пользователя.
     * 
     * @param email email пользователя, для которого создается токен
     * @return созданный токен обновления
     */
    @LogBusinessOperation(operationType = "REFRESH_TOKEN_CREATE", description = "Создание токена обновления")
    fun createRefreshToken(email: String): RefreshToken

    /**
     * Проверяет валидность токена обновления.
     * 
     * @param token строковое значение токена обновления
     * @return валидный токен обновления или исключение, если токен невалиден
     * @throws IllegalArgumentException если токен не найден или истек срок его действия
     */
    @LogBusinessOperation(operationType = "REFRESH_TOKEN_VALIDATE", description = "Валидация токена обновления")
    fun validateRefreshToken(token: String): RefreshToken

    /**
     * Удаляет токен обновления по его значению.
     * 
     * @param token строковое значение токена обновления для удаления
     */
    @LogBusinessOperation(operationType = "REFRESH_TOKEN_DELETE", description = "Удаление токена обновления")
    fun deleteByToken(token: String)

    /**
     * Удаляет все токены обновления, связанные с указанным пользователем.
     * 
     * @param email email пользователя, токены которого нужно удалить
     */
    @LogBusinessOperation(operationType = "REFRESH_TOKEN_DELETE_ALL", description = "Удаление всех токенов обновления пользователя")
    fun deleteAllTokensByEmail(email: String)
}
