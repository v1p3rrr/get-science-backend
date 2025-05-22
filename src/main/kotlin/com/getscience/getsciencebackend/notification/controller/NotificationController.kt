package com.getscience.getsciencebackend.notification.controller

import com.getscience.getsciencebackend.notification.data.Notification
import com.getscience.getsciencebackend.notification.service.NotificationService
import com.getscience.getsciencebackend.util.response_message.PageableResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {
    /**
     * Получает список уведомлений текущего пользователя с поддержкой пагинации.
     *
     * @param page номер страницы (начиная с 0)
     * @param size количество уведомлений на странице
     * @return страница с уведомлениями пользователя в формате PageableResponse
     */
    @GetMapping
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PageableResponse<Notification>> {
        val email = getEmailFromToken()
        val notificationsPage = notificationService.getNotifications(email, page, size)
        val response = PageableResponse(
            content = notificationsPage.content,
            hasNext = notificationsPage.hasNext(),
            totalPages = notificationsPage.totalPages,
            totalElements = notificationsPage.totalElements
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Получает количество непрочитанных уведомлений текущего пользователя.
     *
     * @return количество непрочитанных уведомлений
     */
    @GetMapping("/unread/count")
    fun getUnreadNotificationsCount(): ResponseEntity<Long> {
        val email = getEmailFromToken()
        return ResponseEntity.ok(notificationService.getUnreadNotificationsCount(email))
    }

    /**
     * Отмечает указанное уведомление как прочитанное.
     *
     * @param notificationId идентификатор уведомления
     * @return пустой ответ с кодом 200 при успешной операции
     */
    @PutMapping("/{notificationId}/read")
    fun markAsRead(@PathVariable notificationId: Long): ResponseEntity<Void> {
        val email = getEmailFromToken()
        notificationService.markAsRead(notificationId, email)
        return ResponseEntity.ok().build()
    }

    /**
     * Отмечает все уведомления пользователя как прочитанные.
     *
     * @return пустой ответ с кодом 200 при успешной операции
     */
    @PutMapping("/read-all")
    fun markAllAsRead(): ResponseEntity<Void> {
        val email = getEmailFromToken()
        notificationService.markAllAsRead(email)
        return ResponseEntity.ok().build()
    }

    /**
     * Удаляет указанное уведомление пользователя.
     *
     * @param notificationId идентификатор уведомления
     * @return пустой ответ с кодом 204 при успешной операции
     */
    @DeleteMapping("/{notificationId}")
    fun deleteNotification(@PathVariable notificationId: Long): ResponseEntity<Void> {
        val email = getEmailFromToken()
        notificationService.deleteNotification(notificationId, email)
        return ResponseEntity.noContent().build()
    }

    /**
     * Удаляет все уведомления текущего пользователя.
     *
     * @return пустой ответ с кодом 204 при успешной операции
     */
    @DeleteMapping
    fun deleteAllNotifications(): ResponseEntity<Void> {
        val email = getEmailFromToken()
        notificationService.deleteAllNotifications(email)
        return ResponseEntity.noContent().build()
    }

    /**
     * Извлекает email пользователя из текущего токена аутентификации.
     *
     * @return email текущего пользователя
     */
    private fun getEmailFromToken(): String {
        return SecurityContextHolder.getContext().authentication.name
    }
} 