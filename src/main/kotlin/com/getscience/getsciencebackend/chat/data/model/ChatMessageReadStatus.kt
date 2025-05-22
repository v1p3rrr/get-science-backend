package com.getscience.getsciencebackend.chat.data.model

import com.getscience.getsciencebackend.user.data.model.Profile
import jakarta.persistence.*
import java.io.Serializable
import java.util.Date

@Entity
@Table(
    name = "chat_message_read_status",
    uniqueConstraints = [UniqueConstraint(columnNames = ["chat_id", "profile_id"])]
)
data class ChatMessageReadStatus(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "chat_id", nullable = false)
    val chatId: Long, // Не используем прямую связь @ManyToOne<Chat>, чтобы избежать циклов и упростить

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_read_timestamp", nullable = false)
    var lastReadTimestamp: Date
) : Serializable {
    override fun hashCode(): Int {
        var result = chatId.hashCode()
        result = 31 * result + profile.profileId.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChatMessageReadStatus
        return chatId == other.chatId && profile.profileId == other.profile.profileId
    }

    override fun toString(): String {
        return "ChatMessageReadStatus(id=$id, chatId=$chatId, profileId=${profile.profileId}, lastReadTimestamp=$lastReadTimestamp)"
    }
} 