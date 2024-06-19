package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.event.data.model.DocRequired
import java.io.Serializable

data class DocRequiredResponse(
    val docRequiredId: Long,
    val type: String,
    val extension: String,
    val description: String,
    val mandatory: Boolean
) : Serializable {
    companion object {
        fun fromEntity(docRequired: DocRequired): DocRequiredResponse {
            return DocRequiredResponse(
                docRequiredId = docRequired.docRequiredId,
                type = docRequired.type,
                extension = docRequired.extension,
                description = docRequired.description,
                mandatory = docRequired.mandatory
            )
        }
    }
}