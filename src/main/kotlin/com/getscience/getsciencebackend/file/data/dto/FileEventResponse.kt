package com.getscience.getsciencebackend.file.data.dto

import com.getscience.getsciencebackend.file.data.model.FileEvent
import java.io.Serializable
import java.sql.Timestamp

data class FileEventResponse(
    val fileId: Long,
    val fileName: String,
    val filePath: String,
    val uploadDate: Timestamp,
    val fileType: String
) : Serializable {
    companion object {
        fun fromEntity(fileEvent: FileEvent): FileEventResponse {
            return FileEventResponse(
                fileId = fileEvent.fileId,
                fileName = fileEvent.fileName,
                filePath = fileEvent.filePath,
                uploadDate = fileEvent.uploadDate,
                fileType = fileEvent.fileType
            )
        }
    }
}