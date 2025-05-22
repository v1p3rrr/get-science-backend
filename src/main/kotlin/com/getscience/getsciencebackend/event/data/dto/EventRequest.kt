package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.event.data.model.EventFormat
import com.getscience.getsciencebackend.event.data.model.EventModerationStatus
import com.getscience.getsciencebackend.event.data.model.EventStatus
import com.getscience.getsciencebackend.event.data.model.EventType
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.user.data.model.Profile
import java.util.*

data class EventRequest(
    val title: String,
    val description: String,
    val organizerDescription: String?,
    val dateStart: Date,
    val dateEnd: Date,
    val applicationStart: Date,
    val applicationEnd: Date,
    val location: String,
    val type: EventType,
    val theme: String?,
    val format: EventFormat,
    val results: String?,
    val observersAllowed: Boolean,
    val status: EventStatus,
    val moderationStatus: EventModerationStatus,
    val hidden: Boolean,
    val organizerId: Long?,
    val documentsRequired: List<DocRequiredRequest>,
    val fileEvents: List<FileEventRequest>,
    val reviewers: List<ReviewerDto> = emptyList(),
    val coowners: Set<ReviewerDto> = emptySet()
) {
    fun toEntity(
        profile: Profile,
        reviewerProfiles: List<Profile> = emptyList(),
        coownerProfiles: Set<Profile> = emptySet(),
        applications: List<Application> = emptyList(),
        isOrganizer: Boolean = false
    ): Event {
        val event = Event(
            title = title,
            description = description,
            organizerDescription = organizerDescription,
            dateStart = dateStart,
            dateEnd = dateEnd,
            applicationStart = applicationStart,
            applicationEnd = applicationEnd,
            location = location,
            type = type,
            theme = theme,
            format = format,
            results = results,
            moderationStatus = moderationStatus,
            observersAllowed = observersAllowed,
            status = status,
            hidden = hidden,
            organizer = profile
        )
        event.documentsRequired.clear()
        event.documentsRequired.addAll(documentsRequired.map { it.toEntity(event) })

        event.fileEvents.clear()
        event.fileEvents.addAll(fileEvents.map { it.toEntity(event) })

        event.reviewers.clear()
        event.reviewers.addAll(reviewerProfiles)

        event.applications.clear()
        event.applications.addAll(applications)

        if (isOrganizer) {
            event.coowners.clear()
            event.coowners.addAll(coownerProfiles)
        }

        return event
    }
}