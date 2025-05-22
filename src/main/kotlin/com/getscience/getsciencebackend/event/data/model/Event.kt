package com.getscience.getsciencebackend.event.data.model

import com.getscience.getsciencebackend.file.data.model.FileEvent
import com.getscience.getsciencebackend.user.data.model.Profile
import com.getscience.getsciencebackend.application.data.model.Application

import jakarta.persistence.*
import java.io.Serializable
import java.util.*

@Entity
@Table(name = "event")
data class Event(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val eventId: Long = 0,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(columnDefinition = "TEXT")
    var organizerDescription: String? = null,

    @Column(nullable = false)
    var dateStart: Date,

    @Column(nullable = false)
    var dateEnd: Date,

    @Column(nullable = false)
    var location: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: EventType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var format: EventFormat,

    var theme: String? = null,

    @Column(nullable = false)
    var observersAllowed: Boolean,

    @ManyToOne
    @JoinColumn(name = "organizer_id", referencedColumnName = "profileId")
    var organizer: Profile,

    @Column(columnDefinition = "TEXT")
    var results: String? = null,

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var documentsRequired: MutableList<DocRequired> = mutableListOf(),

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var fileEvents: MutableList<FileEvent> = mutableListOf(),

    @Column(nullable = false)
    var applicationStart: Date,

    @Column(nullable = false)
    var applicationEnd: Date,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.DRAFT,

    @Column(nullable = false)
    var hidden: Boolean = false,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "event_reviewers",
        joinColumns = [JoinColumn(name = "event_id")],
        inverseJoinColumns = [JoinColumn(name = "profile_id")]
    )
    var reviewers: MutableSet<Profile> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "event_coowners",
        joinColumns = [JoinColumn(name = "event_id")],
        inverseJoinColumns = [JoinColumn(name = "profile_id")]
    )
    var coowners: MutableSet<Profile> = mutableSetOf(),

    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    var applications: MutableList<Application> = mutableListOf(),

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    var moderationStatus: EventModerationStatus = EventModerationStatus.PENDING
) : Serializable {
    override fun hashCode(): Int {
        var result = eventId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + dateStart.hashCode()
        result = 31 * result + dateEnd.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + observersAllowed.hashCode()
        result = 31 * result + organizer.hashCode()
        result = 31 * result + (theme?.hashCode() ?: 0)
        result = 31 * result + (organizerDescription?.hashCode() ?: 0)
        result = 31 * result + (results?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (eventId != other.eventId) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (dateStart != other.dateStart) return false
        if (dateEnd != other.dateEnd) return false
        if (location != other.location) return false
        if (type != other.type) return false
        if (observersAllowed != other.observersAllowed) return false
        if (organizer != other.organizer) return false
        if (theme != other.theme) return false
        if (organizerDescription != other.organizerDescription) return false
        if (results != other.results) return false

        return true
    }

    override fun toString(): String {
        return "Event(eventId=$eventId, title='$title', description='$description', organizerDescription=$organizerDescription, " +
                "dateStart=$dateStart, dateEnd=$dateEnd, location='$location', type=$type, theme=$theme, status=$status, " +
                "moderationStatus=$moderationStatus, observersAllowed=$observersAllowed, organizerId=${organizer.profileId}, " +
                "documentsRequiredCount=${documentsRequired.size}, fileEventsCount=${fileEvents.size}, " +
                "reviewersCount=${reviewers.size}, coownersCount=${coowners.size}, applicationsCount=${applications.size}, results=$results)"
    }
}

/**
 * Статусы модерации мероприятия.
 * Определяют текущее состояние мероприятия с точки зрения проверки модератором.
 */
enum class EventModerationStatus {
    PENDING,   // Ожидает модерации
    APPROVED,  // Одобрено модератором
    REJECTED,  // Отклонено модератором
    EDITING,   // Находится в процессе редактирования
    BLOCKED    // Заблокировано модератором
}

/**
 * Статусы публикации мероприятия.
 * Определяют текущее состояние мероприятия с точки зрения его жизненного цикла в системе.
 */
enum class EventStatus {
    DRAFT,     // Черновик, не опубликовано
    PUBLISHED, // Опубликовано, доступно пользователям
    ARCHIVED   // Архивировано
}

/**
 * Форматы проведения мероприятия.
 * Определяют способ участия в мероприятии.
 */
enum class EventFormat {
    ONLINE,    // Проводится онлайн
    OFFLINE,   // Проводится офлайн
    HYBRID     // Комбинированный формат
}

/**
 * Типы мероприятий.
 * Определяют характер проводимого мероприятия.
 */
enum class EventType {
    CONFERENCE, // Конференция
    SEMINAR     // Семинар
}