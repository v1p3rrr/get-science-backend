package com.getscience.getsciencebackend.file.data.dto

import com.getscience.getsciencebackend.file.data.model.FileEvent
import com.getscience.getsciencebackend.file.service.S3Service
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.sql.Timestamp

data class FileEventResponse(
    val fileId: Long,
    val fileName: String,
    val filePath: String,
    val uploadDate: Timestamp,
    val fileType: String,
    val fileKindName: String,
    val category: String,
    val description: String
) : Serializable {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(FileEventResponse::class.java)

        fun fromEntity(fileEvent: FileEvent, s3Service: S3Service? = null): FileEventResponse {
            return FileEventResponse(
                fileId = fileEvent.fileId,
                fileName = fileEvent.fileName,
                filePath = fileEvent.filePath,
                uploadDate = fileEvent.uploadDate,
                fileType = fileEvent.fileType,
                fileKindName = fileEvent.fileKindName,
                category = fileEvent.category,
                description = fileEvent.description
            )
        }
    }
}