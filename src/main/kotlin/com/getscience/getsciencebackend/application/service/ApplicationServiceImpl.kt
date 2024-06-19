package com.getscience.getsciencebackend.application.service

import com.getscience.getsciencebackend.application.data.dto.ApplicationRequest
import com.getscience.getsciencebackend.application.data.dto.ApplicationResponse
import com.getscience.getsciencebackend.application.repository.ApplicationRepository
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.file.service.FileApplicationService
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class ApplicationServiceImpl(
    private val applicationRepository: ApplicationRepository,
    private val eventRepository: EventRepository,
    private val profileRepository: ProfileRepository,
    private val fileService: FileApplicationService,
    private val coroutineScope: CoroutineScope
) : ApplicationService {

    @Transactional
    @CachePut(value = ["applications"], key = "#result.applicationId", condition = "#result != null")
    override fun createApplication(
        applicationRequest: ApplicationRequest,
        files: List<MultipartFile>,
        isEncryptionEnabled: Boolean,
        email: String
    ): ApplicationResponse {
        val event = eventRepository.findById(applicationRequest.eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw RuntimeException("Profile not found")

        val application = applicationRequest.toEntity(event, profile)

        if (files.isNotEmpty()) {
            val fileApplications = fileService.uploadFiles(files, application, isEncryptionEnabled)

            if (fileApplications.isNotEmpty()) {
                application.fileApplications = fileApplications.toMutableList()
            }
        }

        return ApplicationResponse.fromEntity(applicationRepository.save(application))

    }

    @Transactional
    @CachePut(value = ["applications"], key = "#applicationId", condition = "#result != null")
    override fun updateApplication(
        applicationId: Long,
        files: List<MultipartFile>,
        applicationRequest: ApplicationRequest,
        isEncryptionEnabled: Boolean,
        email: String
    ): ApplicationResponse {
        val user = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Applicant not found")
        val application = applicationRepository.findById(applicationId).orElseThrow {
            RuntimeException("Application not found")
        }
        if (application.profile.profileId != user.profileId && application.event.organizer.profileId != user.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }

        val fileApplications = fileService.uploadFiles(files, application, isEncryptionEnabled)

        if (fileApplications.isNotEmpty()) {
            application.fileApplications = fileApplications.toMutableList()
        }

        val updatedApplication = application.copy(
            status = applicationRequest.status,
            submissionDate = applicationRequest.submissionDate,
            message = applicationRequest.message,
            isObserver = applicationRequest.isObserver,
            event = eventRepository.findById(applicationRequest.eventId).orElseThrow {
                RuntimeException("Event not found")
            },
            fileApplications = (application.fileApplications.union(fileApplications)).toMutableList()
        )

        return ApplicationResponse.fromEntity(applicationRepository.save(updatedApplication))

    }

    override fun updateApplicationByOrganizer(
        applicationId: Long,
        applicationRequest: ApplicationRequest,
        email: String
    ): ApplicationResponse {
        val organizer = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Organizer not found")
        val application = applicationRepository.findById(applicationId).orElseThrow {
            RuntimeException("Application not found")
        }
        if (application.event.organizer.profileId != organizer.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }

        val updatedApplication = application.copy(
            status = applicationRequest.status,
            submissionDate = applicationRequest.submissionDate,
            message = applicationRequest.message,
            isObserver = applicationRequest.isObserver,
            event = eventRepository.findById(applicationRequest.eventId).orElseThrow {
                RuntimeException("Event not found")
            },
            verdict = applicationRequest.verdict?:"",
            fileApplications = (application.fileApplications)
        )

        return ApplicationResponse.fromEntity(applicationRepository.save(updatedApplication))
    }


    @Transactional
    @Cacheable(value = ["applications"], key = "applicationId", condition = "#result != null")
    override fun getApplicationById(applicationId: Long, email: String): ApplicationResponse {
        val user = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Applicant not found")
        val application = applicationRepository.findById(applicationId).orElseThrow {
            RuntimeException("Application not found")
        }
        if (application.profile.profileId != user.profileId && application.event.organizer.profileId != user.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }
        return ApplicationResponse.fromEntity(application)
    }

    @Cacheable(value = ["applicationsByEvent"], key = "#eventId", condition = "#result != null")
    override fun getApplicationsByEvent(eventId: Long): List<ApplicationResponse> {
        val applications = applicationRepository.findByEventEventId(eventId)
        return applications.map { ApplicationResponse.fromEntity(it) }
    }

    @Cacheable(value = ["applicationsByProfile"], key = "#profileId", condition = "#result != null")
    override fun getApplicationsByProfile(profileId: Long): List<ApplicationResponse> {
        val applications = applicationRepository.findByProfileProfileId(profileId)
        return applications.map { ApplicationResponse.fromEntity(it) }
    }

    @Cacheable(value = ["applicationsByEvent"], key = "#eventId", condition = "#result != null")
    override fun getApplicationsByOrganizer(email: String): List<ApplicationResponse> {
        val applications = applicationRepository.findByEventOrganizerAccountEmail(email)
        return applications.map { ApplicationResponse.fromEntity(it) }
    }

    @Cacheable(value = ["applicationsByProfile"], key = "#profileId", condition = "#result != null")
    override fun getApplicationsByApplicant(email: String): List<ApplicationResponse> {
        val applications = applicationRepository.findByProfileAccountEmail(email)
        return applications.map { ApplicationResponse.fromEntity(it) }
    }

    @Transactional
    @CacheEvict(
        value = ["applications", "applicationsByProfile", "applicationsByEvent"],
        key = "#profileId",
        condition = "#result != null"
    )
    override fun deleteApplication(applicationId: Long, email: String): Boolean {
        val applicant = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Applicant not found")
        val application = applicationRepository.findById(applicationId).orElseThrow {
            RuntimeException("Application not found")
        }
        if (application.profile.profileId != applicant.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }
        applicationRepository.deleteById(applicationId)
        return true
    }
}