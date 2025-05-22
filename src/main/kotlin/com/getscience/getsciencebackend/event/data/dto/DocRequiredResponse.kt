package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.event.data.model.DocRequired
import com.getscience.getsciencebackend.event.data.model.FileType
import java.io.Serializable

data class DocRequiredResponse(
    val docRequiredId: Long,
    val type: String,
    val fileType: FileType,
    val description: String,
    val mandatory: Boolean
) : Serializable {
    companion object {
        fun fromEntity(docRequired: DocRequired): DocRequiredResponse {
            return DocRequiredResponse(
                docRequiredId = docRequired.docRequiredId,
                type = docRequired.type,
                fileType = docRequired.fileType,
                description = docRequired.description,
                mandatory = docRequired.mandatory
            )
        }
    }
}