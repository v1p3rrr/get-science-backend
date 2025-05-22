package com.getscience.getsciencebackend.notification.controller

import com.getscience.getsciencebackend.notification.data.ApplicationStatus
import com.getscience.getsciencebackend.notification.data.EntityType
import com.getscience.getsciencebackend.notification.data.Notification
import com.getscience.getsciencebackend.notification.data.NotificationType
import com.getscience.getsciencebackend.notification.service.NotificationService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class NotificationControllerTest {

    @Mock
    private lateinit var notificationService: NotificationService

    @Mock
    private lateinit var securityContext: SecurityContext

    @Mock
    private lateinit var authentication: Authentication

    @InjectMocks
    private lateinit var notificationController: NotificationController

    private val testEmail = "test@example.com"
    private val testNotificationId = 1L

    @BeforeEach
    fun setUp() {
        `when`(authentication.name).thenReturn(testEmail)
        `when`(securityContext.authentication).thenReturn(authentication)
        SecurityContextHolder.setContext(securityContext)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    private fun createMockNotification(id: Long): Notification {
        return Notification(
            id = id,
            userId = 100L,
            type = NotificationType.EVENT_UPDATED,
            title = "Test Notification $id",
            message = "This is a test notification $id.",
            isRead = false,
            createdAt = LocalDateTime.now(),
            entityId = 200L + id,
            entityType = EntityType.EVENT,
            status = ApplicationStatus.PENDING
        )
    }

    @Test
    fun `getNotifications should return pageable response of notifications`() {
        val page = 0
        val size = 10
        val notifications = listOf(createMockNotification(1), createMockNotification(2))
        val pageable = PageRequest.of(page, size)
        val notificationsPage = PageImpl(notifications, pageable, notifications.size.toLong())

        `when`(notificationService.getNotifications(testEmail, page, size)).thenReturn(notificationsPage)

        val responseEntity = notificationController.getNotifications(page, size)

        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        assertNotNull(responseEntity.body)
        assertEquals(notifications.size, responseEntity.body?.content?.size)
        assertEquals(notificationsPage.hasNext(), responseEntity.body?.hasNext)
        assertEquals(notificationsPage.totalPages, responseEntity.body?.totalPages)
        assertEquals(notificationsPage.totalElements, responseEntity.body?.totalElements)
        verify(notificationService).getNotifications(testEmail, page, size)
    }

    @Test
    fun `getUnreadNotificationsCount should return count of unread notifications`() {
        val unreadCount = 5L
        `when`(notificationService.getUnreadNotificationsCount(testEmail)).thenReturn(unreadCount)

        val responseEntity = notificationController.getUnreadNotificationsCount()

        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        assertEquals(unreadCount, responseEntity.body)
        verify(notificationService).getUnreadNotificationsCount(testEmail)
    }

    @Test
    fun `markAsRead should mark notification as read and return OK`() {
        doNothing().`when`(notificationService).markAsRead(testNotificationId, testEmail)

        val responseEntity = notificationController.markAsRead(testNotificationId)

        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        verify(notificationService).markAsRead(testNotificationId, testEmail)
    }

    @Test
    fun `markAllAsRead should mark all notifications as read and return OK`() {
        doNothing().`when`(notificationService).markAllAsRead(testEmail)

        val responseEntity = notificationController.markAllAsRead()

        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        verify(notificationService).markAllAsRead(testEmail)
    }

    @Test
    fun `deleteNotification should delete notification and return NoContent`() {
        doNothing().`when`(notificationService).deleteNotification(testNotificationId, testEmail)

        val responseEntity = notificationController.deleteNotification(testNotificationId)

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.statusCode)
        verify(notificationService).deleteNotification(testNotificationId, testEmail)
    }

    @Test
    fun `deleteAllNotifications should delete all notifications and return NoContent`() {
        doNothing().`when`(notificationService).deleteAllNotifications(testEmail)

        val responseEntity = notificationController.deleteAllNotifications()

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.statusCode)
        verify(notificationService).deleteAllNotifications(testEmail)
    }
} 