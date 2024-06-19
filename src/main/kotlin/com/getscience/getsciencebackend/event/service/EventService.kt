package com.getscience.getsciencebackend.event.service

import com.getscience.getsciencebackend.event.data.dto.EventRequest
import com.getscience.getsciencebackend.event.data.dto.EventResponse
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.data.model.FileEvent

interface EventService {
    fun createEvent(eventRequest: EventRequest, email: String): Boolean
    fun updateEvent(eventId: Long, eventRequest: EventRequest, email: String): Boolean
    fun getEventById(eventId: Long): EventResponse
    fun getEventsByOrganizer(organizerId: Long): List<EventResponse>
    fun deleteEvent(eventId: Long, email: String): Boolean
    fun getAllEvents(): List<EventResponse>
    fun getEventsByJwt(email: String): List<EventResponse>
    fun searchEvents(type: String?, theme: String?, location: String?, format:String?, title: String?): List<EventResponse>
    fun getRecommendations(eventId: Long, limit: Int): List<EventResponse>
    fun addFileToEvent(eventId: Long, fileEventRequest: FileEventRequest, email: String): FileEvent
    fun getFilesByEvent(eventId: Long): List<FileEventResponse>
    fun getFileEventById(fileId: Long): FileEventResponse
}
