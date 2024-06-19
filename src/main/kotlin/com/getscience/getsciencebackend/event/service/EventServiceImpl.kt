package com.getscience.getsciencebackend.event.service

import com.getscience.getsciencebackend.event.data.dto.EventRequest
import com.getscience.getsciencebackend.event.data.dto.EventResponse
import com.getscience.getsciencebackend.event.repository.DocRequiredRepository
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.data.model.FileEvent
import com.getscience.getsciencebackend.file.repository.FileEventRepository
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventServiceImpl(
    private val eventRepository: EventRepository,
    private val profileRepository: ProfileRepository,
    private val docRequiredRepository: DocRequiredRepository,
    private val fileEventRepository: FileEventRepository
) : EventService {

    @CachePut(value = ["events"], condition = "#result != null && #result != false")
    override fun createEvent(eventRequest: EventRequest, email: String): Boolean {

        val organizer = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Organizer not found")

        val event = eventRequest.toEntity(organizer)
        eventRepository.save(event)
        return true
    }

    @CachePut(value = ["events"], condition = "#result != null && #result != false")
    override fun updateEvent(eventId: Long, eventRequest: EventRequest, email: String): Boolean {
        val organizer = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Organizer not found")
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        if (event.organizer.profileId != organizer.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }

        event.apply {
            title = eventRequest.title
            description = eventRequest.description
            organizerDescription = eventRequest.organizerDescription
            dateStart = eventRequest.dateStart
            dateEnd = eventRequest.dateEnd
            location = eventRequest.location
            type = eventRequest.type
            theme = eventRequest.theme
            format = eventRequest.format
            results = eventRequest.results
            observersAllowed = eventRequest.observersAllowed
            documentsRequired.clear()
            documentsRequired.addAll(eventRequest.documentsRequired.map { it.toEntity(this) })

            fileEvents.clear()
            fileEvents.addAll(eventRequest.fileEvents.map { it.toEntity(this) })
        }

        return true
    }

    @Cacheable(value = ["events"], key = "#eventId", condition = "#result != null")
    override fun getEventById(eventId: Long): EventResponse {
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }

        return EventResponse.fromEntity(event)
    }

    @Cacheable(value = ["eventsByOrganizer"], key = "#organizerId", condition = "#result != null")
    override fun getEventsByOrganizer(organizerId: Long): List<EventResponse> {
        val events = eventRepository.findByOrganizerProfileId(organizerId)
        return events.map { event -> EventResponse.fromEntity(event) }
    }

    @Cacheable(value = ["events"], key = "#root.methodName + ':' + #email", condition = "#result != null")
    override fun getEventsByJwt(email: String): List<EventResponse> {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Profile not found")
        val events = eventRepository.findByOrganizerProfileId(profile.profileId)
        return events.map { event -> EventResponse.fromEntity(event) }
    }

    @Cacheable(value = ["events"], key = "#root.methodName + ':' + #type + ':' + #theme + ':' + #location + ':' + #format + ':' + #title", condition = "#result != null")
    override fun searchEvents(type: String?, theme: String?, location: String?, format: String?, title: String?): List<EventResponse> {
        if (type == null && theme == null && location == null &&  format == null && title == null) {
            return eventRepository.findAll().map { event -> EventResponse.fromEntity(event) }
        }
        val events = eventRepository.findByTypeOrThemeOrLocationOrFormatOrTitleContainsIgnoreCase(type, theme, location, format, title)
        return events.map { event -> EventResponse.fromEntity(event) }
    }

    override fun getRecommendations(eventId: Long, limit: Int): List<EventResponse> {
        val originalEvent = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        val theme = originalEvent.theme ?: return emptyList()
        val events = eventRepository.findEventsByTheme(theme).filter { it.eventId!=eventId } .shuffled().take(limit)
        return events.map { event -> EventResponse.fromEntity(event) }
    }

    @CacheEvict(value = ["fileEvents"], key = "#eventId", condition = "#result != null")
    override fun addFileToEvent(eventId: Long, fileEventRequest: FileEventRequest, email: String): FileEvent {
        val organizer = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Organizer not found")
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        if (event.organizer.profileId != organizer.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }

        val fileEvent = fileEventRequest.toEntity(event)
        return fileEventRepository.save(fileEvent)
    }

    @Cacheable(value = ["fileEventsByEvent"], key = "#eventId", condition = "#result != null")
    override fun getFilesByEvent(eventId: Long): List<FileEventResponse> {
        val fileEvents = fileEventRepository.findByEventEventId(eventId)
        return fileEvents.map { fileEvent -> FileEventResponse.fromEntity(fileEvent) }
    }

    @CacheEvict(value = ["events"], key = "#eventId", condition = "#result != null")
    override fun deleteEvent(eventId: Long, email: String): Boolean {
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        if (event.organizer.account.email != email) {
            throw IllegalArgumentException("Unauthorized")
        }
        eventRepository.deleteById(eventId)
        return true
    }

    @Cacheable(value = ["events"], condition = "#result != null")
    override fun getAllEvents(): List<EventResponse> {
        val events = eventRepository.findAll()
        return events.map { event -> EventResponse.fromEntity(event) }
    }

    @Cacheable(value = ["fileEvents"], key = "#fileId", condition = "#result != null")
    override fun getFileEventById(fileId: Long): FileEventResponse {
        val fileEvent = fileEventRepository.findById(fileId).orElseThrow {
            RuntimeException("FileEvent not found")
        }
        return FileEventResponse.fromEntity(fileEvent)
    }


}
