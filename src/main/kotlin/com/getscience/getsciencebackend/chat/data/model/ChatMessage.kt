package com.getscience.getsciencebackend.chat.data.model

import com.getscience.getsciencebackend.user.data.model.Profile
import jakarta.persistence.*
import java.io.Serializable
import java.util.Date

@Entity
@Table(name = "chat_message")
data class ChatMessage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    val chat: Chat,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: Profile,

    @Column(columnDefinition = "TEXT", nullable = false)
    val content: String,

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    val timestamp: Date = Date()
) : Serializable {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChatMessage
        return id == other.id
    }

    override fun toString(): String {
        return "ChatMessage(id=$id, chatId=${chat.id}, senderId=${sender.profileId}, timestamp=$timestamp)"
    }
} 