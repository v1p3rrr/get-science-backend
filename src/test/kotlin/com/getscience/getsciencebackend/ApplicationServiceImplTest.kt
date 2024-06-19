package com.getscience.getsciencebackend

import com.getscience.getsciencebackend.application.repository.ApplicationRepository
import com.getscience.getsciencebackend.application.service.ApplicationServiceImpl
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.file.service.FileApplicationService
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplicationServiceImplTest {

    private lateinit var applicationRepository: ApplicationRepository
    private lateinit var eventRepository: EventRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var fileService: FileApplicationService
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var applicationService: ApplicationServiceImpl

    @BeforeEach
    fun setUp() {
        applicationRepository = mockk(relaxed = true)
        eventRepository = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)
        fileService = mockk(relaxed = true)
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        applicationService = ApplicationServiceImpl(applicationRepository, eventRepository, profileRepository, fileService, coroutineScope)
    }

    @Test
    fun `getApplicationsByEvent returns applications for valid event id`() {
        applicationService.getApplicationsByEvent(1)

        coVerify { applicationRepository.findByEventEventId(any()) }
    }

    @Test
    fun `getApplicationsByProfile returns applications for valid profile id`() {
        applicationService.getApplicationsByProfile(1)

        coVerify { applicationRepository.findByProfileProfileId(any()) }
    }

    @Test
    fun `getApplicationsByOrganizer returns applications for valid organizer email`() {
        val email = "test@test.com"

        applicationService.getApplicationsByOrganizer(email)

        coVerify { applicationRepository.findByEventOrganizerAccountEmail(any()) }
    }

    @Test
    fun `getApplicationsByApplicant returns applications for valid applicant email`() {
        val email = "test@test.com"

        applicationService.getApplicationsByApplicant(email)

        coVerify { applicationRepository.findByProfileAccountEmail(any()) }
    }
}