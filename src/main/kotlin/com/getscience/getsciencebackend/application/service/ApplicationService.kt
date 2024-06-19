package com.getscience.getsciencebackend.application.service

import com.getscience.getsciencebackend.application.data.dto.ApplicationRequest
import com.getscience.getsciencebackend.application.data.dto.ApplicationResponse
import org.springframework.web.multipart.MultipartFile

interface ApplicationService {
    fun createApplication(applicationRequest: ApplicationRequest, files: List<MultipartFile>, isEncryptionEnabled: Boolean, email: String): ApplicationResponse
    fun updateApplication(applicationId: Long, files: List<MultipartFile>, applicationRequest: ApplicationRequest, isEncryptionEnabled: Boolean, email: String): ApplicationResponse
    fun updateApplicationByOrganizer(applicationId: Long, applicationRequest: ApplicationRequest, email: String): ApplicationResponse
    fun getApplicationById(applicationId: Long, email: String): ApplicationResponse
    fun getApplicationsByEvent(eventId: Long): List<ApplicationResponse>
    fun getApplicationsByProfile(profileId: Long): List<ApplicationResponse>
    fun getApplicationsByOrganizer(email: String): List<ApplicationResponse>
    fun getApplicationsByApplicant(email: String): List<ApplicationResponse>
    fun deleteApplication(applicationId: Long, email: String): Boolean
}