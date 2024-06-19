package com.getscience.getsciencebackend.user.data.model

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "profile")
data class Profile(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val profileId: Long = 0,

    @Column(nullable = false)
    val firstName: String,

    @Column(nullable = false)
    val lastName: String,

    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "accountId")
    val account: Account,

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "roleId")
    val role: Role
) : Serializable {
    override fun hashCode(): Int {
        var result = profileId.hashCode()
        result = 31 * result + firstName.hashCode()
        result = 31 * result + lastName.hashCode()
        result = 31 * result + role.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Profile

        if (profileId != other.profileId) return false
        if (firstName != other.firstName) return false
        if (lastName != other.lastName) return false
        if (role != other.role) return false

        return true
    }

    override fun toString(): String {
        return "Profile(profileId=$profileId, firstName='$firstName', lastName='$lastName', role=$role)"
    }
}