package com.getscience.getsciencebackend.application.data.dto

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.event.data.model.EventType
import com.getscience.getsciencebackend.event.data.model.FileType
import com.getscience.getsciencebackend.file.data.dto.FileApplicationResponse
import com.getscience.getsciencebackend.user.data.model.Profile
import java.io.Serializable
import java.sql.Timestamp
import java.util.Date

data class ApplicationResponse(
    val applicationId: Long,
    val eventId: Long,
    val eventName: String,
    val profileId: Long,
    val status: String,
    val submissionDate: Timestamp,
    val message: String?,
    val isObserver: Boolean,
    val verdict: String?,
    val fileApplications: List<FileApplicationResponse>
) : Serializable {
    companion object {
        fun fromEntity(application: Application): ApplicationResponse {
            return ApplicationResponse(
                applicationId = application.applicationId,
                eventId = application.event.eventId,
                eventName = application.event.title,
                profileId = application.profile.profileId,
                status = application.status,
                submissionDate = application.submissionDate,
                message = application.message,
                verdict = application.verdict,
                isObserver = application.isObserver,
                fileApplications = application.fileApplications.map { FileApplicationResponse.fromEntity(it) }
            )
        }
    }

    fun toEntity(event: Event, profile: Profile): Application {
        val application = Application(
            applicationId = applicationId,
            event = event,
            profile = profile,
            status = status,
            submissionDate = submissionDate,
            verdict = verdict,
            message = message,
            isObserver = isObserver
        )
        application.fileApplications = fileApplications.map { it.toEntity(application) }.toMutableList()
        return application
    }
}

// DTO для детальной информации о пользователе
data class ProfileDTO(
    val profileId: Long,
    val firstName: String,
    val lastName: String, 
    val email: String,
    val avatarUrl: String?,
    val aboutMe: String?
) : Serializable {
    companion object {
        fun fromEntity(profile: Profile): ProfileDTO {
            return ProfileDTO(
                profileId = profile.profileId,
                firstName = profile.firstName,
                lastName = profile.lastName,
                email = profile.account.email,
                avatarUrl = profile.avatarUrl,
                aboutMe = profile.aboutMe
            )
        }
    }
}

// DTO для документов, требуемых для события
data class DocRequiredDTO(
    val docRequiredId: Long,
    val type: String,
    val fileType: FileType,
    val description: String,
    val mandatory: Boolean
) : Serializable

// DTO для события
data class EventDTO(
    val eventId: Long,
    val title: String,
    val description: String,
    val location: String,
    val type: EventType,
    val dateStart: Date,
    val dateEnd: Date,
    val applicationStart: Date,
    val applicationEnd: Date,
    val documentsRequired: List<DocRequiredDTO>
) : Serializable {
    companion object {
        fun fromEntity(event: Event): EventDTO {
            return EventDTO(
                eventId = event.eventId,
                title = event.title,
                description = event.description,
                location = event.location,
                type = event.type,
                dateStart = event.dateStart,
                dateEnd = event.dateEnd,
                applicationStart = event.applicationStart,
                applicationEnd = event.applicationEnd,
                documentsRequired = event.documentsRequired.map { doc ->
                    DocRequiredDTO(
                        docRequiredId = doc.docRequiredId,
                        type = doc.type,
                        fileType = doc.fileType,
                        description = doc.description,
                        mandatory = doc.mandatory
                    )
                }
            )
        }
    }
}

// Детальный ответ с заявкой и профилем подающего
data class ApplicationDetailWithApplicantResponse(
    val application: ApplicationResponse,
    val event: EventDTO,
    val applicant: ProfileDTO
) : Serializable {
    companion object {
        fun fromEntity(application: Application): ApplicationDetailWithApplicantResponse {
            return ApplicationDetailWithApplicantResponse(
                application = ApplicationResponse.fromEntity(application),
                event = EventDTO.fromEntity(application.event),
                applicant = ProfileDTO.fromEntity(application.profile)
            )
        }
    }
}

// Детальный ответ с заявкой и профилем организатора
data class ApplicationDetailWithOrganizerResponse(
    val application: ApplicationResponse,
    val event: EventDTO,
    val organizer: ProfileDTO
) : Serializable {
    companion object {
        fun fromEntity(application: Application): ApplicationDetailWithOrganizerResponse {
            return ApplicationDetailWithOrganizerResponse(
                application = ApplicationResponse.fromEntity(application),
                event = EventDTO.fromEntity(application.event),
                organizer = ProfileDTO.fromEntity(application.event.organizer)
            )
        }
    }
}