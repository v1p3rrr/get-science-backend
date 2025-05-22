package com.getscience.getsciencebackend.notification.service

import com.getscience.getsciencebackend.notification.data.ApplicationStatus
import com.getscience.getsciencebackend.notification.data.EntityType
import com.getscience.getsciencebackend.notification.data.Notification
import com.getscience.getsciencebackend.notification.data.NotificationType
import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import org.springframework.data.domain.Page

/**
 * Сервис для управления уведомлениями пользователей.
 * 
 * Предоставляет методы для создания, получения, отметки о прочтении
 * и удаления уведомлений в системе.
 */
interface NotificationService {
    /**
     * Создает новое уведомление для пользователя и отправляет email-уведомление.
     *
     * @param email email пользователя, для которого создается уведомление
     * @param type тип уведомления
     * @param title заголовок уведомления
     * @param message содержание уведомления
     * @param entityId идентификатор связанной сущности (заявки или мероприятия)
     * @param entityType тип связанной сущности (заявка или мероприятие)
     * @param status опциональный статус заявки (если уведомление связано с заявкой)
     * @return созданное уведомление
     */
    fun createNotification(
        email: String,
        type: NotificationType,
        title: String,
        message: String,
        entityId: Long,
        entityType: EntityType,
        status: ApplicationStatus? = null
    ): Notification

    /**
     * Получает список уведомлений пользователя с поддержкой пагинации.
     *
     * @param email email пользователя, для которого запрашиваются уведомления
     * @param page номер страницы (начиная с 0)
     * @param size количество уведомлений на странице
     * @return страница с уведомлениями пользователя
     */
    fun getNotifications(email: String, page: Int, size: Int): Page<Notification>

    /**
     * Возвращает количество непрочитанных уведомлений для пользователя.
     *
     * @param email email пользователя
     * @return количество непрочитанных уведомлений
     */
    fun getUnreadNotificationsCount(email: String): Long

    /**
     * Отмечает конкретное уведомление как прочитанное.
     *
     * @param notificationId идентификатор уведомления
     * @param email email пользователя (для проверки прав доступа)
     */
    fun markAsRead(notificationId: Long, email: String)

    /**
     * Отмечает все уведомления пользователя как прочитанные.
     *
     * @param email email пользователя
     */
    fun markAllAsRead(email: String)

    /**
     * Удаляет конкретное уведомление пользователя.
     *
     * @param notificationId идентификатор уведомления
     * @param email email пользователя (для проверки прав доступа)
     */
    fun deleteNotification(notificationId: Long, email: String)

    /**
     * Удаляет все уведомления пользователя.
     *
     * @param email email пользователя
     */
    fun deleteAllNotifications(email: String)
}

