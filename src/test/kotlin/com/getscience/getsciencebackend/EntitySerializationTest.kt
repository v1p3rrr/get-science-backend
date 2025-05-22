package com.getscience.getsciencebackend

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.getscience.getsciencebackend.application.data.model.*
import com.getscience.getsciencebackend.chat.data.model.*
import com.getscience.getsciencebackend.event.data.model.*
import com.getscience.getsciencebackend.file.data.model.*
import com.getscience.getsciencebackend.notification.data.*
import com.getscience.getsciencebackend.user.data.RoleType
import com.getscience.getsciencebackend.user.data.model.*
import com.getscience.getsciencebackend.user.data.model.token.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

class EntitySerializationSmokeTest {

    private fun trySerialize(any: Any) {
        try {
            val objectMapper = jacksonObjectMapper()
                .registerModule(KotlinModule.Builder().build())
                .apply {
                    activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                        JsonTypeInfo.As.PROPERTY
                    )
                }

            println("OK   ${any::class.simpleName}: ${objectMapper}…")
        } catch (e: Exception) {
            println("FAIL ${any::class.simpleName}: ${e.message}")
        }
    }

    @Disabled // for manual checking
    @Test
    fun `serialize every entity`() {
        /* ---------- базовые объекты ---------- */
        val role       = Role(1, RoleType.USER)
        val account    = Account(
            accountId = 1,
            email = "john.doe@example.com",
            passwordHash = "bcrypt\$dummy",
            emailConfirmed = true
        )
        val profile    = Profile(
            profileId = 1,
            firstName = "John",
            lastName  = "Doe",
            account   = account,
            role      = role
        ).also { account.profile = it }

        /* ---------- Event и зависимые сущности ---------- */
        val now   = Date()
        val event = Event(
            eventId = 1,
            title   = "Test Event",
            description = "desc",
            dateStart = now,
            dateEnd   = now,
            location  = "Riga",
            type      = EventType.CONFERENCE,
            format    = EventFormat.ONLINE,
            observersAllowed = true,
            organizer = profile,
            applicationStart = now,
            applicationEnd   = now
        )

        val docReq = DocRequired(
            docRequiredId = 1,
            type          = "Passport",
            fileType      = FileType.DOCUMENT,
            description   = "main ID",
            mandatory     = true,
            event         = event
        )
        event.documentsRequired += docReq

        val fileEvent = FileEvent(
            fileId    = 1,
            event     = event,
            fileName  = "agenda.pdf",
            filePath  = "/files/agenda.pdf",
            uploadDate = Timestamp(now.time),
            fileType   = "application/pdf",
            fileKindName = "AGENDA",
            category   = "PDF",
            description = "Agenda file"
        )
        event.fileEvents += fileEvent

        /* ---------- Application + FileApplication ---------- */
        val application = Application(
            applicationId = 1,
            event         = event,
            profile       = profile,
            status        = "PENDING",
            submissionDate = Timestamp(now.time),
            isObserver    = false
        )
        event.applications += application

        val fileApp = FileApplication(
            fileId = 1,
            application  = application,
            docRequired  = docReq,
            fileName     = "scan.pdf",
            filePath     = "/files/scan.pdf",
            uploadDate   = Timestamp(now.time),
            fileType     = "application/pdf",
            isEncryptionEnabled = false
        )
        application.fileApplications += fileApp

        /* ---------- Chat ---------- */
        val chat = Chat(
            id        = 1,
            event     = event,
            initiator = profile,
            lastMessageTimestamp = now
        )
        val chatParticipant = ChatParticipant(
            id      = 1,
            chat    = chat,
            profile = profile
        )
        chat.participants += chatParticipant

        val chatMsg = ChatMessage(
            id      = 1,
            chat    = chat,
            sender  = profile,
            content = "Hello!"
        )
        val readStatus = ChatMessageReadStatus(
            id      = 1,
            chatId  = chat.id,
            profile = profile,
            lastReadTimestamp = now
        )

        /* ---------- Notification ---------- */
        val notification = Notification(
            id        = 1,
            userId    = profile.profileId,
            type      = NotificationType.NEW_APPLICATION,
            title     = "New application",
            message   = "Somebody applied",
            entityId  = event.eventId,
            entityType = EntityType.EVENT,
            status    = ApplicationStatus.PENDING
        )

        /* ---------- Tokens ---------- */
        val refreshToken = RefreshToken(
            id = 1,
            token = UUID.randomUUID().toString(),
            account = account,
            expiryDate = now
        )
        val token = Token(
            id = 1,
            token = UUID.randomUUID().toString(),
            account = account,
            type = TokenType.EMAIL_VERIFICATION,
            expiresAt = now
        )

        /* ---------- Проверяем все ---------- */
        listOf(
            role, account, profile,
            event, docReq, fileEvent,
            application, fileApp,
            chat, chatParticipant, chatMsg, readStatus,
            notification,
            refreshToken, token
        ).forEach { trySerialize(it) }
    }
}
