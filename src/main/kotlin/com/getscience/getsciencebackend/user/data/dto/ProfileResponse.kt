package com.getscience.getsciencebackend.user.data.dto

import java.io.Serializable

data class ProfileResponse(
    val profileId: Long,
    val firstName: String,
    val lastName: String,
    val role: String,
    val email: String
) : Serializable {
    companion object {
        fun fromEntity(profile: com.getscience.getsciencebackend.user.data.model.Profile): ProfileResponse {
            return ProfileResponse(
                profileId = profile.profileId,
                firstName = profile.firstName,
                lastName = profile.lastName,
                role = profile.role.title,
                email = profile.account.email
            )
        }
    }
}