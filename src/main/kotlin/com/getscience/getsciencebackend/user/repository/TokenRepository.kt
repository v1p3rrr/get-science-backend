package com.getscience.getsciencebackend.user.repository

import com.getscience.getsciencebackend.user.data.model.token.Token
import com.getscience.getsciencebackend.user.data.model.token.TokenType
import com.getscience.getsciencebackend.user.data.model.Account
import org.springframework.data.jpa.repository.JpaRepository

interface TokenRepository : JpaRepository<Token, Long> {
    fun findByTokenAndType(token: String, type: TokenType): Token?
    fun deleteAllByAccountAndType(account: Account, type: TokenType)
}
