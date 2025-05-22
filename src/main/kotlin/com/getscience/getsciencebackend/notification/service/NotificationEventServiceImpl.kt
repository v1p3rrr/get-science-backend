package com.getscience.getsciencebackend.notification.service

import com.getscience.getsciencebackend.monitoring.LogBusinessOperation
import com.getscience.getsciencebackend.notification.data.ApplicationStatus
import com.getscience.getsciencebackend.notification.data.EntityType
import com.getscience.getsciencebackend.notification.data.NotificationType
import com.getscience.getsciencebackend.user.service.ProfileService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.user.data.model.Profile

@Service
class NotificationEventServiceImpl(
    private val notificationService: NotificationService,
    private val profileService: ProfileService
) : NotificationEventService {

    /**
     * Обрабатывает создание новой заявки на участие в мероприятии.
     * Отправляет уведомление организатору мероприятия.
     *
     * @param applicationId идентификатор новой заявки
     * @param eventId идентификатор мероприятия
     * @param eventOwnerId идентификатор профиля организатора мероприятия
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_NEW_APPLICATION", description = "Обработка новой заявки для уведомления")
    @Transactional
    override fun handleNewApplication(applicationId: Long, eventId: Long, eventOwnerId: Long) {
        // Уведомление для организатора
        profileService.findByProfileId(eventOwnerId)?.email?.let {
            notificationService.createNotification(
                email = it,
                type = NotificationType.NEW_APPLICATION,
                title = "Новая заявка",
                message = "Получена новая заявка на участие в мероприятии",
                entityId = applicationId,
                entityType = EntityType.APPLICATION,
                status = ApplicationStatus.PENDING
            )
        }
    }

    /**
     * Обрабатывает изменение статуса заявки.
     * Отправляет уведомление подавшему заявку пользователю.
     *
     * @param applicationId идентификатор заявки
     * @param eventId идентификатор мероприятия
     * @param userId идентификатор профиля пользователя, подавшего заявку
     * @param newStatus новый статус заявки (строковое представление)
     */
    @LogBusinessOperation(operationType = "NOTIFICATION_APP_STATUS_CHANGED", description = "Обработка изменения статуса заявки для уведомления")
    @Transactional
    override fun handleApplicationStatusChanged(
        applicationId: Long,
        eventId: Long,
        userId: Long,
        newStatus: String
    ) {
        // Уведомление для заявителя
        profileService.findByProfileId(userId)?.email?.let {
            val statusMessage = when (newStatus) {
                "APPROVED" -> "Ваша заявка одобрена"
                "REJECTED" -> "Ваша заявка отклонена"
                "PENDING" -> "Статус вашей заявки изменен на 'В ожидании'"
                else -> "Статус вашей заявки изменен на: $newStatus"
            }

            notificationService.createNotification(
                email = it,
                type = NotificationType.APPLICATION_STATUS_CHANGED,
                title = "Изменение статуса заявки",
                message = statusMessage,
                entityId = applicationId,
                entityType = EntityType.APPLICATION,
                status = ApplicationStatus.valueOf(newStatus.uppercase(Locale.getDefault()))
            )
        }
    }

    /**
     * Уведомляет заявителя об обновлении его заявки.
     * Создает и отправляет уведомление пользователю, чья заявка была обновлена.
     *
     * @param application обновленная заявка
     */
    @LogBusinessOperation(operationType = "NOTIFY_APPLICANT_APP_UPDATE", description = "Уведомление заявителя об обновлении заявки")
    override fun notifyApplicantAboutApplicationUpdate(application: Application) {
        val applicantEmail = application.profile.account.email
        notificationService.createNotification(
            email = applicantEmail,
            type = NotificationType.APPLICATION_UPDATED,
            title = "Ваша заявка обновлена",
            message = "Ваша заявка на мероприятие '${application.event.title}' была изменена организатором или рецензентом.",
            entityId = application.applicationId,
            entityType = EntityType.APPLICATION
        )
    }

    /**
     * Уведомляет персонал мероприятия об обновлении заявки.
     * Отправляет уведомления всем организаторам, соорганизаторам и рецензентам мероприятия,
     * кроме инициатора обновления и автора заявки.
     *
     * @param application обновленная заявка
     * @param initiator профиль пользователя, инициировавшего обновление
     */
    @LogBusinessOperation(operationType = "NOTIFY_STAFF_APP_UPDATE", description = "Уведомление организаторов об обновлении заявки")
    override fun notifyEventStaffAboutApplicationUpdate(application: Application, initiator: Profile) {
        val event = application.event
        val staffProfiles = mutableSetOf<Profile>()
        staffProfiles.add(event.organizer)
        staffProfiles.addAll(event.coowners)
        staffProfiles.addAll(event.reviewers)
        staffProfiles.remove(initiator)
        staffProfiles.remove(application.profile) // не уведомлять инициатора и автора заявки
        staffProfiles.forEach { staff ->
            notificationService.createNotification(
                email = staff.account.email,
                type = NotificationType.APPLICATION_UPDATED,
                title = "Обновлена заявка",
                message = "Заявка на мероприятие '${event.title}' была обновлена участником.",
                entityId = application.applicationId,
                entityType = EntityType.APPLICATION
            )
        }
    }

    /**
     * Уведомляет персонал мероприятия об изменении или удалении мероприятия.
     * Отправляет уведомления всем организаторам, соорганизаторам и рецензентам мероприятия,
     * кроме инициатора изменения.
     *
     * @param event измененное или удаленное мероприятие
     * @param initiator профиль пользователя, инициировавшего изменение
     * @param isDelete true если мероприятие удалено, false если обновлено
     */
    @LogBusinessOperation(operationType = "NOTIFY_STAFF_EVENT_CHANGE", description = "Уведомление организаторов об изменении события")
    override fun notifyEventStaffAboutEventChange(event: Event, initiator: Profile, isDelete: Boolean) {
        val staffProfiles = mutableSetOf<Profile>()
        staffProfiles.add(event.organizer)
        staffProfiles.addAll(event.coowners)
        staffProfiles.addAll(event.reviewers)
        staffProfiles.remove(initiator)
        staffProfiles.forEach { staff ->
            notificationService.createNotification(
                email = staff.account.email,
                type = if (isDelete) NotificationType.EVENT_DELETED else NotificationType.EVENT_UPDATED,
                title = if (isDelete) "Мероприятие удалено" else "Мероприятие обновлено",
                message = if (isDelete)
                    "Мероприятие '${event.title}' было удалено другим организатором/совладельцем/рецензентом."
                else
                    "Мероприятие '${event.title}' было обновлено другим организатором/совладельцем/рецензентом.",
                entityId = event.eventId,
                entityType = EntityType.EVENT
            )
        }
    }

    /**
     * Уведомляет всех заявителей об изменении или удалении мероприятия.
     * Отправляет уведомления всем пользователям, подавшим заявки на мероприятие,
     * кроме инициатора изменения.
     *
     * @param event измененное или удаленное мероприятие
     * @param initiator профиль пользователя, инициировавшего изменение
     * @param isDelete true если мероприятие удалено, false если обновлено
     */
    @LogBusinessOperation(operationType = "NOTIFY_APPLICANTS_EVENT_CHANGE", description = "Уведомление заявителей об изменении события")
    override fun notifyApplicantsAboutEventChange(event: Event, initiator: Profile, isDelete: Boolean) {
        val applications = event.applications
        applications.forEach { application ->
            if (application.profile.profileId != initiator.profileId) {
                notificationService.createNotification(
                    email = application.profile.account.email,
                    type = if (isDelete) NotificationType.EVENT_DELETED else NotificationType.EVENT_UPDATED,
                    title = if (isDelete) "Мероприятие удалено" else "Мероприятие обновлено",
                    message = if (isDelete)
                        "Мероприятие '${event.title}' было удалено. Ваша заявка также удалена."
                    else
                        "Мероприятие '${event.title}' было обновлено.",
                    entityId = event.eventId,
                    entityType = EntityType.EVENT
                )
            }
        }
    }

    /**
     * Уведомляет персонал мероприятия об удалении заявки участником.
     * Отправляет уведомления всем организаторам, соорганизаторам и рецензентам мероприятия,
     * кроме инициатора удаления.
     *
     * @param application удаленная заявка
     * @param initiator профиль пользователя, инициировавшего удаление
     */
    @LogBusinessOperation(operationType = "NOTIFY_STAFF_APP_DELETION", description = "Уведомление организаторов об удалении заявки")
    override fun notifyEventStaffAboutApplicationDeletion(application: Application, initiator: Profile) {
        val event = application.event
        val staffProfiles = mutableSetOf<Profile>()
        staffProfiles.add(event.organizer)
        staffProfiles.addAll(event.coowners)
        staffProfiles.addAll(event.reviewers)
        staffProfiles.remove(initiator)

        staffProfiles.forEach { staff ->
            notificationService.createNotification(
                email = staff.account.email,
                type = NotificationType.APPLICATION_DELETED,
                title = "Заявка отменена участником",
                message = "Участник отменил свою заявку на мероприятие '${event.title}'.",
                entityId = application.applicationId,
                entityType = EntityType.APPLICATION
            )
        }
    }
} 