package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.event.data.model.DocRequired
import com.getscience.getsciencebackend.event.data.model.Event

data class DocRequiredRequest(
    val type: String,
    val extension: String,
    val description: String,
    val mandatory: Boolean
) {
    fun toEntity(event: Event): DocRequired {
        return DocRequired(
            type = type,
            extension = extension,
            description = description,
            mandatory = mandatory,
            event = event
        )
    }
}