package com.getscience.getsciencebackend.user.data.dto

import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.data.model.Role

data class ProfileRequest(
    val firstName: String,
    val lastName: String,
    val aboutMe: String? = null,
    val avatarUrl: String? = null
) {
    fun toEntity(account: Account, role: Role): Profile {
        return Profile(
            firstName = this.firstName,
            lastName = this.lastName,
            aboutMe = this.aboutMe,
            avatarUrl = this.avatarUrl,
            role = role,
            account = account
        )
    }
}
