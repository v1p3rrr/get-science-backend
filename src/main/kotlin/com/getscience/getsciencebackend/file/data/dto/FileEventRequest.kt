package com.getscience.getsciencebackend.file.data.dto

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.model.FileEvent
import java.sql.Timestamp

data class FileEventRequest(
    val fileName: String,
    val filePath: String,
    val uploadDate: Timestamp,
    val fileType: String
) {
    fun toEntity(event: Event): FileEvent {
        return FileEvent(
            fileName = fileName,
            filePath = filePath,
            uploadDate = uploadDate,
            fileType = fileType,
            event = event
        )
    }
}