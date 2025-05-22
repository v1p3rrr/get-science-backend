package com.getscience.getsciencebackend.notification.repository

import com.getscience.getsciencebackend.notification.data.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<Notification>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>
    fun findByUserIdAndIsReadFalse(userId: Long): List<Notification>
    fun countByUserIdAndIsReadFalse(userId: Long): Long
    fun deleteByUserId(userId: Long)
    fun deleteByIdAndUserId(id: Long, userId: Long)
} 