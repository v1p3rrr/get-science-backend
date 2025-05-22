package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.event.data.model.EventFormat
import com.getscience.getsciencebackend.event.data.model.EventType
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.service.S3Service
import java.io.Serializable
import java.util.*
import com.getscience.getsciencebackend.event.data.model.EventModerationStatus
import com.getscience.getsciencebackend.event.data.model.EventStatus

data class EventResponse(
    val eventId: Long,
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
    val hidden: Boolean,
    val moderationStatus: EventModerationStatus,
    val organizerId: Long,
    val organizer: String,
    val documentsRequired: List<DocRequiredResponse>,
    val fileEvents: List<FileEventResponse>,
    val reviewers: List<ReviewerDto>,
    val coowners: List<ReviewerDto>,
) : Serializable {
    companion object {
        fun fromEntity(event: Event, s3Service: S3Service? = null): EventResponse {
            return EventResponse(
                eventId = event.eventId,
                title = event.title,
                description = event.description,
                organizerDescription = event.organizerDescription,
                dateStart = event.dateStart,
                dateEnd = event.dateEnd,
                applicationStart = event.applicationStart,
                applicationEnd = event.applicationEnd,
                location = event.location,
                type = event.type,
                theme = event.theme,
                format = event.format,
                results = event.results,
                observersAllowed = event.observersAllowed,
                status = event.status,
                hidden = event.hidden,
                organizerId = event.organizer.profileId,
                organizer = ("${event.organizer.account.profile?.firstName} ${event.organizer.account.profile?.lastName}"),
                documentsRequired = event.documentsRequired.map { DocRequiredResponse.fromEntity(it) },
                fileEvents = event.fileEvents.map { FileEventResponse.fromEntity(it, s3Service) },
                reviewers = event.reviewers.map { ReviewerDto(it.profileId, it.account.email, it.firstName, it.lastName) },
                coowners = event.coowners.map { ReviewerDto(it.profileId, it.account.email, it.firstName, it.lastName) },
                moderationStatus = event.moderationStatus
            )
        }
    }
}