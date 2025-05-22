package com.getscience.getsciencebackend.event.repository

import com.getscience.getsciencebackend.event.data.model.*
import com.getscience.getsciencebackend.user.data.model.Profile
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.data.jpa.repository.EntityGraph
import java.time.LocalDateTime

/**
 * Репозиторий для работы с сущностями мероприятий.
 * Предоставляет методы для выполнения CRUD-операций и специализированных запросов.
 */
@Repository
interface EventRepository : JpaRepository<Event, Long> {
    fun findByOrganizerProfileId(organizerId: Long, pageable: Pageable): Page<Event>
    fun findByOrganizerProfileId(organizerId: Long): List<Event>
    fun findByTitleContainingIgnoreCase(title: String): List<Event>
    fun findByTypeOrThemeOrLocationOrFormatOrTitleContainsIgnoreCase(
        type: EventType,
        theme: String,
        location: String,
        format: EventFormat,
        title: String
    ): List<Event>
    fun findEventsByTheme(theme: String): List<Event>

    /**
     * Находит события по теме с учетом статуса модерации и статуса события.
     *
     * @param theme тема события
     * @param moderationStatus статус модерации
     * @param status статус события
     * @return список событий, соответствующих критериям
     */
    fun findEventsByThemeAndModerationStatusAndStatus(
        theme: String,
        moderationStatus: EventModerationStatus,
        status: EventStatus
    ): List<Event>

    /**
     * Поиск событий по параметрам с поддержкой множественных значений.
     * Метод позволяет фильтровать события по спискам значений для типов, тем, локаций и форматов.
     *
     * @param types список типов событий
     * @param themes список тем событий
     * @param locations список локаций
     * @param formats список форматов событий
     * @param title подстрока для поиска в названии
     * @param moderationStatus статус модерации
     * @param status статус события
     * @param pageable параметры пагинации
     * @return постраничный список событий, соответствующих критериям
     */
    @Query("""
        SELECT e FROM Event e 
        WHERE (:#{#types == null || #types.isEmpty()} = true OR e.type IN :types)
          AND (:#{#themes == null || #themes.isEmpty()} = true OR e.theme IN :themes)
          AND (:#{#locations == null || #locations.isEmpty()} = true OR e.location IN :locations)
          AND (:#{#formats == null || #formats.isEmpty()} = true OR e.format IN :formats)
          AND (:title IS NULL OR LOWER(e.title) LIKE :title)
          AND (:moderationStatus IS NULL OR e.moderationStatus = :moderationStatus)
          AND (:status IS NULL OR e.status = :status)
    """)
    fun searchEventsWithMultipleValues(
        @Param("types") types: List<EventType>?,
        @Param("themes") themes: List<String>?,
        @Param("locations") locations: List<String>?,
        @Param("formats") formats: List<EventFormat>?,
        @Param("title") title: String?,
        @Param("moderationStatus") moderationStatus: EventModerationStatus?,
        @Param("status") status: EventStatus?,
        pageable: Pageable
    ): Page<Event>

    /**
     * Поиск событий по одиночным значениям параметров с пагинацией.
     *
     * @param type тип события
     * @param theme тема события
     * @param location локация события
     * @param format формат события
     * @param title подстрока для поиска в названии
     * @param moderationStatus статус модерации
     * @param status статус события
     * @param pageable параметры пагинации
     * @return постраничный список событий, соответствующих критериям
     */
    @Query("""
        SELECT e FROM Event e 
        WHERE (:type IS NULL OR e.type = :type)
          AND (:theme IS NULL OR e.theme = :theme)
          AND (:location IS NULL OR e.location = :location)
          AND (:format IS NULL OR e.format = :format)
          AND (:title IS NULL OR LOWER(e.title) LIKE :title)
          AND (:moderationStatus IS NULL OR e.moderationStatus = :moderationStatus)
          AND (:status IS NULL OR e.status = :status)
    """)
    fun searchEventsPaged(
        @Param("type") type: String?,
        @Param("theme") theme: String?,
        @Param("location") location: String?,
        @Param("format") format: String?,
        @Param("title") title: String?,
        @Param("moderationStatus") moderationStatus: EventModerationStatus?,
        @Param("status") status: EventStatus?,
        pageable: Pageable
    ): Page<Event>

    /**
     * Находит событие по идентификатору с предзагрузкой связанных рецензентов и со-организаторов.
     * Использует Entity Graph для оптимизации запроса.
     *
     * @param eventId идентификатор события
     * @return событие с загруженными связями или null
     */
    @EntityGraph(attributePaths = ["reviewers", "coowners"])
    fun findWithReviewersAndCoownersByEventId(eventId: Long): Event?

    fun findByOrganizer(organizer: Profile): List<Event>
    fun findByCoownersContaining(profile: Profile): List<Event>
    fun findByReviewersContaining(profile: Profile): List<Event>

    fun findByOrganizer(organizer: Profile, pageable: Pageable): Page<Event>
    fun findByCoownersContaining(profile: Profile, pageable: Pageable): Page<Event>
    fun findByReviewersContaining(profile: Profile, pageable: Pageable): Page<Event>

    fun findByModerationStatus(status: EventModerationStatus, pageable: Pageable): Page<Event>
    fun findByModerationStatusAndStatus(moderationStatus: EventModerationStatus, status: EventStatus, pageable: Pageable): Page<Event>
    fun findByModerationStatusIsNotAndStatus(moderationStatus: EventModerationStatus, status: EventStatus, pageable: Pageable): Page<Event>
    
    /**
     * Получение списка всех уникальных тем мероприятий.
     * Возвращает только темы мероприятий со статусом APPROVED и PUBLISHED.
     *
     * @return список уникальных тем мероприятий
     */
    @Query("SELECT DISTINCT e.theme FROM Event e WHERE e.theme IS NOT NULL AND e.theme <> '' AND e.moderationStatus = 'APPROVED' AND e.status = 'PUBLISHED'")
    fun findAllUniqueThemes(): List<String>
    
    /**
     * Получение списка всех уникальных местоположений мероприятий.
     * Возвращает только локации мероприятий со статусом APPROVED и PUBLISHED.
     *
     * @return список уникальных местоположений мероприятий
     */
    @Query("SELECT DISTINCT e.location FROM Event e WHERE e.location IS NOT NULL AND e.location <> '' AND e.moderationStatus = 'APPROVED' AND e.status = 'PUBLISHED'")
    fun findAllUniqueLocations(): List<String>

    /**
     * Расширенный поиск мероприятий по множественным критериям с учетом временных интервалов 
     * и статуса мероприятия относительно текущей даты.
     *
     * @param types типы мероприятий
     * @param themes темы мероприятий
     * @param formats форматы мероприятий
     * @param locations местоположения
     * @param title подстрока в названии
     * @param observersAllowed фильтр по возможности наблюдателей
     * @param isApplicationAvailable фильтр по возможности подачи заявок
     * @param dateFrom начальная дата для поиска
     * @param dateTo конечная дата для поиска
     * @param now текущая дата для вычисления статуса относительно даты
     * @param applyLiveStatusFilter флаг применения фильтра по статусу относительно даты
     * @param filterNotStarted включить мероприятия, которые еще не начались
     * @param filterInProgress включить мероприятия, которые идут сейчас
     * @param filterCompleted включить мероприятия, которые уже завершились
     * @param pageable параметры пагинации
     * @return постраничный список мероприятий, соответствующих критериям
     */
    @Query("""
        SELECT e FROM Event e
        WHERE
            (:types IS NULL OR e.type IN :types) AND
            (:themes IS NULL OR e.theme IN :themes) AND
            (:formats IS NULL OR e.format IN :formats) AND
            (:locations IS NULL OR e.location IN :locations) AND
            (:title IS NULL OR LOWER(e.title) LIKE :title) AND
            (:observersAllowed IS NULL OR e.observersAllowed = :observersAllowed) AND
            (
                :isApplicationAvailable IS NULL OR
                (:isApplicationAvailable = TRUE AND :now >= e.applicationStart AND :now <= e.applicationEnd) OR
                (:isApplicationAvailable = FALSE AND (:now < e.applicationStart OR :now > e.applicationEnd))
            ) AND
            (e.dateEnd >= :dateFrom) AND 
            (e.dateStart <= :dateTo) AND
            (
                :applyLiveStatusFilter = FALSE OR
                (:filterNotStarted = TRUE AND :now < e.dateStart) OR
                (:filterInProgress = TRUE AND :now >= e.dateStart AND :now <= e.dateEnd) OR
                (:filterCompleted = TRUE AND :now > e.dateEnd)
            )
            AND e.moderationStatus = 'APPROVED' AND e.status = 'PUBLISHED'
    """)
    fun findByMultipleFilters(
        @Param("types") types: List<EventType>?,
        @Param("themes") themes: List<String>?,
        @Param("formats") formats: List<EventFormat>?,
        @Param("locations") locations: List<String>?,
        @Param("title") title: String?,
        @Param("observersAllowed") observersAllowed: Boolean?,
        @Param("isApplicationAvailable") isApplicationAvailable: Boolean?,
        @Param("dateFrom") dateFrom: LocalDateTime,
        @Param("dateTo") dateTo: LocalDateTime,
        @Param("now") now: LocalDateTime,
        @Param("applyLiveStatusFilter") applyLiveStatusFilter: Boolean,
        @Param("filterNotStarted") filterNotStarted: Boolean,
        @Param("filterInProgress") filterInProgress: Boolean,
        @Param("filterCompleted") filterCompleted: Boolean,
        pageable: Pageable
    ): Page<Event>
}
