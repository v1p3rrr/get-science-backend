package com.getscience.getsciencebackend.notification.data

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "notification")
data class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: NotificationType,
    
    @Column(name = "title", nullable = false)
    val title: String,
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    val message: String,
    
    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "entity_id", nullable = false)
    val entityId: Long,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    val entityType: EntityType,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    val status: ApplicationStatus? = null
)

enum class NotificationType {
    NEW_APPLICATION,
    APPLICATION_STATUS_CHANGED,
    APPLICATION_UPDATED,
    EVENT_UPDATED,
    EVENT_DELETED,
    APPLICATION_DELETED
}

enum class EntityType {
    EVENT,
    APPLICATION
}

enum class ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
} 