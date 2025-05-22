package com.getscience.getsciencebackend.user.repository

import com.getscience.getsciencebackend.user.data.model.Account
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    fun findByEmail(email: String): Account?
}