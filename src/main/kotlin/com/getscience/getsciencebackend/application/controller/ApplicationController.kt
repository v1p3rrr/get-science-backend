package com.getscience.getsciencebackend.application.controller

import com.getscience.getsciencebackend.application.data.dto.ApplicationRequest
import com.getscience.getsciencebackend.application.data.dto.ApplicationResponse
import com.getscience.getsciencebackend.application.service.ApplicationService
import com.getscience.getsciencebackend.user.service.AccountService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("api/v1/applications")
class ApplicationController(
    private val applicationService: ApplicationService,
    private val accountService: AccountService
) {

    val log = KotlinLogging.logger {}

    @PostMapping("/submit",
        consumes = ["multipart/form-data", "application/octet-stream"])
     fun submitApplication(
        @RequestPart applicationRequest: ApplicationRequest,
        @RequestParam isEncryptionEnabled: Boolean?,
        @RequestPart("files") files: List<MultipartFile>?
    ): ResponseEntity<ApplicationResponse> {
        val email = getEmailFromToken()
        val application = applicationService.createApplication(applicationRequest, files?: emptyList(),isEncryptionEnabled ?: false, email)
        return ResponseEntity.ok(applicationService.getApplicationById(application.applicationId, email))
    }

    @PostMapping("/{applicationId}/update")
    @PutMapping("/{applicationId}")
     fun updateApplication(@PathVariable applicationId: Long,
                                  @RequestPart applicationRequest: ApplicationRequest,
                                  @RequestParam isEncryptionEnabled: Boolean?,
                                  @RequestPart ("files") files: List<MultipartFile>?
    ): ResponseEntity<ApplicationResponse> {
        val email = getEmailFromToken()
        val updatedApplication = applicationService.updateApplication(applicationId, files?: emptyList(), applicationRequest, isEncryptionEnabled?: false, email)
        return ResponseEntity.ok(applicationService.getApplicationById(updatedApplication.applicationId, email))
    }

    @PostMapping("/{applicationId}/update-organizer")
    @PutMapping("/{applicationId}")
    fun updateApplicationByOrganizer(@PathVariable applicationId: Long,
                          @RequestBody applicationRequest: ApplicationRequest
    ): ResponseEntity<ApplicationResponse> {
        val email = getEmailFromToken()
        val updatedApplication = applicationService.updateApplicationByOrganizer(applicationId, applicationRequest, email)
        return ResponseEntity.ok(applicationService.getApplicationById(updatedApplication.applicationId, email))
    }

    @GetMapping("/{applicationId}")
    fun getApplicationById(@PathVariable applicationId: Long): ResponseEntity<ApplicationResponse> {
        val email = getEmailFromToken()
        val application = applicationService.getApplicationById(applicationId, email)
        return ResponseEntity.ok(application)
    }

    @GetMapping("/event/{eventId}")
    fun getApplicationsByEvent(@PathVariable eventId: Long): ResponseEntity<List<ApplicationResponse>> {
        val applications = applicationService.getApplicationsByEvent(eventId)
        return ResponseEntity.ok(applications)
    }

    @GetMapping("/profile/{profileId}")
    fun getApplicationsByProfile(@PathVariable profileId: Long): ResponseEntity<List<ApplicationResponse>> {
        if (!checkSuccessfulAuth()) {
            return ResponseEntity.badRequest().build()
        }
        val applications = applicationService.getApplicationsByProfile(profileId)
        return ResponseEntity.ok(applications)
    }

    @GetMapping("/organizer")
    fun getApplicationsByOrganizer(): ResponseEntity<List<ApplicationResponse>> {
        val email = getEmailFromToken()
        val applications = applicationService.getApplicationsByOrganizer(email)
        return ResponseEntity.ok(applications)
    }

    @GetMapping("/applicant")
    fun getApplicationsByApplicant(): ResponseEntity<List<ApplicationResponse>> {
        val email = getEmailFromToken()
        val applications = applicationService.getApplicationsByApplicant(email)
        return ResponseEntity.ok(applications)
    }

    @DeleteMapping("/{applicationId}")
    fun deleteApplication(@PathVariable applicationId: Long): ResponseEntity<Boolean> {
        val email = getEmailFromToken()
        return ResponseEntity.ok(applicationService.deleteApplication(applicationId, email))
    }

    private fun getEmailFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        return email
    }

    private fun getRoleFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val role = authentication.authorities.stream().findFirst().get().toString()
        return role
    }

    private fun checkSuccessfulAuth(): Boolean {
        return accountService.checkIfProfileExists(getEmailFromToken())
    }
}