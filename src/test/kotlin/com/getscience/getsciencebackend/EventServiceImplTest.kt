package com.getscience.getsciencebackend

import com.getscience.getsciencebackend.event.data.dto.EventRequest
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.event.repository.DocRequiredRepository
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.event.service.EventServiceImpl
import com.getscience.getsciencebackend.file.repository.FileEventRepository
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class EventServiceImplTest {

    private lateinit var eventRepository: EventRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var docRequiredRepository: DocRequiredRepository
    private lateinit var fileEventRepository: FileEventRepository
    private lateinit var eventService: EventServiceImpl

    @BeforeEach
    fun setUp() {
        eventRepository = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)
        docRequiredRepository = mockk(relaxed = true)
        fileEventRepository = mockk(relaxed = true)
        val organzier = mockk<Profile>()
        eventService = EventServiceImpl(eventRepository, profileRepository, docRequiredRepository, fileEventRepository)
        every { eventRepository.findById(any()) } returns Optional.empty()
        every { eventRepository.save(any()) } returns Event(1, "title", "description", "organizerDescription", Date(), Date(), "location", "type", "theme", true, organzier, "", emptyList())

    }

    @Test
    fun `createEvent returns true when valid inputs are provided`() = runBlocking {
        val eventRequest = EventRequest(
            title = "title",
            description = "description",
            organizerDescription = "organizerDescription",
            dateStart = Date(),
            dateEnd = Date(),
            location = "location",
            type = "type",
            theme = "theme",
            observersAllowed = true,
            organizerId = 1L,
            documentsRequired = emptyList(),
            fileEvents = emptyList()
        )
        val email = "test@test.com"

        val result = eventService.createEvent(eventRequest, email)

        assertEquals(true, result)
        coVerify { eventRepository.save(any()) }
    }



    @Test
    fun `updateEvent throws exception when invalid inputs are provided`(): Unit = runBlocking {
        val eventRequest = EventRequest(
            title = "title",
            description = "description",
            organizerDescription = "organizerDescription",
            dateStart = Date(),
            dateEnd = Date(),
            location = "location",
            type = "type",
            theme = "theme",
            observersAllowed = true,
            organizerId = 1L,
            documentsRequired = emptyList(),
            fileEvents = emptyList()
        )
        val email = "invalid@test.com"

        assertThrows<RuntimeException> {
            eventService.updateEvent(-1L, eventRequest, email)
        }
    }

    @Test
    fun `deleteEvent throws exception when invalid inputs are provided`(): Unit = runBlocking {
        assertThrows<RuntimeException> {
            eventService.updateEvent(-1L, mockk(), "invalid@test.com")
        }
    }
}