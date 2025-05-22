package com.getscience.getsciencebackend.file.data.dto

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.model.FileEvent
import java.sql.Timestamp

data class FileEventRequest(
    val fileId: Long? = null,
    val fileName: String,
    val filePath: String,
    val uploadDate: Timestamp,
    val fileType: String,
    val fileKindName: String,
    val category: String,
    val description: String
) {
    fun toEntity(event: Event): FileEvent {
        return FileEvent(
            fileId = fileId ?: 0,
            fileName = fileName,
            filePath = filePath,
            uploadDate = uploadDate,
            fileType = fileType,
            fileKindName = fileKindName,
            category = category,
            description = description,
            event = event
        )
    }
}