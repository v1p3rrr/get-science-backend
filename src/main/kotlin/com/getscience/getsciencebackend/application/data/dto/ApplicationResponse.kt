package com.getscience.getsciencebackend.application.data.dto

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.dto.FileApplicationResponse
import com.getscience.getsciencebackend.user.data.model.Profile
import java.io.Serializable
import java.sql.Timestamp

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