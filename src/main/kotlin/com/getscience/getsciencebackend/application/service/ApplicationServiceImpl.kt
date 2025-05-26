package com.getscience.getsciencebackend.application.service

import com.getscience.getsciencebackend.application.data.dto.*
import com.getscience.getsciencebackend.application.repository.ApplicationRepository
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.file.service.FileApplicationService
import com.getscience.getsciencebackend.file.service.S3Service
import com.getscience.getsciencebackend.notification.service.NotificationEventService
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.Date
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation

@Service
@Transactional
class ApplicationServiceImpl(
    private val applicationRepository: ApplicationRepository,
    private val eventRepository: EventRepository,
    private val profileRepository: ProfileRepository,
    private val fileService: FileApplicationService,
    private val notificationEventService: NotificationEventService,
    private val fileApplicationService: FileApplicationService,
    private val s3Service: S3Service
) : ApplicationService {

    private val log: Logger = LoggerFactory.getLogger(ApplicationServiceImpl::class.java)

    /**
     * Создает новую заявку на участие в мероприятии.
     * Проверяет, что текущая дата находится в периоде приема заявок.
     * Загружает прикрепленные файлы и отправляет уведомление организатору.
     * 
     * @param applicationRequest данные заявки
     * @param fileApplicationFileMetadataDTO метаданные загружаемых файлов
     * @param files файлы, прикрепляемые к заявке
     * @param email email пользователя, создающего заявку
     * @return информация о созданной заявке
     * @throws RuntimeException если мероприятие или профиль не найдены
     * @throws IllegalStateException если период приема заявок не активен
     */
    @LogBusinessOperation(operationType = "APPLICATION_CREATE", description = "Создание новой заявки")
    @Transactional
    @CacheEvict(value = ["applications", "applicationsByProfile", "applicationsByEvent", "applicationsByOrganizer", "applicationDetail"], allEntries = true)
    @CachePut(value = ["applications"], key = "#result.applicationId", unless = "#result == null")
    override fun createApplication(
        applicationRequest: ApplicationRequest,
        fileApplicationFileMetadataDTO: List<ApplicationFileMetadataDTO>,
        files: List<MultipartFile>,
        email: String
    ): ApplicationResponse {
        val event = eventRepository.findById(applicationRequest.eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw RuntimeException("Profile not found")

        // Проверка, что текущая дата в периоде приема заявок
        val currentDate = Date()
        if (currentDate.before(event.applicationStart) || currentDate.after(event.applicationEnd)) {
            throw IllegalStateException("Application period is not active. Applications for this event are accepted from ${event.applicationStart} to ${event.applicationEnd}")
        }

        val application = applicationRequest.toEntity(event, profile)

        if (files.isNotEmpty()) {
            val fileApplications = fileService.uploadFiles(files, application, fileApplicationFileMetadataDTO)

            if (fileApplications.isNotEmpty()) {
                application.fileApplications = fileApplications.toMutableList()
            }
        }

        val savedApplication = applicationRepository.save(application)
        
        // Отправляем уведомление организатору
        notificationEventService.handleNewApplication(
            applicationId = savedApplication.applicationId,
            eventId = event.eventId,
            eventOwnerId = event.organizer.profileId
        )

        return ApplicationResponse.fromEntity(savedApplication)
    }

    /**
     * Обновляет существующую заявку.
     * Проверяет права доступа пользователя.
     * Обрабатывает прикрепленные файлы, удаляя старые версии документов при необходимости.
     * Отправляет уведомление заинтересованным сторонам.
     * 
     * @param applicationId идентификатор заявки
     * @param applicationRequest обновленные данные заявки
     * @param fileApplicationFileMetadataDTO метаданные загружаемых файлов
     * @param files файлы, прикрепляемые к заявке
     * @param email email пользователя, обновляющего заявку
     * @return информация об обновленной заявке
     * @throws RuntimeException если заявка не найдена
     * @throws IllegalArgumentException если пользователь не имеет прав на обновление
     */
    @LogBusinessOperation(operationType = "APPLICATION_UPDATE", description = "Обновление заявки пользователем")
    @Transactional
    @CacheEvict(value = ["applications", "applicationsByProfile", "applicationsByEvent", "applicationsByOrganizer", "applicationDetail"], allEntries = true)
    @CachePut(value = ["applications"], key = "#applicationId", unless = "#result == null")
    override fun updateApplication(applicationId: Long,
                          applicationRequest: ApplicationRequest,
                          fileApplicationFileMetadataDTO: List<ApplicationFileMetadataDTO>,
                          files: List<MultipartFile>,
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

        // Отслеживаем, какие docRequired были в прикрепленных файлах
        val existingDocRequiredIds = application.fileApplications
            .filter { it.docRequired != null }
            .map { it.docRequired!!.docRequiredId }
            .toSet()
        
        // Отслеживаем, какие docRequired будут в новых файлах
        val newDocRequiredIds = fileApplicationFileMetadataDTO.map { it.docRequiredId }.toSet()
        
        // Сохраняем текущие файлы
        val existingFiles = application.fileApplications.toList()
        
        // Загружаем новые файлы
        val newFiles = if (files.isNotEmpty()) {
            fileService.uploadFiles(files, application, fileApplicationFileMetadataDTO)
        } else {
            emptyList()
        }
        
        // Для каждого docRequiredId, который есть в новых файлах и был в старых,
        // удаляем старые файлы с этим docRequiredId
        for (docRequiredId in existingDocRequiredIds.intersect(newDocRequiredIds)) {
            // Находим все файлы с этим docRequiredId
            val filesToRemove = existingFiles.filter { 
                it.docRequired != null && it.docRequired.docRequiredId == docRequiredId 
            }
            
            // Удаляем файлы из S3 и БД
            filesToRemove.forEach { file ->
                s3Service.deleteFile(file.filePath)
                fileApplicationService.deleteFileApplication(file.fileId, email)
            }
        }
        
        // Создаем новый список файлов, исключая те, которые мы только что удалили
        val remainingFiles = existingFiles.filter { file ->
            file.docRequired == null || !newDocRequiredIds.contains(file.docRequired.docRequiredId)
        }

        val updatedApplication = application.copy(
            status = applicationRequest.status,
            submissionDate = applicationRequest.submissionDate,
            message = applicationRequest.message,
            isObserver = applicationRequest.isObserver,
            event = eventRepository.findById(applicationRequest.eventId).orElseThrow {
                RuntimeException("Event not found")
            },
            fileApplications = (remainingFiles + newFiles).toMutableList()
        )

        val saved = applicationRepository.save(updatedApplication)

        if (user.profileId == application.profile.profileId) {
            notificationEventService.notifyEventStaffAboutApplicationUpdate(saved, user)
        } else {
            notificationEventService.notifyApplicantAboutApplicationUpdate(saved)
        }

        return ApplicationResponse.fromEntity(saved)
    }

    /**
     * Обновляет заявку организатором мероприятия.
     * Проверяет, что пользователь является организатором данного мероприятия.
     * Отправляет уведомление заявителю об обновлении статуса.
     * 
     * @param applicationId идентификатор заявки
     * @param applicationRequest обновленные данные заявки
     * @param email email организатора
     * @return информация об обновленной заявке
     * @throws RuntimeException если заявка не найдена
     * @throws IllegalArgumentException если пользователь не является организатором
     */
    @LogBusinessOperation(operationType = "APPLICATION_UPDATE_BY_ORGANIZER", description = "Обновление заявки организатором")
    @Transactional
    @CacheEvict(value = ["applications", "applicationsByProfile", "applicationsByEvent", "applicationsByOrganizer", "applicationDetail"], allEntries = true)
    @CachePut(value = ["applications"], key = "#applicationId", unless = "#result == null")
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

        val saved = applicationRepository.save(updatedApplication)

        notificationEventService.notifyApplicantAboutApplicationUpdate(saved)

        notificationEventService.handleApplicationStatusChanged(
            applicationId = saved.applicationId,
            eventId = saved.event.eventId,
            userId = saved.profile.profileId,
            newStatus = saved.status
        )

        return ApplicationResponse.fromEntity(saved)
    }

    /**
     * Получает информацию о заявке по идентификатору.
     * Проверяет права доступа пользователя.
     * 
     * @param applicationId идентификатор заявки
     * @param email email запрашивающего пользователя
     * @return информация о заявке
     * @throws RuntimeException если заявка не найдена
     * @throws IllegalArgumentException если пользователь не имеет прав доступа
     */
    @LogBusinessOperation(operationType = "APPLICATION_GET_BY_ID", description = "Получение заявки по ID")
    @Transactional
    @Cacheable(value = ["applications"], key = "#applicationId")
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

    /**
     * Получает детальную информацию о заявке с данными заявителя.
     * Проверяет права доступа пользователя.
     * Генерирует presigned URL для аватара пользователя.
     * 
     * @param applicationId идентификатор заявки
     * @param email email запрашивающего пользователя
     * @return детальная информация о заявке с данными заявителя
     * @throws RuntimeException если заявка не найдена
     * @throws IllegalArgumentException если пользователь не имеет прав доступа
     */
    @LogBusinessOperation(operationType = "APPLICATION_GET_DETAIL_APPLICANT", description = "Получение деталей заявки с заявителем")
    @Cacheable(value = ["applicationDetail"], key = "'applicant_' + #applicationId")
    override fun getApplicationDetailWithApplicant(applicationId: Long, email: String): ApplicationDetailWithApplicantResponse {
        val user = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("User not found")
        val application = applicationRepository.findById(applicationId).orElseThrow {
            RuntimeException("Application not found")
        }
        if (application.profile.profileId != user.profileId && application.event.organizer.profileId != user.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }
        
        return ApplicationDetailWithApplicantResponse(
            application = ApplicationResponse.fromEntity(application),
            event = EventDTO.fromEntity(application.event),
            applicant = createProfileDTOWithPresignedUrl(application.profile)
        )
    }

    /**
     * Получает детальную информацию о заявке с данными организатора.
     * Проверяет права доступа пользователя.
     * Генерирует presigned URL для аватара организатора.
     * 
     * @param applicationId идентификатор заявки
     * @param email email запрашивающего пользователя
     * @return детальная информация о заявке с данными организатора
     * @throws RuntimeException если заявка не найдена
     * @throws IllegalArgumentException если пользователь не имеет прав доступа
     */
    @LogBusinessOperation(operationType = "APPLICATION_GET_DETAIL_ORGANIZER", description = "Получение деталей заявки с организатором")
    @Cacheable(value = ["applicationDetail"], key = "'organizer_' + #applicationId")
    override fun getApplicationDetailWithOrganizer(applicationId: Long, email: String): ApplicationDetailWithOrganizerResponse {
        val user = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("User not found")
        val application = applicationRepository.findById(applicationId).orElseThrow {
            RuntimeException("Application not found")
        }
        if (application.profile.profileId != user.profileId && application.event.organizer.profileId != user.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }
        
        return ApplicationDetailWithOrganizerResponse(
            application = ApplicationResponse.fromEntity(application),
            event = EventDTO.fromEntity(application.event),
            organizer = createProfileDTOWithPresignedUrl(application.event.organizer)
        )
    }
    
    /**
     * Создает DTO профиля с presigned URL для аватара.
     * 
     * @param profile профиль пользователя
     * @return DTO профиля с presigned URL для аватара
     */
    private fun createProfileDTOWithPresignedUrl(profile: Profile): ProfileDTO {
        // Генерируем presigned URL для аватара, если он есть
        val avatarUrl = if (profile.avatarUrl != null) {
            s3Service.generatePresignedUrl(profile.avatarUrl).toString()
        } else {
            null
        }
        
        return ProfileDTO(
            profileId = profile.profileId,
            firstName = profile.firstName,
            lastName = profile.lastName,
            email = profile.account.email,
            avatarUrl = avatarUrl,
            aboutMe = profile.aboutMe
        )
    }

    /**
     * Получает список всех заявок для указанного мероприятия.
     * 
     * @param eventId идентификатор мероприятия
     * @return список заявок
     */
    @LogBusinessOperation(operationType = "APPLICATION_GET_BY_EVENT", description = "Получение заявок по ID мероприятия")
    @Cacheable(value = ["applicationsByEvent"], key = "#eventId")
    override fun getApplicationsByEvent(eventId: Long): List<ApplicationResponse> {
        val applications = applicationRepository.findByEventEventId(eventId)
        return applications.map { ApplicationResponse.fromEntity(it) }
    }

    /**
     * Получает список всех заявок для указанного профиля.
     * 
     * @param profileId идентификатор профиля
     * @return список заявок
     */
    @LogBusinessOperation(operationType = "APPLICATION_GET_BY_PROFILE", description = "Получение заявок по ID профиля")
    @Cacheable(value = ["applicationsByProfile"], key = "#profileId")
    override fun getApplicationsByProfile(profileId: Long): List<ApplicationResponse> {
        val applications = applicationRepository.findByProfileProfileId(profileId)
        return applications.map { ApplicationResponse.fromEntity(it) }
    }

    /**
     * Получает список всех заявок для мероприятий, организованных пользователем с указанным email.
     * 
     * @param email email организатора
     * @return список заявок
     */
    @LogBusinessOperation(operationType = "APPLICATION_GET_BY_ORGANIZER_EMAIL", description = "Получение заявок по email организатора")
    @Cacheable(value = ["applicationsByEvent"], key = "#email")
    override fun getApplicationsByOrganizer(email: String): List<ApplicationResponse> {
        val applications = applicationRepository.findByEventOrganizerAccountEmail(email)
        return applications.map { ApplicationResponse.fromEntity(it) }
    }

    /**
     * Получает список всех заявок, поданных пользователем с указанным email.
     * 
     * @param email email заявителя
     * @return список заявок
     */
    @LogBusinessOperation(operationType = "APPLICATION_GET_BY_APPLICANT_EMAIL", description = "Получение заявок по email заявителя")
    @Cacheable(value = ["applicationsByProfile"], key = "#email")
    override fun getApplicationsByApplicant(email: String): List<ApplicationResponse> {
        val applications = applicationRepository.findByProfileAccountEmail(email)
        return applications.map { ApplicationResponse.fromEntity(it) }
    }

    /**
     * Удаляет заявку по идентификатору.
     * Проверяет, что пользователь является автором заявки.
     * Удаляет связанные файлы из хранилища S3.
     * 
     * @param applicationId идентификатор заявки
     * @param email email пользователя, удаляющего заявку
     * @return результат операции удаления
     * @throws RuntimeException если заявка не найдена
     * @throws IllegalArgumentException если пользователь не является автором заявки
     */
    @LogBusinessOperation(operationType = "APPLICATION_DELETE", description = "Удаление заявки")
    @Transactional
    @CacheEvict(value = ["applications", "applicationsByProfile", "applicationsByEvent", "applicationsByOrganizer", "applicationDetail"], allEntries = true, condition = "#result != null && #result == true")
    override fun deleteApplication(applicationId: Long, email: String): Boolean {
        val applicant = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Applicant not found")
        val application = applicationRepository.findById(applicationId).orElseThrow {
            RuntimeException("Application not found")
        }
        if (application.profile.profileId != applicant.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }
        
        // Удаляем файлы заявки из S3
        var deletedFileCount = 0
        var failedFileDeleteCount = 0
        
        for (fileApplication in application.fileApplications) {
            try {
                val s3DeleteSuccess = s3Service.deleteFile(fileApplication.filePath)
                if (s3DeleteSuccess) {
                    deletedFileCount++
                } else {
                    log.warn("Failed to delete application file from S3: ${fileApplication.filePath}")
                    failedFileDeleteCount++
                }
            } catch (e: Exception) {
                log.error("Error deleting application file from S3: ${fileApplication.filePath}", e)
                failedFileDeleteCount++
            }
        }
        
        log.info("Deleted $deletedFileCount files related to application $applicationId. Failed to delete files: $failedFileDeleteCount")
        
        try {
        applicationRepository.deleteById(applicationId)
            log.info("Successfully deleted application with ID: $applicationId")
        return true
        } catch (e: Exception) {
            log.error("Error deleting application with ID: $applicationId", e)
            return false
        }
    }
}