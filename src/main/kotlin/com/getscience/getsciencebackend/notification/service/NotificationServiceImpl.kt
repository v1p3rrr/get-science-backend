package com.getscience.getsciencebackend.notification.service

import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import com.getscience.getsciencebackend.email.EmailService
import com.getscience.getsciencebackend.notification.data.ApplicationStatus
import com.getscience.getsciencebackend.notification.data.EntityType
import com.getscience.getsciencebackend.notification.data.Notification
import com.getscience.getsciencebackend.notification.data.NotificationType
import com.getscience.getsciencebackend.notification.repository.NotificationRepository
import com.getscience.getsciencebackend.user.service.AccountService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationServiceImpl(
    private val notificationRepository: NotificationRepository,
    private val emailService: EmailService,
    private val accountService: AccountService,
    @Qualifier("notificationCoroutineScope") private val notificationCoroutineScope: CoroutineScope
) : NotificationService {

    private val logger = LoggerFactory.getLogger(NotificationServiceImpl::class.java)

    /**
     * Создает новое уведомление для пользователя и асинхронно отправляет email-уведомление.
     *
     * @param email email пользователя, для которого создается уведомление
     * @param type тип уведомления
     * @param title заголовок уведомления
     * @param message содержание уведомления
     * @param entityId идентификатор связанной сущности (заявки или мероприятия)
     * @param entityType тип связанной сущности (заявка или мероприятие)
     * @param status опциональный статус заявки (если уведомление связано с заявкой)
     * @return созданное уведомление
     * @throws IllegalArgumentException если профиль пользователя не найден
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_CREATE", description = "Создание нового уведомления")
    @Transactional
    override fun createNotification(
        email: String,
        type: NotificationType,
        title: String,
        message: String,
        entityId: Long,
        entityType: EntityType,
        status: ApplicationStatus?
    ): Notification {
        val profile = accountService.findProfileDTOByEmail(email)
        if (profile == null) {
            logger.error("Profile not found for email: $email")
            throw IllegalArgumentException("Profile not found for the given email")
        }

        val notification = Notification(
            userId = profile.profileId,
            type = type,
            title = title,
            message = message,
            entityId = entityId,
            entityType = entityType,
            status = status
        )
        notificationRepository.save(notification)

        // Отправляем email-уведомление
        if (email.isNotBlank()) {
            notificationCoroutineScope.launch {
                try {
                    emailService.sendEmail(to = email, subject = title, content = message)
                } catch (e: Exception) {
                    logger.error("Failed to send email notification to $email", e)
                }
            }
        }
        return notification
    }

    /**
     * Получает список уведомлений пользователя с поддержкой пагинации.
     *
     * @param email email пользователя, для которого запрашиваются уведомления
     * @param page номер страницы (начиная с 0)
     * @param size количество уведомлений на странице
     * @return страница с уведомлениями пользователя или пустая страница, если профиль не найден
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_GET_ALL", description = "Получение всех уведомлений пользователя")
    @Transactional
    override fun getNotifications(email: String, page: Int, size: Int): Page<Notification> {
        val profile = accountService.findProfileDTOByEmail(email)
        if (profile == null) {
            logger.error("Profile not found for email: $email")
            return Page.empty()
        }
        val pageable = PageRequest.of(page, size)
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(profile.profileId, pageable)
    }

    /**
     * Возвращает количество непрочитанных уведомлений для пользователя.
     *
     * @param email email пользователя
     * @return количество непрочитанных уведомлений или 0, если профиль не найден
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_GET_UNREAD_COUNT", description = "Получение количества непрочитанных уведомлений")
    @Transactional
    override fun getUnreadNotificationsCount(email: String): Long {
        val profile = accountService.findProfileDTOByEmail(email)
        if (profile == null) {
            logger.error("Profile not found for email: $email")
            return 0
        }
        return notificationRepository.countByUserIdAndIsReadFalse(profile.profileId)
    }

    /**
     * Отмечает конкретное уведомление как прочитанное.
     * Проверяет принадлежность уведомления пользователю.
     *
     * @param notificationId идентификатор уведомления
     * @param email email пользователя (для проверки прав доступа)
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_MARK_AS_READ", description = "Отметка уведомления как прочитанного")
    @Transactional
    override fun markAsRead(notificationId: Long, email: String) {
        val profile = accountService.findProfileDTOByEmail(email)
        if (profile == null) {
            logger.error("Profile not found for email: $email")
            return
        }

        notificationRepository.findById(notificationId).ifPresent { notification ->
            if (notification.userId == profile.profileId) {
                notification.isRead = true
                notificationRepository.save(notification)
            } else {
                logger.error("User $email tried to mark notification $notificationId as read, but it belongs to another user")
            }
        }
    }

    /**
     * Отмечает все уведомления пользователя как прочитанные.
     *
     * @param email email пользователя
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_MARK_ALL_AS_READ", description = "Отметка всех уведомлений как прочитанных")
    @Transactional
    override fun markAllAsRead(email: String) {
        val profile = accountService.findProfileDTOByEmail(email)
        if (profile == null) {
            logger.error("Profile not found for email: $email")
            return
        }

        val notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(profile.profileId)
        notifications.forEach { notification ->
            notification.isRead = true
            notificationRepository.save(notification)
        }
    }

    /**
     * Удаляет конкретное уведомление пользователя.
     * Проверяет принадлежность уведомления пользователю.
     *
     * @param notificationId идентификатор уведомления
     * @param email email пользователя (для проверки прав доступа)
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_DELETE", description = "Удаление уведомления")
    @Transactional
    override fun deleteNotification(notificationId: Long, email: String) {
        val profile = accountService.findProfileDTOByEmail(email)
        if (profile == null) {
            logger.error("Profile not found for email: $email")
            return
        }
        // Удаляем только если уведомление принадлежит пользователю
        notificationRepository.deleteByIdAndUserId(notificationId, profile.profileId)
    }

    /**
     * Удаляет все уведомления пользователя.
     *
     * @param email email пользователя
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_DELETE_ALL", description = "Удаление всех уведомлений пользователя")
    @Transactional
    override fun deleteAllNotifications(email: String) {
        val profile = accountService.findProfileDTOByEmail(email)
        if (profile == null) {
            logger.error("Profile not found for email: $email")
            return
        }
        notificationRepository.deleteByUserId(profile.profileId)
    }
} 