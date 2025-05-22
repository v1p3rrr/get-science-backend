package com.getscience.getsciencebackend.security

import com.getscience.getsciencebackend.user.data.model.token.RefreshToken
import com.getscience.getsciencebackend.user.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Реализация сервиса для управления токенами обновления.
 * 
 * Обеспечивает создание, валидацию и удаление токенов обновления,
 * используемых для получения новых JWT-токенов доступа без повторной аутентификации.
 */
@Service
class RefreshTokenServiceImpl(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val accountRepository: AccountRepository
) : RefreshTokenService {

    /**
     * Создает новый токен обновления для указанного пользователя.
     * Удаляет существующие токены обновления пользователя перед созданием нового.
     * 
     * @param email email пользователя, для которого создается токен
     * @return созданный токен обновления
     * @throws IllegalArgumentException если пользователь с указанным email не найден
     */
    @Transactional
    override fun createRefreshToken(email: String): RefreshToken {
        val account = accountRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Account not found")

        refreshTokenRepository.deleteByAccount(account) // Один активный токен на аккаунт
        refreshTokenRepository.flush()

        val token = UUID.randomUUID().toString()
        val expiryDate = Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7) // 7 дней

        val refreshToken = RefreshToken(token = token, account = account, expiryDate = expiryDate)
        return refreshTokenRepository.save(refreshToken)
    }

    /**
     * Проверяет валидность токена обновления.
     * Если срок действия токена истек, токен удаляется из базы данных.
     * 
     * @param token строковое значение токена обновления
     * @return валидный токен обновления
     * @throws IllegalArgumentException если токен не найден или истек срок его действия
     */
    @Transactional
    override fun validateRefreshToken(token: String): RefreshToken {
        val refreshToken = refreshTokenRepository.findByToken(token)
            ?: throw IllegalArgumentException("Invalid refresh token")

        if (refreshToken.expiryDate.before(Date())) {
            refreshTokenRepository.delete(refreshToken)
            throw IllegalArgumentException("Refresh token expired")
        }

        return refreshToken
    }

    /**
     * Удаляет токен обновления по его значению, если токен существует.
     * 
     * @param token строковое значение токена обновления для удаления
     */
    @Transactional
    override fun deleteByToken(token: String) {
        val tokenEntity = refreshTokenRepository.findByToken(token)
        if (tokenEntity != null) {
            refreshTokenRepository.delete(tokenEntity)
        }
    }

    /**
     * Удаляет все токены обновления, связанные с указанным пользователем.
     * 
     * @param email email пользователя, токены которого нужно удалить
     * @throws IllegalArgumentException если пользователь с указанным email не найден
     */
    @Transactional
    override fun deleteAllTokensByEmail(email: String) {
        val account = accountRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Account not found")
        refreshTokenRepository.deleteAllByAccount(account)
    }
}
