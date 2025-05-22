package com.getscience.getsciencebackend.file.data.model

import com.getscience.getsciencebackend.application.data.model.Application
import com.getscience.getsciencebackend.event.data.model.DocRequired
import java.sql.Timestamp
import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "file_application")
data class FileApplication(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val fileId: Long = 0,

    @ManyToOne
    @JoinColumn(name = "application_id", referencedColumnName = "applicationId")
    val application: Application,

    @ManyToOne
    @JoinColumn(name = "doc_required_id", referencedColumnName = "docRequiredId")
    val docRequired: DocRequired? = null,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false)
    val filePath: String,

    @Column(nullable = false)
    val uploadDate: Timestamp,

    @Column(nullable = false)
    val fileType: String,

    @Column(nullable = false)
    val isEncryptionEnabled: Boolean

) : Serializable {
    override fun hashCode(): Int {
        var result = fileId.hashCode()
        result = 31 * result + application.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + uploadDate.hashCode()
        result = 31 * result + fileType.hashCode()
        result = 31 * result + isEncryptionEnabled.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileApplication

        if (fileId != other.fileId) return false
        if (application != other.application) return false
        if (fileName != other.fileName) return false
        if (filePath != other.filePath) return false
        if (uploadDate != other.uploadDate) return false
        if (fileType != other.fileType) return false
        if (isEncryptionEnabled != other.isEncryptionEnabled) return false

        return true
    }

    override fun toString(): String {
        return "FileApplication(fileId=$fileId, application=$application, fileName='$fileName', filePath='$filePath', uploadDate=$uploadDate, fileType='$fileType', isEncryptionEnabled=$isEncryptionEnabled)"
    }
}