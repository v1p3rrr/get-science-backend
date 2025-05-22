package com.getscience.getsciencebackend.event.controller

import com.getscience.getsciencebackend.event.data.dto.EventFilterMetadataResponse
import com.getscience.getsciencebackend.event.data.dto.EventFilterRequest
import com.getscience.getsciencebackend.event.data.dto.EventRequest
import com.getscience.getsciencebackend.event.data.dto.EventResponse
import com.getscience.getsciencebackend.event.service.EventService
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.util.response_message.PageableResponse
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import com.getscience.getsciencebackend.event.data.model.EventModerationStatus
import org.springframework.web.multipart.MultipartFile
import java.security.Principal
import org.springframework.http.MediaType
import org.springframework.http.HttpHeaders

@RestController
@RequestMapping("api/v1/events")
class EventController(
    private val eventService: EventService
) {

    /**
     * Создает новое мероприятие с возможностью загрузки файлов.
     *
     * @param eventRequest информация о создаваемом мероприятии
     * @param fileEventRequest метаданные файлов для загрузки
     * @param files список файлов для загрузки
     * @return ResponseEntity с результатом операции
     */
    @PostMapping(consumes = ["multipart/form-data", "application/octet-stream"])
    fun createEventMultipart(
        @RequestPart eventRequest: EventRequest,
        @RequestPart fileEventRequest: List<FileEventRequest>?,
        @RequestPart("files") files: List<MultipartFile>?
    ): ResponseEntity<String> {
        val email = getEmailFromToken()
        val result = eventService.createEventWithFiles(eventRequest, fileEventRequest ?: emptyList(), files ?: emptyList(), email)
        return if (result) {
            ResponseEntity.ok("Success")
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Обновляет существующее мероприятие с возможностью добавления/обновления файлов.
     *
     * @param eventId идентификатор мероприятия
     * @param eventRequest обновленная информация о мероприятии
     * @param fileEventRequest метаданные файлов для загрузки
     * @param files список файлов для загрузки
     * @return ResponseEntity с результатом операции
     */
    @PutMapping(value = ["/{eventId}"], consumes = ["multipart/form-data", "application/octet-stream"])
    fun updateEventMultipart(
        @PathVariable eventId: Long,
        @RequestPart eventRequest: EventRequest,
        @RequestPart fileEventRequest: List<FileEventRequest>?,
        @RequestPart("files") files: List<MultipartFile>?
    ): ResponseEntity<String> {
        val email = getEmailFromToken()
        val result = eventService.updateEventWithFiles(eventId, eventRequest, fileEventRequest ?: emptyList(), files ?: emptyList(), email)
        return if (result) {
            ResponseEntity.ok("Success")
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Получает информацию о мероприятии по его идентификатору.
     *
     * @param eventId идентификатор мероприятия
     * @return ResponseEntity с данными мероприятия
     */
    @GetMapping("/{eventId}")
    fun getEventById(@PathVariable eventId: Long): ResponseEntity<EventResponse> {
        val eventResponse = eventService.getEventById(eventId)
        return ResponseEntity.ok(eventResponse)
    }

    /**
     * Удаляет мероприятие по его идентификатору.
     * Доступно только организатору мероприятия.
     *
     * @param eventId идентификатор мероприятия
     * @return ResponseEntity с результатом операции
     */
    @DeleteMapping("/{eventId}")
    fun deleteEvent(@PathVariable eventId: Long): ResponseEntity<String> {
        val email = getEmailFromToken()
        val result = eventService.deleteEvent(eventId, email)
        return if (result) {
            ResponseEntity.ok("Success")
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Получает список рекомендуемых мероприятий на основе указанного мероприятия.
     *
     * @param eventId идентификатор мероприятия, для которого нужны рекомендации
     * @return ResponseEntity со списком рекомендуемых мероприятий
     */
    @GetMapping("/{eventId}/recommendations")
    fun searchEvents(@PathVariable eventId: Long
    ): ResponseEntity<List<EventResponse>> {
        val events = eventService.getRecommendations(eventId, 4)
        return ResponseEntity.ok(events)
    }

    /**
     * Добавляет файл к мероприятию.
     *
     * @param eventId идентификатор мероприятия
     * @param fileEventRequest информация о добавляемом файле
     * @return ResponseEntity с информацией о добавленном файле
     */
    @PostMapping("/{eventId}/files")
    fun addFileToEvent(@PathVariable eventId: Long, @RequestBody fileEventRequest: FileEventRequest): ResponseEntity<FileEventResponse> {
        val email = getEmailFromToken()
        val fileEvent = eventService.addFileToEvent(eventId, fileEventRequest, email)
        return ResponseEntity.ok(FileEventResponse.fromEntity(fileEvent))
    }

    /**
     * Получает список файлов, связанных с мероприятием.
     *
     * @param eventId идентификатор мероприятия
     * @return ResponseEntity со списком файлов
     */
    @GetMapping("/{eventId}/files")
    fun getFilesByEvent(@PathVariable eventId: Long): ResponseEntity<List<FileEventResponse>> {
        val files = eventService.getFilesByEvent(eventId)
        return ResponseEntity.ok(files)
    }

    /**
     * Получает информацию о файле по его идентификатору.
     *
     * @param fileId идентификатор файла
     * @return ResponseEntity с информацией о файле
     */
    @GetMapping("/files/{fileId}")
    fun getFileEventById(@PathVariable fileId: Long): ResponseEntity<FileEventResponse> {
        val fileEvent = eventService.getFileEventById(fileId)
        return ResponseEntity.ok(fileEvent)
    }

    /**
     * Получает список всех мероприятий с пагинацией.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return ResponseEntity с постраничным списком мероприятий
     */
    @GetMapping
    fun getAllEventsPaged(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int
    ): ResponseEntity<PageableResponse<EventResponse>> {
        val eventsPage = eventService.getAllEventsPaged(page, size)
        val response = PageableResponse(
            content = eventsPage.content,
            hasNext = eventsPage.hasNext(),
            totalPages = eventsPage.totalPages,
            totalElements = eventsPage.totalElements
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Получает список мероприятий текущего пользователя с пагинацией.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return ResponseEntity с постраничным списком мероприятий
     */
    @GetMapping("/my-events")
    fun getEventsByJwtPaged(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int
    ): ResponseEntity<PageableResponse<EventResponse>> {
        val email = getEmailFromToken()
        val eventsPage = eventService.getEventsByJwtPaged(email, page, size)
        val response = PageableResponse(
            content = eventsPage.content,
            hasNext = eventsPage.hasNext(),
            totalPages = eventsPage.totalPages,
            totalElements = eventsPage.totalElements
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Получает список мероприятий указанного организатора с пагинацией.
     *
     * @param organizerId идентификатор организатора
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return ResponseEntity с постраничным списком мероприятий
     */
    @GetMapping("/organizer/{organizerId}")
    fun getEventsByOrganizerPaged(
            @PathVariable organizerId: Long,
            @RequestParam(defaultValue = "0") page: Int,
            @RequestParam(defaultValue = "12") size: Int
    ): ResponseEntity<PageableResponse<EventResponse>> {
        val eventsPage = eventService.getEventsByOrganizerPaged(organizerId, page, size)
        val response = PageableResponse(
            content = eventsPage.content,
            hasNext = eventsPage.hasNext(),
            totalPages = eventsPage.totalPages,
            totalElements = eventsPage.totalElements
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Получает информацию о мероприятии, включая данные о рецензентах и со-организаторах.
     *
     * @param eventId идентификатор мероприятия
     * @return ResponseEntity с полной информацией о мероприятии
     */
    @GetMapping("/{eventId}/with-people")
    fun getEventWithReviewersAndCoowners(@PathVariable eventId: Long): ResponseEntity<EventResponse> {
        val event = eventService.getEventWithReviewersAndCoownersById(eventId)
        return ResponseEntity.ok(event)
    }

    /**
     * Получает список мероприятий, организатором которых является текущий пользователь.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return ResponseEntity с постраничным списком мероприятий
     */
    @GetMapping("/my")
    fun getMyEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "6") size: Int
    ): ResponseEntity<Page<EventResponse>> {
        val email = getEmailFromToken()
        return ResponseEntity.ok(eventService.getMyEvents(email, page, size))
    }

    /**
     * Получает список мероприятий, где текущий пользователь является со-организатором.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return ResponseEntity с постраничным списком мероприятий
     */
    @GetMapping("/coowner")
    fun getCoownerEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "6") size: Int
    ): ResponseEntity<Page<EventResponse>> {
        val email = getEmailFromToken()
        return ResponseEntity.ok(eventService.getCoownerEvents(email, page, size))
    }

    /**
     * Получает список мероприятий, где текущий пользователь является рецензентом.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return ResponseEntity с постраничным списком мероприятий
     */
    @GetMapping("/reviewer")
    fun getReviewerEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "6") size: Int
    ): ResponseEntity<Page<EventResponse>> {
        val email = getEmailFromToken()
        return ResponseEntity.ok(eventService.getReviewerEvents(email, page, size))
    }

    /**
     * Получает список мероприятий, ожидающих модерации.
     * Доступно только пользователям с ролью модератора.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return ResponseEntity с постраничным списком мероприятий
     */
    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/moderation/pending")
    fun getPendingModerationEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int
    ): ResponseEntity<PageableResponse<EventResponse>> {
        val eventsPage = eventService.getModerationNonApprovedEvents(page, size)
        val response = PageableResponse(
            content = eventsPage.content,
            hasNext = eventsPage.hasNext(),
            totalPages = eventsPage.totalPages,
            totalElements = eventsPage.totalElements
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Получает список мероприятий, прошедших модерацию.
     * Доступно только пользователям с ролью модератора.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество элементов на странице
     * @return ResponseEntity с постраничным списком мероприятий
     */
    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/moderation/approved")
    fun getApprovedModerationEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int
    ): ResponseEntity<PageableResponse<EventResponse>> {
        val eventsPage = eventService.getModerationApprovedEvents(page, size)
        val response = PageableResponse(
            content = eventsPage.content,
            hasNext = eventsPage.hasNext(),
            totalPages = eventsPage.totalPages,
            totalElements = eventsPage.totalElements
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Обновляет статус модерации мероприятия.
     * Доступно только пользователям с ролью модератора.
     *
     * @param eventId идентификатор мероприятия
     * @param moderationStatus новый статус модерации
     * @return ResponseEntity с результатом операции
     */
    @PreAuthorize("hasRole('MODERATOR')")
    @PatchMapping("/{eventId}/moderation")
    fun updateEventModerationStatus(
        @PathVariable eventId: Long,
        @RequestBody moderationStatus: EventModerationStatus
    ): ResponseEntity<String> {
        val email = getEmailFromToken()
        val result = eventService.updateEventModerationStatus(eventId, moderationStatus, email)
        return if (result) {
            ResponseEntity.ok("Success")
        } else {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Добавляет материал к мероприятию.
     * Требует аутентификации пользователя.
     *
     * @param eventId идентификатор мероприятия
     * @param file загружаемый файл
     * @param name название материала
     * @param category категория материала
     * @param description описание материала
     * @param principal информация о пользователе
     * @return ResponseEntity с информацией о добавленном материале
     */
    @PostMapping("/{eventId}/material")
    @PreAuthorize("isAuthenticated()")
    fun addEventMaterial(
        @PathVariable eventId: Long,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("name") name: String,
        @RequestParam("category") category: String,
        @RequestParam("description") description: String,
        principal: Principal
    ): ResponseEntity<FileEventResponse> {
        val fileEventResponse = eventService.addEventMaterial(eventId, principal.name, file, name, category, description)
        return ResponseEntity.ok(fileEventResponse)
    }

    /**
     * Экспортирует событие в формате iCalendar
     *
     * @param eventId ID события для экспорта
     * @return Файл в формате iCalendar
     */
    @GetMapping("/{eventId}/calendar")
    fun exportEventCalendar(@PathVariable eventId: Long): ResponseEntity<ByteArray> {
        val calendarData = eventService.exportEventToCalendar(eventId)
        val event = eventService.getEventById(eventId)
        
        // Формируем безопасное имя файла из названия события
        val fileName = event.title
            .replace(Regex("[^\\w\\s-]"), "")  // Удаляем специальные символы
            .replace(Regex("\\s+"), "_")       // Заменяем пробелы на подчеркивания
            .trim()
                
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/calendar"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileName}.ics\"")
            .body(calendarData)
    }

    /**
     * Экспортирует список участников события в формате Excel
     *
     * @param eventId ID события для экспорта
     * @return Excel-файл со списком участников
     */
    @GetMapping("/{eventId}/participants/export")
    @PreAuthorize("isAuthenticated()")
    fun exportEventParticipantsExcel(@PathVariable eventId: Long): ResponseEntity<ByteArray> {
        val email = getEmailFromToken()
        val excelData = eventService.exportEventParticipantsToExcel(eventId, email)
        val event = eventService.getEventById(eventId)
        
        // Формируем безопасное имя файла из названия события
        val fileName = event.title
            .replace(Regex("[^\\w\\s-]"), "")  // Удаляем специальные символы
            .replace(Regex("\\s+"), "_")       // Заменяем пробелы на подчеркивания
            .trim()
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${fileName}_participants.xlsx\"")
            .body(excelData)
    }

    /**
     * Получает метаданные для построения фильтров мероприятий.
     *
     * @return ResponseEntity с метаданными фильтров
     */
    @GetMapping("/filter-metadata")
    fun getEventFilterMetadata(): ResponseEntity<EventFilterMetadataResponse> {
        val metadata = eventService.getEventFilterMetadata()
        return ResponseEntity.ok(metadata)
    }

    /**
     * Выполняет расширенный поиск мероприятий с поддержкой множественных фильтров.
     *
     * @param filterRequest запрос с параметрами фильтрации
     * @return ResponseEntity с постраничным списком отфильтрованных мероприятий
     */
    @PostMapping("/search")
    fun searchEventsWithMultipleFilters(@RequestBody filterRequest: EventFilterRequest): ResponseEntity<PageableResponse<EventResponse>> {
        val adjustedRequest = if (filterRequest.dateFrom != null || filterRequest.dateTo != null) {
            filterRequest.copy()
        } else {
            filterRequest
        }
        
        val eventsPage = eventService.searchEventsWithMultipleFilters(adjustedRequest)
        val response = PageableResponse(
            content = eventsPage.content,
            hasNext = eventsPage.hasNext(),
            totalPages = eventsPage.totalPages,
            totalElements = eventsPage.totalElements
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Извлекает email пользователя из текущего токена аутентификации.
     *
     * @return email пользователя
     */
    private fun getEmailFromToken(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val email = authentication.name
        return email
    }
}