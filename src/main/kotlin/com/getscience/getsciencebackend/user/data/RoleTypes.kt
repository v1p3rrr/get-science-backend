package com.getscience.getsciencebackend.user.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class RoleTypes(val role: String) {
    companion object {
        const val USER = "USER"
        const val ORGANIZER = "ORGANIZER"

        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): RoleTypes {
            return RoleTypes(value)
        }
    }

    @JsonValue
    override fun toString(): String {
        return role
    }
}