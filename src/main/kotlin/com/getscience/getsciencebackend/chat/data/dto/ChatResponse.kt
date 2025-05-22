package com.getscience.getsciencebackend.chat.data.dto

import com.getscience.getsciencebackend.chat.data.model.Chat
import com.getscience.getsciencebackend.chat.data.model.ChatMessage
import java.util.*

data class ChatResponse(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val initiatorId: Long,
    val initiatorFirstName: String,
    val initiatorLastName: String,
    val participantIds: List<Long>,
    val lastMessage: ChatMessageResponse? = null,
    val lastMessageTimestamp: Date?,
    var unreadCount: Int = 0
) {
    companion object {
        fun fromEntity(chat: Chat, lastMessage: ChatMessage?, unreadCount: Int = 0, includeOnlyActiveParticipants: Boolean = false): ChatResponse {
            val participantsToInclude = if (includeOnlyActiveParticipants) {
                chat.participants.filter { it.isActive }
            } else {
                chat.participants
            }
            return ChatResponse(
                id = chat.id,
                eventId = chat.event.eventId,
                eventTitle = chat.event.title,
                initiatorId = chat.initiator.profileId,
                initiatorFirstName = chat.initiator.firstName,
                initiatorLastName = chat.initiator.lastName,
                participantIds = participantsToInclude.map { it.profile.profileId }.distinct(),
                lastMessage = lastMessage?.let { ChatMessageResponse.fromEntity(it) },
                lastMessageTimestamp = chat.lastMessageTimestamp,
                unreadCount = unreadCount
            )
        }
    }
}