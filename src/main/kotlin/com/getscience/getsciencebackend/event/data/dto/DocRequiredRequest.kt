package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.event.data.model.DocRequired
import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.event.data.model.FileType

data class DocRequiredRequest(
    val type: String,
    val fileType: FileType,
    val description: String,
    val mandatory: Boolean
) {
    fun toEntity(event: Event): DocRequired {
        return DocRequired(
            type = type,
            fileType = fileType,
            description = description,
            mandatory = mandatory,
            event = event
        )
    }
}