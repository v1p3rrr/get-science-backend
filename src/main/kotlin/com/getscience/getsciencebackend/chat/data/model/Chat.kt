package com.getscience.getsciencebackend.chat.data.model

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.user.data.model.Profile
import jakarta.persistence.*
import java.io.Serializable
import java.util.Date

@Entity
@Table(name = "chat")
data class Chat(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "eventId", unique = false)
    val event: Event,

    @ManyToOne
    @JoinColumn(name = "initiator_id", referencedColumnName = "profileId")
    val initiator: Profile,

    @OneToMany(mappedBy = "chat", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val participants: MutableSet<ChatParticipant> = mutableSetOf(),

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_message_timestamp")
    var lastMessageTimestamp: Date? = null
) : Serializable {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Chat
        return id == other.id
    }

    override fun toString(): String {
        return "Chat(id=$id, eventId=${event.eventId}, initiatorId=${initiator.profileId}, lastMessageTimestamp=$lastMessageTimestamp)"
    }
} 