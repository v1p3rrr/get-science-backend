package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.user.data.model.Profile
import java.util.*

data class EventRequest(
    val title: String,
    val description: String,
    val organizerDescription: String?,
    val dateStart: Date,
    val dateEnd: Date,
    val location: String,
    val type: String,
    val theme: String?,
    val format: String,
    val results: String?,
    val observersAllowed: Boolean,
    val organizerId: Long?,
    val documentsRequired: List<DocRequiredRequest>,
    val fileEvents: List<FileEventRequest>
) {
    fun toEntity(profile: Profile): Event {
        val event = Event(
            title = title,
            description = description,
            organizerDescription = organizerDescription,
            dateStart = dateStart,
            dateEnd = dateEnd,
            location = location,
            type = type,
            theme = theme,
            format = format,
            results = results,
            observersAllowed = observersAllowed,
            organizer = profile
        )
        event.documentsRequired.clear()
        event.documentsRequired.addAll(documentsRequired.map { it.toEntity(event) })

        event.fileEvents.clear()
        event.fileEvents.addAll(fileEvents.map { it.toEntity(event) })

        return event
    }
}