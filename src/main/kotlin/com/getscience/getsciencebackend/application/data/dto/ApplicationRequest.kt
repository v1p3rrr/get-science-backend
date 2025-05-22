package com.getscience.getsciencebackend.application.data.dto

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.dto.FileApplicationRequest
import com.getscience.getsciencebackend.user.data.model.Profile
import java.sql.Timestamp

data class ApplicationRequest(
    val eventId: Long,
    val profileId: Long?,
    val status: String,
    val submissionDate: Timestamp,
    val message: String?,
    val isObserver: Boolean,
    val fileApplications: List<FileApplicationRequest>,
    val verdict: String? = null
) {
    fun toEntity(event: Event, profile: Profile): Application {
        val application = Application(
            event = event,
            profile = profile,
            status = status,
            submissionDate = submissionDate,
            message = message,
            verdict = verdict,
            isObserver = isObserver
        )
        application.fileApplications = fileApplications.map { it.toEntity(application) }.toMutableList()
        return application
    }
}