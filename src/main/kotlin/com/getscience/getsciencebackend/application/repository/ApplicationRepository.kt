package com.getscience.getsciencebackend.application.repository

import com.getscience.getsciencebackend.application.data.model.Application
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ApplicationRepository : JpaRepository<Application, Long> {
    fun findByEventEventId(eventId: Long): List<Application>
    fun findByProfileProfileId(profileId: Long): List<Application>
    fun findByEventOrganizerAccountEmail(email: String): List<Application>
    fun findByProfileAccountEmail(email: String): List<Application>
    fun findByFileApplicationsFileId(fileId: Long): Application?
}