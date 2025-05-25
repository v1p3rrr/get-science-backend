package com.getscience.getsciencebackend.event.service

import com.getscience.getsciencebackend.application.repository.ApplicationRepository
import com.getscience.getsciencebackend.chat.service.ChatService
import com.getscience.getsciencebackend.event.data.dto.*
import com.getscience.getsciencebackend.event.data.model.EventFormat
import com.getscience.getsciencebackend.event.data.model.EventModerationStatus
import com.getscience.getsciencebackend.event.data.model.EventStatus
import com.getscience.getsciencebackend.event.data.model.EventType
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.data.model.FileEvent
import com.getscience.getsciencebackend.file.repository.FileEventRepository
import com.getscience.getsciencebackend.file.service.FileEventService
import com.getscience.getsciencebackend.file.service.S3Service
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import com.getscience.getsciencebackend.notification.service.NotificationEventService
import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.repository.ProfileRepository
import jakarta.persistence.EntityNotFoundException
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import java.time.LocalDateTime

@Service
@Transactional
class EventServiceImpl(
    private val eventRepository: EventRepository,
    private val profileRepository: ProfileRepository,
    private val fileEventRepository: FileEventRepository,
    private val applicationRepository: ApplicationRepository,
    private val notificationEventService: NotificationEventService,
    private val chatService: ChatService,
    private val s3Service: S3Service,
    private val exportGeneratorService: ExportGeneratorService,
    private val fileEventService: FileEventService
) : EventService {

    private val logger = KotlinLogging.logger {}

    @LogBusinessOperation(operationType = "EVENT_CREATE", description = "Создание нового мероприятия")
    @CachePut(value = ["events"], unless = "#result == null && #result == false")
    override fun createEvent(eventRequest: EventRequest, email: String): Long {
        val organizer = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Organizer not found")
        val reviewerProfiles = eventRequest.reviewers.mapNotNull { it.email.let(profileRepository::findByAccountEmail) }
        val coownerProfiles = eventRequest.coowners.mapNotNull { it.email.let(profileRepository::findByAccountEmail) }.toSet()
        val event = eventRequest.toEntity(organizer, reviewerProfiles, coownerProfiles)
        val savedEvent = eventRepository.save(event)
        return savedEvent.eventId
    }

    @LogBusinessOperation(operationType = "EVENT_CREATE_WITH_FILES", description = "Создание нового мероприятия с файлами")
    @CachePut(value = ["events"], unless = "#result == null && #result == false")
    override fun createEventWithFiles(
        eventRequest: EventRequest,
        fileEventRequestList: List<FileEventRequest>,
        files: List<MultipartFile>,
        email: String
    ): Long {
        val organizer = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Organizer not found")
        val reviewerProfiles = eventRequest.reviewers.mapNotNull { it.email.let(profileRepository::findByAccountEmail) }
        val coownerProfiles = eventRequest.coowners.mapNotNull { it.email.let(profileRepository::findByAccountEmail) }.toSet()
        val event = eventRequest.toEntity(organizer, reviewerProfiles, coownerProfiles)
        
        // Сохраняем событие, чтобы получить ID
        val savedEvent = eventRepository.save(event)
        
        // Если есть файлы для загрузки, выполняем их загрузку
        if (files.isNotEmpty()) {
            if (files.size != fileEventRequestList.size) {
                throw IllegalArgumentException("Files count must match metadata count")
            }
            val uploadedFiles = fileEventService.uploadFiles(files, savedEvent, fileEventRequestList)
            
            // Добавляем новые файлы в список
            savedEvent.fileEvents.addAll(uploadedFiles)
            eventRepository.save(savedEvent)
        }
        
        return savedEvent.eventId
    }

    @LogBusinessOperation(operationType = "EVENT_UPDATE", description = "Обновление мероприятия")
    @CacheEvict(value = ["events"], key="#eventId")
    override fun updateEvent(eventId: Long, eventRequest: EventRequest, email: String): Long {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("User not found")
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        
        val isOrganizer = event.organizer.profileId == profile.profileId
        val isCoowner = event.coowners.any { it.profileId == profile.profileId }
        
        if (!isOrganizer && !isCoowner) {
            throw IllegalArgumentException("Unauthorized")
        }

        event.apply {
            title = eventRequest.title
            description = eventRequest.description
            organizerDescription = eventRequest.organizerDescription
            dateStart = eventRequest.dateStart
            dateEnd = eventRequest.dateEnd
            applicationStart = eventRequest.applicationStart
            applicationEnd = eventRequest.applicationEnd
            location = eventRequest.location
            type = eventRequest.type
            theme = eventRequest.theme
            format = eventRequest.format
            results = eventRequest.results
            status = eventRequest.status
            moderationStatus = eventRequest.moderationStatus
            observersAllowed = eventRequest.observersAllowed
            documentsRequired.clear()
            documentsRequired.addAll(eventRequest.documentsRequired.map { it.toEntity(this) })

            val existingFileIds = fileEvents.map { it.fileId }
            val existingFileTimestamps = fileEvents.associate { it.fileId to it.uploadDate }
            
            fileEvents.clear()
            fileEvents.addAll(eventRequest.fileEvents.map { fileEventRequest ->
                if (fileEventRequest.fileId in existingFileIds) {
                    val originalUploadDate = existingFileTimestamps[fileEventRequest.fileId]
                    val fileEvent = fileEventRequest.toEntity(this)
                    
                    if (originalUploadDate != null) {
                        FileEvent(
                            fileId = fileEvent.fileId,
                            event = fileEvent.event,
                            fileName = fileEvent.fileName,
                            filePath = fileEvent.filePath,
                            uploadDate = originalUploadDate,
                            fileType = fileEvent.fileType,
                            fileKindName = fileEvent.fileKindName,
                            category = fileEvent.category,
                            description = fileEvent.description
                        )
                    } else {
                        fileEvent
                    }
                } else {
                    fileEventRequest.toEntity(this)
                }
            })
            
            reviewers.clear()
            val reviewerProfiles = eventRequest.reviewers.mapNotNull { it.email.let(profileRepository::findByAccountEmail) }
            reviewers.addAll(reviewerProfiles)

            if (isOrganizer) {
                coowners.clear()
                val coownerProfiles = eventRequest.coowners.mapNotNull { it.email.let(profileRepository::findByAccountEmail) }
                coowners.addAll(coownerProfiles)
            }
        }

        eventRepository.save(event)

        notificationEventService.notifyEventStaffAboutEventChange(event, profile, isDelete = false)
        notificationEventService.notifyApplicantsAboutEventChange(event, profile, isDelete = false)

        chatService.updateChatParticipantsByEventId(event.eventId)

        return event.eventId
    }

    @LogBusinessOperation(operationType = "EVENT_UPDATE_WITH_FILES", description = "Обновление мероприятия с файлами")
    @CacheEvict(value = ["events"], key="#eventId")
    override fun updateEventWithFiles(
        eventId: Long,
        eventRequest: EventRequest,
        fileEventRequestList: List<FileEventRequest>,
        files: List<MultipartFile>,
        email: String
    ): Long {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("User not found")
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        
        val isOrganizer = event.organizer.profileId == profile.profileId
        val isCoowner = event.coowners.any { it.profileId == profile.profileId }
        
        if (!isOrganizer && !isCoowner) {
            throw IllegalArgumentException("Unauthorized")
        }

        event.apply {
            title = eventRequest.title
            description = eventRequest.description
            organizerDescription = eventRequest.organizerDescription
            dateStart = eventRequest.dateStart
            dateEnd = eventRequest.dateEnd
            applicationStart = eventRequest.applicationStart
            applicationEnd = eventRequest.applicationEnd
            location = eventRequest.location
            type = eventRequest.type
            theme = eventRequest.theme
            format = eventRequest.format
            results = eventRequest.results
            status = eventRequest.status
            moderationStatus = eventRequest.moderationStatus
            observersAllowed = eventRequest.observersAllowed
            documentsRequired.clear()
            documentsRequired.addAll(eventRequest.documentsRequired.map { it.toEntity(this) })
            
            val existingFileEvents = fileEvents.toList()
            val existingFileIds = existingFileEvents.map { it.fileId }.toSet()
            val sentFileIds = eventRequest.fileEvents.mapNotNull { it.fileId }.toSet()
            
            // Удаляем файлы, которых нет в новом списке
            val filesToRemove = existingFileEvents.filter { it.fileId !in sentFileIds }
            filesToRemove.forEach { fileEvent ->
                fileEventService.deleteFileEvent(fileEvent.fileId, email)
            }
            
            // Очищаем список файлов
            fileEvents.clear()
            
            // Обновляем существующие файлы
            for (fileEventRequest in eventRequest.fileEvents) {
                if (fileEventRequest.fileId != null && fileEventRequest.fileId > 0) {
                    val existingFile = existingFileEvents.find { it.fileId == fileEventRequest.fileId }
                    if (existingFile != null) {
                        // Обновляем существующий файл
                        existingFile.apply {
                            category = fileEventRequest.category
                            description = fileEventRequest.description
                            if (fileName != fileEventRequest.fileName) {
                                val newFilePath = s3Service.renameFile(filePath, fileEventRequest.fileName)
                                if (newFilePath != null) {
                                    filePath = newFilePath
                                }
                                fileName = fileEventRequest.fileName
                            }
                        }
                        fileEvents.add(existingFile)
                    }
                }
            }
            
            // Добавляем новые файлы
            if (files.isNotEmpty()) {
                if (files.size != fileEventRequestList.size) {
                    throw IllegalArgumentException("Files count must match metadata count")
                }
                val newFiles = fileEventService.uploadFiles(files, this, fileEventRequestList)
                fileEvents.addAll(newFiles)
            }
            
            reviewers.clear()
            val reviewerProfiles = eventRequest.reviewers.mapNotNull { it.email.let(profileRepository::findByAccountEmail) }
            reviewers.addAll(reviewerProfiles)

            if (isOrganizer) {
                coowners.clear()
                val coownerProfiles = eventRequest.coowners.mapNotNull { it.email.let(profileRepository::findByAccountEmail) }
                coowners.addAll(coownerProfiles)
            }
        }

        val savedEvent = eventRepository.save(event)

        notificationEventService.notifyEventStaffAboutEventChange(savedEvent, profile, isDelete = false)
        notificationEventService.notifyApplicantsAboutEventChange(savedEvent, profile, isDelete = false)

        chatService.updateChatParticipantsByEventId(savedEvent.eventId)

        return savedEvent.eventId
    }

    @LogBusinessOperation(operationType = "EVENT_GET_BY_ID", description = "Получение мероприятия по ID")
    @Cacheable(value = ["events"], key = "#eventId")
    override fun getEventById(eventId: Long): EventResponse {
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }

        return EventResponse.fromEntity(event, s3Service)
    }

    @LogBusinessOperation(operationType = "EVENT_SEARCH_PAGED", description = "Поиск мероприятий с пагинацией")
    @Cacheable(value = ["events"], key = "#root.methodName + ':' + #type + ':' + #theme + ':' + #location + ':' + #format + ':' + #title + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    override fun searchEventsPaged(type: String?, theme: String?, location: String?, format: String?, title: String?, page: Int, size: Int): Page<EventResponse> {
        val pageable = PageRequest.of(page, size)
        val normalizedTitle = title?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { "%$it%" }
        val eventsPage = eventRepository.searchEventsPaged(type, theme, location, format, normalizedTitle, EventModerationStatus.APPROVED, EventStatus.PUBLISHED, pageable)
        return eventsPage.map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_RECOMMENDATIONS", description = "Получение рекомендаций мероприятий")
    override fun getRecommendations(eventId: Long, limit: Int): List<EventResponse> {
        val originalEvent = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        val theme = originalEvent.theme ?: return emptyList()
        val events = eventRepository.findEventsByThemeAndModerationStatusAndStatus(theme, EventModerationStatus.APPROVED, EventStatus.PUBLISHED).filter { it.eventId!=eventId } .shuffled().take(limit)
        return events.map { event -> EventResponse.fromEntity(event, s3Service) }.shuffled()
    }

    @LogBusinessOperation(operationType = "EVENT_ADD_MATERIAL_METADATA", description = "Добавление метаданных материала к мероприятию")
    @CacheEvict(value = ["fileEvents"], key = "#eventId", condition = "#result != null")
    override fun addFileToEvent(eventId: Long, fileEventRequest: FileEventRequest, email: String): FileEvent {
        val organizer = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Organizer not found")
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        if (event.organizer.profileId != organizer.profileId) {
            throw IllegalArgumentException("Unauthorized")
        }

        val fileEvent = fileEventRequest.toEntity(event)
        return fileEventRepository.save(fileEvent)
    }

    @Cacheable(value = ["fileEventsByEvent"], key = "#eventId")
    override fun getFilesByEvent(eventId: Long): List<FileEventResponse> {
        val fileEvents = fileEventRepository.findByEventEventId(eventId)
        return fileEvents.map { fileEvent -> FileEventResponse.fromEntity(fileEvent) }
    }

    @LogBusinessOperation(operationType = "EVENT_DELETE", description = "Удаление мероприятия")
    @CacheEvict(value = ["events"], key = "#eventId", condition = "#result != null")
    override fun deleteEvent(eventId: Long, email: String): Boolean {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("User not found")
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        
        if (event.organizer.profileId != profile.profileId) {
            throw IllegalArgumentException("Unauthorized - only the organizer can delete an event")
        }

        notificationEventService.notifyEventStaffAboutEventChange(event, profile, isDelete = true)
        notificationEventService.notifyApplicantsAboutEventChange(event, profile, isDelete = true)

        val chatDeleted = chatService.deleteChatByEventId(eventId)
        if (chatDeleted) {
            logger.info("Чаты для события с ID: $eventId успешно удалены")
        } else {
            logger.warn("Для события с ID: $eventId не найдены чаты или произошла ошибка при удалении")
        }

        // Удаляем файлы всех заявок на мероприятие
        var deletedApplicationCount = 0
        var failedApplicationFileDeleteCount = 0
        
        applicationRepository.findByEventEventId(eventId).forEach { application ->
            try {
                for (fileApplication in application.fileApplications) {
                    try {
                        val s3DeleteSuccess = s3Service.deleteFile(fileApplication.filePath)
                        if (!s3DeleteSuccess) {
                            logger.warn("Failed to delete application file from S3: ${fileApplication.filePath}")
                            failedApplicationFileDeleteCount++
                        }
                    } catch (e: Exception) {
                        logger.error("Error deleting application file from S3: ${fileApplication.filePath}", e)
                        failedApplicationFileDeleteCount++
                    }
                }
                
                applicationRepository.deleteById(application.applicationId)
                deletedApplicationCount++
            } catch (e: Exception) {
                logger.error("Error deleting application with ID: ${application.applicationId}", e)
            }
        }
        
        logger.info("Deleted $deletedApplicationCount applications related to event $eventId. Failed to delete files: $failedApplicationFileDeleteCount")

        var deletedEventFileCount = 0
        var failedEventFileDeleteCount = 0
        
        for (fileEvent in event.fileEvents) {
            try {
                val s3DeleteSuccess = s3Service.deleteFile(fileEvent.filePath)
                if (s3DeleteSuccess) {
                    deletedEventFileCount++
                } else {
                    logger.warn("Failed to delete event file from S3: ${fileEvent.filePath}")
                    failedEventFileDeleteCount++
                }
            } catch (e: Exception) {
                logger.error("Error deleting event file from S3: ${fileEvent.filePath}", e)
                failedEventFileDeleteCount++
            }
        }
        
        logger.info("Deleted $deletedEventFileCount files related to event $eventId. Failed to delete files: $failedEventFileDeleteCount")

        try {
        eventRepository.deleteById(eventId)
            logger.info("Successfully deleted event with ID: $eventId")
        return true
        } catch (e: Exception) {
            logger.error("Error deleting event with ID: $eventId", e)
            return false
        }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_ALL", description = "Получение всех мероприятий")
    @Cacheable(value = ["events"])
    override fun getAllEvents(): List<EventResponse> {
        val events = eventRepository.findAll()
        return events.map { event -> EventResponse.fromEntity(event, s3Service) }
    }

    @Cacheable(value = ["fileEvents"], key = "#fileId")
    override fun getFileEventById(fileId: Long): FileEventResponse {
        val fileEvent = fileEventRepository.findById(fileId).orElseThrow {
            RuntimeException("FileEvent not found")
        }
        return FileEventResponse.fromEntity(fileEvent)
    }

    @LogBusinessOperation(operationType = "EVENT_GET_ALL_PAGED", description = "Получение всех мероприятий с пагинацией")
    @Cacheable(value = ["events"])
    override fun getAllEventsPaged(page: Int, size: Int): Page<EventResponse> {
        val pageable = PageRequest.of(page, size)
        val eventsPage = eventRepository.findAll(pageable)
        return eventsPage.map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_BY_JWT_PAGED", description = "Получение мероприятий пользователя (JWT) с пагинацией")
    @Cacheable(value = ["events"], key = "#root.methodName + ':' + #email")
    override fun getEventsByJwtPaged(email: String, page: Int, size: Int): Page<EventResponse> {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("Profile not found")
        val pageable = PageRequest.of(page, size)
        val eventsPage = eventRepository.findByOrganizerProfileId(profile.profileId, pageable)
        return eventsPage.map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_BY_ORGANIZER_PAGED", description = "Получение мероприятий организатора с пагинацией")
    @Cacheable(value = ["eventsByOrganizer"], key = "#organizerId")
    override fun getEventsByOrganizerPaged(organizerId: Long, page: Int, size: Int): Page<EventResponse> {
        val pageable = PageRequest.of(page, size)
        val events = eventRepository.findByOrganizerProfileId(organizerId, pageable)
        return events.map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_WITH_STAFF", description = "Получение мероприятия с рецензентами и со-организаторами")
    override fun getEventWithReviewersAndCoownersById(eventId: Long): EventResponse {
        val event = eventRepository.findWithReviewersAndCoownersByEventId(eventId)
            ?: throw RuntimeException("Event not found")
        return EventResponse.fromEntity(event, s3Service)
    }

    @LogBusinessOperation(operationType = "EVENT_GET_MY_PAGED", description = "Получение моих мероприятий (организатор) с пагинацией")
    override fun getMyEvents(email: String, page: Int, size: Int): Page<EventResponse> {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw RuntimeException("Profile not found")
        val pageable = PageRequest.of(page, size)
        return eventRepository.findByOrganizer(profile, pageable).map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_COOWNER_PAGED", description = "Получение мероприятий (со-организатор) с пагинацией")
    override fun getCoownerEvents(email: String, page: Int, size: Int): Page<EventResponse> {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw RuntimeException("Profile not found")
        val pageable = PageRequest.of(page, size)
        return eventRepository.findByCoownersContaining(profile, pageable).map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_REVIEWER_PAGED", description = "Получение мероприятий (рецензент) с пагинацией")
    override fun getReviewerEvents(email: String, page: Int, size: Int): Page<EventResponse> {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw RuntimeException("Profile not found")
        val pageable = PageRequest.of(page, size)
        return eventRepository.findByReviewersContaining(profile, pageable).map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_MODERATION_APPROVED", description = "Получение одобренных мероприятий")
    override fun getModerationApprovedEvents(page: Int, size: Int): Page<EventResponse> {
        val pageable = PageRequest.of(page, size)
        val eventsPage = eventRepository.findByModerationStatusAndStatus(EventModerationStatus.APPROVED, EventStatus.PUBLISHED, pageable)
        return eventsPage.map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_GET_MODERATION_NON_APPROVED", description = "Получение мероприятий не прошедших модерацию")
    override fun getModerationNonApprovedEvents(page: Int, size: Int): Page<EventResponse> {
        val pageable = PageRequest.of(page, size)
        val eventsPage = eventRepository.findByModerationStatusIsNotAndStatus(EventModerationStatus.APPROVED, EventStatus.PUBLISHED, pageable)
        return eventsPage.map { EventResponse.fromEntity(it, s3Service) }
    }

    @LogBusinessOperation(operationType = "EVENT_UPDATE_MODERATION_STATUS", description = "Обновление статуса модерации мероприятия")
    @CachePut(value = ["events"], key = "#eventId", unless = "#result == null && #result == false")
    override fun updateEventModerationStatus(eventId: Long, moderationStatus: EventModerationStatus, email: String): Boolean {
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("User not found")
        if (profile.role.title != RoleType.MODERATOR) { throw IllegalArgumentException("User is not a moderator") }
        val event = eventRepository.findById(eventId).orElseThrow {
            RuntimeException("Event not found")
        }
        
        event.moderationStatus = moderationStatus
        eventRepository.save(event)
        return true
    }

    @LogBusinessOperation(operationType = "EVENT_ADD_MATERIAL", description = "Добавление материала к мероприятию")
    override fun addEventMaterial(eventId: Long, uploaderEmail: String, file: MultipartFile, name: String, category: String, description: String): FileEventResponse {
        val event = eventRepository.findById(eventId).orElseThrow {
            EntityNotFoundException("Event not found with id $eventId")
        }
        val uploaderProfile = profileRepository.findByAccountEmail(uploaderEmail) ?: throw EntityNotFoundException("Profile not found for email $uploaderEmail")

        val isOrganizer = event.organizer.profileId == uploaderProfile.profileId
        val isCoowner = event.coowners.any { it.profileId == uploaderProfile.profileId }

        if (!isOrganizer && !isCoowner) {
            throw AccessDeniedException("User is not authorized to add materials to this event")
        }

        val originalFileName = file.originalFilename ?:
            if (name.contains('.')) name
            else {
                val fileExtension = when(file.contentType) {
                    "video/mp4" -> ".mp4"
                    "application/pdf" -> ".pdf"
                    "application/msword" -> ".doc"
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx"
                    "application/vnd.ms-excel" -> ".xls"
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx"
                    "image/jpeg" -> ".jpg"
                    "image/png" -> ".png"
                    else -> ""
                }
                "$name$fileExtension"
            }

        val s3Key = s3Service.uploadEventMaterial(file, eventId, originalFileName)

        val fileEvent = FileEvent(
            event = event,
            fileName = originalFileName,
            filePath = s3Key,
            uploadDate = Timestamp(System.currentTimeMillis()),
            fileType = file.contentType ?: "application/octet-stream",
            fileKindName = "MATERIAL",
            category = category,
            description = description
        )
        val savedFileEvent = fileEventRepository.save(fileEvent)
        event.fileEvents.add(savedFileEvent)
        eventRepository.save(event)
        return FileEventResponse.fromEntity(savedFileEvent, s3Service)
    }

    @LogBusinessOperation(operationType = "EVENT_EXPORT_PARTICIPANTS_EXCEL", description = "Экспорт участников мероприятия в Excel")
    override fun exportEventParticipantsToExcel(eventId: Long, email: String): ByteArray {
        val event = eventRepository.findById(eventId)
            .orElseThrow { NoSuchElementException("Event not found") }
        
        // Проверяем права пользователя (только организатор и совладельцы)
        val profile = profileRepository.findByAccountEmail(email)
            ?: throw IllegalArgumentException("User not found")
        
        val isOrganizer = event.organizer.profileId == profile.profileId
        val isCoowner = event.coowners.any { it.profileId == profile.profileId }
        
        if (!isOrganizer && !isCoowner) {
            throw IllegalArgumentException("Access denied. Only event organizer and co-owners can export participants list")
        }
        
        return exportGeneratorService.generateEventParticipantsExcel(event)
    }
    
    @LogBusinessOperation(operationType = "EVENT_EXPORT_CALENDAR", description = "Экспорт мероприятия в iCalendar")
    override fun exportEventToCalendar(eventId: Long): ByteArray {
        val event = eventRepository.findByIdOrNull(eventId)
            ?: throw IllegalArgumentException("Мероприятие с ID $eventId не найдено")

        return exportGeneratorService.generateICalendarFile(event)
    }

    @LogBusinessOperation(operationType = "EVENT_GET_FILTER_METADATA", description = "Получение метаданных для фильтрации мероприятий")
    @Cacheable(value = ["eventFilters"], key = "#root.methodName")
    override fun getEventFilterMetadata(): EventFilterMetadataResponse {
        // Получаем уникальные темы из базы данных
        val themes = eventRepository.findAllUniqueThemes().sorted()
        
        // Получаем все возможные типы из enum
        val types = EventType.entries
        
        // Получаем все возможные форматы из enum
        val formats = EventFormat.entries
        
        // Получаем уникальные местоположения
        val locations = eventRepository.findAllUniqueLocations().sorted()
        
        return EventFilterMetadataResponse(
            themes = themes,
            types = types,
            formats = formats,
            locations = locations
        )
    }

    @LogBusinessOperation(operationType = "EVENT_SEARCH_MULTIPLE_FILTERS", description = "Поиск мероприятий по нескольким фильтрам")
    @Cacheable(value = ["events"], key = "#root.methodName + ':' + #filterRequest.toString() + ':' + #filterRequest.page + ':' + #filterRequest.size")
    override fun searchEventsWithMultipleFilters(filterRequest: EventFilterRequest): Page<EventResponse> {
        println(filterRequest.toString())
        val pageable = PageRequest.of(filterRequest.page, filterRequest.size)
        
        val typesList = if (filterRequest.types.isEmpty()) null else filterRequest.types.mapNotNull {
            try { EventType.valueOf(it) } catch (e: Exception) { null } 
        }
        
        val formatsList = if (filterRequest.formats.isEmpty()) null else filterRequest.formats.mapNotNull {
            try { EventFormat.valueOf(it) } catch (e: Exception) { null } 
        }
        
        val normalizedTitle = filterRequest.title?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { "%$it%" }
        
        val dateFrom = filterRequest.dateFrom ?: LocalDateTime.of(1970, 1, 1, 0, 0)
        val dateTo = filterRequest.dateTo ?: LocalDateTime.of(3000, 1, 1, 0, 0)
        
        val isApplicationAvailable = filterRequest.isApplicationAvailable
        
        val now = LocalDateTime.now()

        val applyLiveStatusFilter = filterRequest.liveStatus.isNotEmpty()
        val requestedLiveStatuses = filterRequest.liveStatus.toSet()

        val filteredEventsPage = eventRepository.findByMultipleFilters(
            types = typesList,
            themes = filterRequest.themes.ifEmpty { null },
            formats = formatsList,
            locations = filterRequest.locations.ifEmpty { null },
            title = normalizedTitle,
            observersAllowed = filterRequest.observersAllowed,
            isApplicationAvailable = isApplicationAvailable,
            dateFrom = dateFrom,
            dateTo = dateTo,
            now = now,
            applyLiveStatusFilter = applyLiveStatusFilter,
            filterNotStarted = if (applyLiveStatusFilter) requestedLiveStatuses.contains(LiveStatus.NOT_STARTED) else false,
            filterInProgress = if (applyLiveStatusFilter) requestedLiveStatuses.contains(LiveStatus.IN_PROGRESS) else false,
            filterCompleted = if (applyLiveStatusFilter) requestedLiveStatuses.contains(LiveStatus.COMPLETED) else false,
            pageable = pageable
        )
        
        return filteredEventsPage.map { event -> EventResponse.fromEntity(event) }
    }

}
