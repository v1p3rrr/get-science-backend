package com.getscience.getsciencebackend.notification.service

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.user.data.model.Profile

/**
 * Сервис для обработки событий, связанных с уведомлениями.
 * 
 * Предоставляет методы для отправки уведомлений различным пользователям
 * при изменениях статуса заявок, обновлении мероприятий и других бизнес-событиях.
 */
interface NotificationEventService {
    /**
     * Обрабатывает создание новой заявки на участие в мероприятии.
     * Отправляет уведомление организатору мероприятия.
     *
     * @param applicationId идентификатор новой заявки
     * @param eventId идентификатор мероприятия
     * @param eventOwnerId идентификатор профиля организатора мероприятия
     */
    fun handleNewApplication(
        applicationId: Long,
        eventId: Long,
        eventOwnerId: Long
    )

    /**
     * Обрабатывает изменение статуса заявки.
     * Отправляет уведомление подавшему заявку пользователю.
     *
     * @param applicationId идентификатор заявки
     * @param eventId идентификатор мероприятия
     * @param userId идентификатор профиля пользователя, подавшего заявку
     * @param newStatus новый статус заявки (строковое представление)
     */
    fun handleApplicationStatusChanged(
        applicationId: Long,
        eventId: Long,
        userId: Long,
        newStatus: String
    )

    /**
     * Уведомляет заявителя об обновлении его заявки.
     *
     * @param application обновленная заявка
     */
    fun notifyApplicantAboutApplicationUpdate(application: Application)

    /**
     * Уведомляет персонал мероприятия (организатора, соорганизаторов, рецензентов)
     * об обновлении заявки на участие.
     *
     * @param application обновленная заявка
     * @param initiator профиль пользователя, инициировавшего обновление
     */
    fun notifyEventStaffAboutApplicationUpdate(application: Application, initiator: Profile)

    /**
     * Уведомляет персонал мероприятия (организатора, соорганизаторов, рецензентов) 
     * об изменении или удалении мероприятия.
     *
     * @param event измененное или удаленное мероприятие
     * @param initiator профиль пользователя, инициировавшего изменение
     * @param isDelete true если мероприятие удалено, false если обновлено
     */
    fun notifyEventStaffAboutEventChange(event: Event, initiator: Profile, isDelete: Boolean = false)

    /**
     * Уведомляет всех заявителей об изменении или удалении мероприятия.
     *
     * @param event измененное или удаленное мероприятие
     * @param initiator профиль пользователя, инициировавшего изменение
     * @param isDelete true если мероприятие удалено, false если обновлено
     */
    fun notifyApplicantsAboutEventChange(event: Event, initiator: Profile, isDelete: Boolean = false)

    /**
     * Уведомляет персонал мероприятия об удалении заявки на участие.
     *
     * @param application удаленная заявка
     * @param initiator профиль пользователя, инициировавшего удаление
     */
    fun notifyEventStaffAboutApplicationDeletion(application: Application, initiator: Profile)
}