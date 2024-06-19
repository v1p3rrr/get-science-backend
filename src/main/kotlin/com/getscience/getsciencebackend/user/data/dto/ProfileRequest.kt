package com.getscience.getsciencebackend.user.data.dto

import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.data.model.Role

data class ProfileRequest(
    val firstName: String,
    val lastName: String
) {
    fun toEntity(account: Account, role: Role): Profile {
        return Profile(
            firstName = this.firstName,
            lastName = this.lastName,
            role = role,
            account = account
        )
    }
}
