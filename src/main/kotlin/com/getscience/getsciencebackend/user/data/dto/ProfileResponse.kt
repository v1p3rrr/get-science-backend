package com.getscience.getsciencebackend.user.data.dto

import com.getscience.getsciencebackend.user.data.model.Profile
import java.io.Serializable

data class EventShortDto(
    val eventId: Long,
    val title: String
)

data class ProfileResponse(
    val profileId: Long,
    val firstName: String,
    val lastName: String,
    val aboutMe: String?,
    val avatarUrl: String?,
    val avatarPresignedUrl: String?,
    val role: String,
    val email: String,
    val reviewEvents: List<EventShortDto> = emptyList(),
    val coownerEvents: List<EventShortDto> = emptyList(),
    val isActive: Boolean? = null
) : Serializable {
    companion object {
        fun fromEntity(
            profile: Profile,
            avatarPresignedUrl: String? = null,
            isActive: Boolean? = null
        ): ProfileResponse {
            return ProfileResponse(
                profileId = profile.profileId,
                firstName = profile.firstName,
                lastName = profile.lastName,
                aboutMe = profile.aboutMe,
                avatarUrl = profile.avatarUrl,
                avatarPresignedUrl = avatarPresignedUrl,
                role = profile.role.title.name,
                email = profile.account.email,
                reviewEvents = profile.reviewEvents.map { event -> EventShortDto(event.eventId, event.title) },
                coownerEvents = profile.coownerEvents.map { event -> EventShortDto(event.eventId, event.title) },
                isActive = isActive
            )
        }
    }
}