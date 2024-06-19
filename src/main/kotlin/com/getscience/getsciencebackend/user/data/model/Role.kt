package com.getscience.getsciencebackend.user.data.model

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "role")
data class Role(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val roleId: Long = 0,

    @Column(nullable = false)
    val title: String
) : Serializable {
    override fun hashCode(): Int {
        var result = roleId.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Role

        if (roleId != other.roleId) return false
        if (title != other.title) return false

        return true
    }

    override fun toString(): String {
        return "Role(roleId=$roleId, title='$title')"
    }
}
