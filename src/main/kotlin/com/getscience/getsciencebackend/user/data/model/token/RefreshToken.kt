package com.getscience.getsciencebackend.user.data.model.token

import com.getscience.getsciencebackend.user.data.model.Account
import jakarta.persistence.*
import java.util.*

/**
 * Токен обновления для JWT-аутентификации.
 * 
 * Используется для получения нового JWT-токена доступа без повторной аутентификации.
 * Каждый токен связан с определенным аккаунтом пользователя и имеет срок действия.
 */
@Entity
data class RefreshToken(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Строковое значение токена обновления (UUID)
     */
    val token: String,

    /**
     * Аккаунт пользователя, связанный с токеном
     */
    @OneToOne(fetch = FetchType.LAZY)
    val account: Account,

    /**
     * Дата и время истечения срока действия токена
     */
    val expiryDate: Date
)
