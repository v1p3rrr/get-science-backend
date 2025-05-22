package com.getscience.getsciencebackend.application.controller

import com.getscience.getsciencebackend.application.data.dto.*
import com.getscience.getsciencebackend.application.service.ApplicationService
import com.getscience.getsciencebackend.user.service.AccountService
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

    /**
     * Создает новую заявку на участие в мероприятии.
     * @param applicationRequest данные заявки
     * @param fileApplicationFileMetadataDTO метаданные загружаемых файлов
     * @param files файлы, прикрепляемые к заявке
     * @return информация о созданной заявке
     */
    @PostMapping("/submit",
        consumes = ["multipart/form-data", "application/octet-stream"])
     fun submitApplication(
        @RequestPart applicationRequest: ApplicationRequest,
        @RequestPart fileApplicationFileMetadataDTO: List<ApplicationFileMetadataDTO>?,
        @RequestPart ("files") files: List<MultipartFile>?
    ): ResponseEntity<ApplicationResponse> {
        val email = getEmailFromToken()
        val application = applicationService.createApplication(applicationRequest, fileApplicationFileMetadataDTO?: emptyList(), files?: emptyList(), email)
        return ResponseEntity.ok(applicationService.getApplicationById(application.applicationId, email))
    }

    /**
     * Обновляет существующую заявку.
     * @param applicationId идентификатор заявки
     * @param applicationRequest обновленные данные заявки
     * @param fileApplicationFileMetadataDTO метаданные загружаемых файлов
     * @param files файлы, прикрепляемые к заявке
     * @return информация об обновленной заявке
     */
    @PutMapping("/{applicationId}")
     fun updateApplication(
        @PathVariable applicationId: Long,
        @RequestPart applicationRequest: ApplicationRequest,
        @RequestPart fileApplicationFileMetadataDTO: List<ApplicationFileMetadataDTO>?,
        @RequestPart ("files") files: List<MultipartFile>?
    ): ResponseEntity<ApplicationResponse> {
        val email = getEmailFromToken()
        val updatedApplication = applicationService.updateApplication(applicationId, applicationRequest, fileApplicationFileMetadataDTO?: emptyList(), files?: emptyList(), email)
        return ResponseEntity.ok(applicationService.getApplicationById(updatedApplication.applicationId, email))
    }

    /**
     * Обновляет заявку организатором мероприятия.
     * @param applicationId идентификатор заявки
     * @param applicationRequest обновленные данные заявки
     * @return информация об обновленной заявке
     */
    @PutMapping("/{applicationId}/update-organizer")
    fun updateApplicationByOrganizer(@PathVariable applicationId: Long,
                          @RequestBody applicationRequest: ApplicationRequest
    ): ResponseEntity<ApplicationResponse> {
        val email = getEmailFromToken()
        val updatedApplication = applicationService.updateApplicationByOrganizer(applicationId, applicationRequest, email)
        return ResponseEntity.ok(applicationService.getApplicationById(updatedApplication.applicationId, email))
    }

    /**
     * Получает информацию о заявке по идентификатору.
     * @param applicationId идентификатор заявки
     * @return информация о заявке
     */
    @GetMapping("/{applicationId}")
    fun getApplicationById(@PathVariable applicationId: Long): ResponseEntity<ApplicationResponse> {
        val email = getEmailFromToken()
        val application = applicationService.getApplicationById(applicationId, email)
        return ResponseEntity.ok(application)
    }

    /**
     * Получает детальную информацию о заявке с данными заявителя.
     * @param applicationId идентификатор заявки
     * @return детальная информация о заявке с данными заявителя
     */
    @GetMapping("/{applicationId}/with-applicant")
    fun getApplicationWithApplicant(@PathVariable applicationId: Long): ResponseEntity<ApplicationDetailWithApplicantResponse> {
        val email = getEmailFromToken()
        val application = applicationService.getApplicationDetailWithApplicant(applicationId, email)
        return ResponseEntity.ok(application)
    }

    /**
     * Получает детальную информацию о заявке с данными организатора.
     * @param applicationId идентификатор заявки
     * @return детальная информация о заявке с данными организатора
     */
    @GetMapping("/{applicationId}/with-organizer")
    fun getApplicationWithOrganizer(@PathVariable applicationId: Long): ResponseEntity<ApplicationDetailWithOrganizerResponse> {
        val email = getEmailFromToken()
        val application = applicationService.getApplicationDetailWithOrganizer(applicationId, email)
        return ResponseEntity.ok(application)
    }

    /**
     * Получает список всех заявок для указанного мероприятия.
     * @param eventId идентификатор мероприятия
     * @return список заявок
     */
    @GetMapping("/event/{eventId}")
    fun getApplicationsByEvent(@PathVariable eventId: Long): ResponseEntity<List<ApplicationResponse>> {
        val applications = applicationService.getApplicationsByEvent(eventId)
        return ResponseEntity.ok(applications)
    }

    /**
     * Получает список всех заявок для указанного профиля.
     * @param profileId идентификатор профиля
     * @return список заявок
     */
    @GetMapping("/profile/{profileId}")
    fun getApplicationsByProfile(@PathVariable profileId: Long): ResponseEntity<List<ApplicationResponse>> {
        if (!checkSuccessfulAuth()) {
            return ResponseEntity.badRequest().build()
        }
        val applications = applicationService.getApplicationsByProfile(profileId)
        return ResponseEntity.ok(applications)
    }

    /**
     * Получает список всех заявок для текущего организатора.
     * @return список заявок
     */
    @GetMapping("/organizer")
    fun getApplicationsByOrganizer(): ResponseEntity<List<ApplicationResponse>> {
        val email = getEmailFromToken()
        val applications = applicationService.getApplicationsByOrganizer(email)
        return ResponseEntity.ok(applications)
    }

    /**
     * Получает список всех заявок для текущего заявителя.
     * @return список заявок
     */
    @GetMapping("/applicant")
    fun getApplicationsByApplicant(): ResponseEntity<List<ApplicationResponse>> {
        val email = getEmailFromToken()
        val applications = applicationService.getApplicationsByApplicant(email)
        return ResponseEntity.ok(applications)
    }

    /**
     * Удаляет заявку по идентификатору.
     * @param applicationId идентификатор заявки
     * @return результат операции удаления
     */
    @DeleteMapping("/{applicationId}")
    fun deleteApplication(@PathVariable applicationId: Long): ResponseEntity<Boolean> {
        val email = getEmailFromToken()
        return ResponseEntity.ok(applicationService.deleteApplication(applicationId, email))
    }

    /**
     * Извлекает email пользователя из JWT токена.
     * @return email пользователя
     */
    private fun getEmailFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        return email
    }

    /**
     * Извлекает роль пользователя из JWT токена.
     * @return роль пользователя
     */
    private fun getRoleFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val role = authentication.authorities.stream().findFirst().get().toString()
        return role
    }

    /**
     * Проверяет успешность аутентификации пользователя.
     * @return результат проверки
     */
    private fun checkSuccessfulAuth(): Boolean {
        return accountService.checkIfProfileExists(getEmailFromToken())
    }
}