package com.getscience.getsciencebackend.chat.data.model

import com.getscience.getsciencebackend.user.data.model.Profile
import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(
    name = "chat_participant",
    uniqueConstraints = [UniqueConstraint(columnNames = ["chat_id", "profile_id"])]
)
data class ChatParticipant(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    val chat: Chat,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
) : Serializable {
    override fun hashCode(): Int {
        var result = chat.id.hashCode()
        result = 31 * result + profile.profileId.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChatParticipant
        return chat.id == other.chat.id && profile.profileId == other.profile.profileId
    }

    override fun toString(): String {
        return "ChatParticipant(id=$id, chatId=${chat.id}, profileId=${profile.profileId})"
    }
} 