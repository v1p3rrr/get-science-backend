package com.getscience.getsciencebackend.chat.data.dto

import com.getscience.getsciencebackend.chat.data.model.ChatMessage
import java.util.*

data class ChatMessageResponse(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderFirstName: String,
    val senderLastName: String,
    val content: String,
    val timestamp: Date
) {
    companion object {
        fun fromEntity(chatMessage: ChatMessage): ChatMessageResponse {
            return ChatMessageResponse(
                id = chatMessage.id,
                chatId = chatMessage.chat.id,
                senderId = chatMessage.sender.profileId,
                senderFirstName = chatMessage.sender.firstName,
                senderLastName = chatMessage.sender.lastName,
                content = chatMessage.content,
                timestamp = chatMessage.timestamp
            )
        }
    }
}