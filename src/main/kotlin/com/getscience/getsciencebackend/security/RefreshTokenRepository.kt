package com.getscience.getsciencebackend.security

import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.token.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Репозиторий для работы с токенами обновления (refresh tokens).
 * 
 * Предоставляет методы для поиска, удаления и управления токенами обновления в базе данных.
 */
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    
    /**
     * Находит токен обновления по его значению.
     * 
     * @param token значение токена обновления
     * @return найденный токен обновления или null, если токен не найден
     */
    fun findByToken(token: String): RefreshToken?
    
    /**
     * Удаляет токен обновления, связанный с указанным аккаунтом.
     * 
     * @param account аккаунт пользователя
     */
    fun deleteByAccount(account: Account)
    
    /**
     * Удаляет все токены обновления, связанные с указанным аккаунтом.
     * 
     * @param account аккаунт пользователя
     */
    fun deleteAllByAccount(account: Account)
}
