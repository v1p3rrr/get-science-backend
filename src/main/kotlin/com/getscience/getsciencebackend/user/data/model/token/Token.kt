package com.getscience.getsciencebackend.user.data.model.token

import com.getscience.getsciencebackend.user.data.model.Account
import jakarta.persistence.*
import java.util.*

/**
 * Токен для выполнения операций, требующих подтверждения.
 * 
 * Используется для верификации email и сброса пароля.
 * Каждый токен имеет тип, определяющий его назначение, срок действия и
 * может быть использован только один раз.
 */
@Entity
@Table(name = "token")
data class Token(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /**
     * Строковое значение токена (UUID)
     */
    @Column(nullable = false, unique = true)
    val token: String,

    /**
     * Аккаунт пользователя, связанный с токеном
     */
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    /**
     * Тип токена, определяющий его назначение (верификация email или сброс пароля)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: TokenType,

    /**
     * Дата и время истечения срока действия токена
     */
    @Column(nullable = false)
    val expiresAt: Date,

    /**
     * Флаг, указывающий, был ли токен уже использован
     */
    @Column(nullable = false)
    val used: Boolean = false
)
