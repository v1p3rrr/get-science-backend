package com.getscience.getsciencebackend.application.service

import com.getscience.getsciencebackend.application.data.dto.ApplicationRequest
import com.getscience.getsciencebackend.application.data.dto.ApplicationResponse
import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.application.repository.ApplicationRepository
import com.getscience.getsciencebackend.config.CoroutineTest
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.event.data.model.EventFormat
import com.getscience.getsciencebackend.event.data.model.EventType
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.file.service.FileApplicationService
import com.getscience.getsciencebackend.file.service.S3Service
import com.getscience.getsciencebackend.notification.data.ApplicationStatus
import com.getscience.getsciencebackend.notification.service.NotificationEventService
import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.model.Account
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.data.model.Role
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import java.util.*


@CoroutineTest
class ApplicationServiceImplTest {

    @Mock
    private lateinit var applicationRepository: ApplicationRepository
    @Mock
    private lateinit var eventRepository: EventRepository
    @Mock
    private lateinit var profileRepository: ProfileRepository
    @Mock
    private lateinit var fileService: FileApplicationService
    @Mock
    private lateinit var fileApplicationServiceMock: FileApplicationService
    @Mock
    private lateinit var notificationEventService: NotificationEventService
    @Mock
    private lateinit var s3Service: S3Service

    @InjectMocks
    private lateinit var applicationService: ApplicationServiceImpl

    private lateinit var applicantProfile: Profile
    private lateinit var organizerProfile: Profile
    private lateinit var mockEvent: Event
    private lateinit var mockApplication: Application
    private val applicantEmail = "applicant@example.com"
    private val organizerEmail = "organizer@example.com"
    private val eventId = 1L
    private val applicantProfileId = 1L
    private val organizerProfileId = 2L

    @BeforeEach
    fun setUp() {
        applicationService = ApplicationServiceImpl(
            applicationRepository,
            eventRepository,
            profileRepository,
            fileService,
            notificationEventService,
            fileApplicationServiceMock,
            s3Service
        )

        val applicantAccount = Account(accountId = applicantProfileId, email = applicantEmail, passwordHash = "hash")
        val applicantRole = Role(roleId = 1L, title = RoleType.USER)
        applicantProfile = Profile(profileId = applicantProfileId, firstName = "Applicant", lastName = "User", account = applicantAccount, role = applicantRole)

        val organizerAccount = Account(accountId = organizerProfileId, email = organizerEmail, passwordHash = "hash")
        val organizerRole = Role(roleId = 2L, title = RoleType.ORGANIZER)
        organizerProfile = Profile(profileId = organizerProfileId, firstName = "Organizer", lastName = "User", account = organizerAccount, role = organizerRole)

        val now = Date()

        mockEvent = Event(
            eventId = eventId,
            title = "Test Event",
            description = "Description",
            organizer = organizerProfile,
            dateStart = Date(now.time - 10000),
            dateEnd = Date(now.time + 10000),
            location = "Test Location",
            type = EventType.CONFERENCE,
            format = EventFormat.OFFLINE,
            observersAllowed = true,
            applicationStart = Date(now.time - 10000),
            applicationEnd = Date(now.time + 10000)
        )

        mockApplication = Application(
            applicationId = 1L,
            event = mockEvent,
            profile = applicantProfile,
            status = ApplicationStatus.PENDING.name,
            submissionDate = Timestamp(System.currentTimeMillis()),
            isObserver = false
        )

        `when`(profileRepository.findByAccountEmail(applicantEmail)).thenReturn(applicantProfile)
        `when`(profileRepository.findById(applicantProfileId)).thenReturn(Optional.of(applicantProfile))
        `when`(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent))
        `when`(profileRepository.findByAccountEmail(organizerEmail)).thenReturn(organizerProfile)
    }

    @Test
    fun `createApplication successfully creates an application`() = runTest {
        val applicationRequest = ApplicationRequest(
            eventId = eventId,
            profileId = applicantProfileId,
            status = ApplicationStatus.PENDING.name,
            submissionDate = Timestamp(System.currentTimeMillis()),
            message = "My Application",
            isObserver = false,
            fileApplications = emptyList()
        )
        val mockFiles = listOf<MultipartFile>(mock(MultipartFile::class.java))

        `when`(applicationRepository.save(any(Application::class.java))).thenReturn(mockApplication)
        `when`(fileService.uploadFiles(anyList(), any(), anyList())).thenReturn(emptyList())

        val createdApplicationResponse = applicationService.createApplication(applicationRequest, emptyList(), mockFiles, applicantEmail)

        assertNotNull(createdApplicationResponse)
        assertEquals(applicantProfile.profileId, createdApplicationResponse.profileId)
        assertEquals(mockEvent.eventId, createdApplicationResponse.eventId)
        verify(applicationRepository).save(any(Application::class.java))
        verify(fileService).uploadFiles(any(), any(), any())
        verify(notificationEventService).handleNewApplication(anyLong(), eq(eventId), eq(organizerProfile.profileId))
    }

    @Test
    fun `getApplicationsByEvent returns applications for valid event id`() {
        applicationService.getApplicationsByEvent(eventId)
        verify(applicationRepository).findByEventEventId(eventId)
        }

    @Test
    fun `getApplicationsByOrganizer returns applications for valid organizer email`() {
        applicationService.getApplicationsByOrganizer(organizerEmail)
        verify(applicationRepository).findByEventOrganizerAccountEmail(organizerEmail)
    }

    @Test
    fun `getApplicationsByApplicant returns applications for valid applicant email`() {
        applicationService.getApplicationsByApplicant(applicantEmail)
        verify(applicationRepository).findByProfileAccountEmail(applicantEmail)
    }

    @Test
    fun `updateApplicationStatus updates status and notifies applicant`() = runTest {
        val applicationId = 1L
        val newStatus = ApplicationStatus.APPROVED
        val currentApplication = Application(
            applicationId = applicationId,
            profile = applicantProfile,
            event = mockEvent,
            message = "desc",
            status = ApplicationStatus.PENDING.name,
            submissionDate = Timestamp(System.currentTimeMillis()),
            isObserver = false
        )
        
        val applicationRequest = ApplicationRequest(
            eventId = mockEvent.eventId,
            profileId = applicantProfile.profileId,
            status = newStatus.name,
            submissionDate = Timestamp(System.currentTimeMillis()),
            message = currentApplication.message,
            isObserver = currentApplication.isObserver,
            fileApplications = emptyList(),
            verdict = "Approved by organizer"
        )
        
        val updatedApplication = currentApplication.copy(status = newStatus.name)
        val mockApplicationResponse = ApplicationResponse.fromEntity(updatedApplication)

        `when`(applicationRepository.findById(applicationId)).thenReturn(Optional.of(currentApplication))
        `when`(applicationRepository.save(any(Application::class.java))).thenReturn(updatedApplication)
        `when`(profileRepository.findByAccountEmail(organizerEmail)).thenReturn(organizerProfile)

        val resultResponse = applicationService.updateApplicationByOrganizer(applicationId, applicationRequest, organizerEmail)

        assertNotNull(resultResponse)
        assertEquals(newStatus.name, resultResponse.status)
        verify(notificationEventService).handleApplicationStatusChanged(applicationId, eventId, applicantProfileId, newStatus.name)
    }
} 