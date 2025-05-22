package com.getscience.getsciencebackend.user.data.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable

@Entity
@Table(name = "account")
data class Account(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    val accountId: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    @JsonIgnore
    val passwordHash: String,

    @OneToOne(mappedBy = "account", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonIgnoreProperties("account")
    var profile: Profile? = null,

    @Column(nullable = false)
    val emailConfirmed: Boolean = false
) : Serializable, UserDetails {
    override fun hashCode(): Int {
        var result = accountId.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + passwordHash.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        if (accountId != other.accountId) return false
        if (email != other.email) return false
        if (passwordHash != other.passwordHash) return false

        return true
    }

    override fun toString(): String {
        return "Account(accountId=$accountId, email='$email', passwordHash='$passwordHash')"
    }

    @JsonIgnore
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(GrantedAuthority { profile?.role?.title?.name })
    }

    @JsonIgnore
    override fun getPassword(): String {
        return passwordHash
    }

    @JsonIgnore
    override fun getUsername(): String {
        return email
    }
}