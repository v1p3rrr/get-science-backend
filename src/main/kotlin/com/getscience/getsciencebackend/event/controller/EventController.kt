package com.getscience.getsciencebackend.event.controller

import com.getscience.getsciencebackend.event.data.dto.EventRequest
import com.getscience.getsciencebackend.event.data.dto.EventResponse
import com.getscience.getsciencebackend.event.service.EventService
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/events")
class EventController(
    private val eventService: EventService
) {

    @PostMapping
    fun createEvent(@RequestBody eventRequest: EventRequest): ResponseEntity<String> {
        val email = getEmailFromToken()
        val result = eventService.createEvent(eventRequest, email)
        return if (result) {
            ResponseEntity.ok("Success")
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{eventId}")
    @PostMapping("/{eventId}/update")
    fun updateEvent(@PathVariable eventId: Long, @RequestBody eventRequest: EventRequest): ResponseEntity<String> {
        val email = getEmailFromToken()
        val result = eventService.updateEvent(eventId, eventRequest, email)
        return if (result) {
            ResponseEntity.ok("Success")
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{eventId}")
    fun getEventById(@PathVariable eventId: Long): ResponseEntity<EventResponse> {
        val event = eventService.getEventById(eventId)
        return ResponseEntity.ok(event)
    }

    @GetMapping("/organizer/{organizerId}")
    fun getEventsByOrganizer(@PathVariable organizerId: Long): ResponseEntity<List<EventResponse>> {
        val events = eventService.getEventsByOrganizer(organizerId)
        return ResponseEntity.ok(events)
    }

    @DeleteMapping("/{eventId}")
    fun deleteEvent(@PathVariable eventId: Long): ResponseEntity<String> {
        val email = getEmailFromToken()
        val result = eventService.deleteEvent(eventId, email)
        return if (result) {
            ResponseEntity.ok("Success")
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping
    fun getAllEvents(): ResponseEntity<List<EventResponse>> {
        val events = eventService.getAllEvents()
        return ResponseEntity.ok(events)
    }

    @GetMapping("/my-events")
    fun getEventsByJwt(): ResponseEntity<List<EventResponse>> {
        val email = getEmailFromToken()
        val events = eventService.getEventsByJwt(email)
        return ResponseEntity.ok(events)
    }

    @GetMapping("/search")
    fun searchEvents(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) theme: String?,
        @RequestParam(required = false) location: String?,
        @RequestParam(required = false) format: String?,
        @RequestParam(required = false) title: String?
    ): ResponseEntity<List<EventResponse>> {
        val events = eventService.searchEvents(type, theme, location, format, title)
        return ResponseEntity.ok(events)
    }

    @GetMapping("/{eventId}/recommendations")
    fun searchEvents(@PathVariable eventId: Long
    ): ResponseEntity<List<EventResponse>> {
        val events = eventService.getRecommendations(eventId, 4)
        return ResponseEntity.ok(events)
    }

    @PostMapping("/{eventId}/files")
    fun addFileToEvent(@PathVariable eventId: Long, @RequestBody fileEventRequest: FileEventRequest): ResponseEntity<FileEventResponse> {
        val email = getEmailFromToken()
        val fileEvent = eventService.addFileToEvent(eventId, fileEventRequest, email)
        return ResponseEntity.ok(FileEventResponse.fromEntity(fileEvent))
    }

    @GetMapping("/{eventId}/files")
    fun getFilesByEvent(@PathVariable eventId: Long): ResponseEntity<List<FileEventResponse>> {
        val files = eventService.getFilesByEvent(eventId)
        return ResponseEntity.ok(files)
    }

    @GetMapping("/files/{fileId}")
    fun getFileEventById(@PathVariable fileId: Long): ResponseEntity<FileEventResponse> {
        val fileEvent = eventService.getFileEventById(fileId)
        return ResponseEntity.ok(fileEvent)
    }

    private fun getEmailFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        return email
    }
}