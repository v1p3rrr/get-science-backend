package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import java.io.Serializable
import java.util.*

data class EventResponse(
    val eventId: Long,
    val title: String,
    val description: String,
    val organizerDescription: String?,
    val dateStart: Date,
    val dateEnd: Date,
    val location: String,
    val type: String,
    val theme: String?,
    val format: String,
    val observersAllowed: Boolean,
    val organizerId: Long,
    val organizer: String,
    val documentsRequired: List<DocRequiredResponse>,
    val fileEvents: List<FileEventResponse>,
    val results: String?
) : Serializable {
    companion object {
        fun fromEntity(event: Event): EventResponse {
            return EventResponse(
                eventId = event.eventId,
                title = event.title,
                description = event.description,
                organizerDescription = event.organizerDescription,
                dateStart = event.dateStart,
                dateEnd = event.dateEnd,
                location = event.location,
                type = event.type,
                theme = event.theme,
                format = event.format,
                observersAllowed = event.observersAllowed,
                organizerId = event.organizer.profileId,
                organizer = ("${event.organizer.account.profile?.firstName} ${event.organizer.account.profile?.lastName}"),
                documentsRequired = event.documentsRequired.map { DocRequiredResponse.fromEntity(it) },
                fileEvents = event.fileEvents.map { FileEventResponse.fromEntity(it) },
                results = event.results
            )
        }
    }
}