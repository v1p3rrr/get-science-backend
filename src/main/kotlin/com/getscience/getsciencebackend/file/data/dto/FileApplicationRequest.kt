package com.getscience.getsciencebackend.file.data.dto

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.file.data.model.FileApplication
import java.sql.Timestamp

data class FileApplicationRequest(
    val fileName: String,
    val filePath: String,
    val uploadDate: Timestamp,
    val fileType: String,
    val isEncryptionEnabled: Boolean
) {
    fun toEntity(application: Application): FileApplication {
        return FileApplication(
            fileName = fileName,
            filePath = filePath,
            uploadDate = uploadDate,
            fileType = fileType,
            isEncryptionEnabled = isEncryptionEnabled,
            application = application
        )
    }
}