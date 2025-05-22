package com.getscience.getsciencebackend.file.data.dto

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.event.data.dto.DocRequiredResponse
import com.getscience.getsciencebackend.file.data.model.FileApplication
import java.io.Serializable
import java.sql.Timestamp

data class FileApplicationResponse(
    val fileId: Long,
    val fileName: String,
    val filePath: String,
    val uploadDate: Timestamp,
    val fileType: String,
    val docRequired: DocRequiredResponse? = null,
    val isEncryptionEnabled: Boolean
) : Serializable {
    companion object {
        fun fromEntity(fileApplication: FileApplication): FileApplicationResponse {
            return FileApplicationResponse(
                fileId = fileApplication.fileId,
                fileName = fileApplication.fileName,
                filePath = fileApplication.filePath,
                uploadDate = fileApplication.uploadDate,
                fileType = fileApplication.fileType,
                docRequired = fileApplication.docRequired?.let { DocRequiredResponse.fromEntity(it) },
                isEncryptionEnabled = fileApplication.isEncryptionEnabled
            )
        }
    }

    fun toEntity(application: Application): FileApplication {
        return FileApplication(
            fileId = fileId,
            fileName = fileName,
            filePath = filePath,
            uploadDate = uploadDate,
            fileType = fileType,
            isEncryptionEnabled = isEncryptionEnabled,
            application = application
        )
    }
}