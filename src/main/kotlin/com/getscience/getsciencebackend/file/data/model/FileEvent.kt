package com.getscience.getsciencebackend.file.data.model

import com.getscience.getsciencebackend.event.data.model.Event
import java.sql.Timestamp
import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "file_event")
data class FileEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val fileId: Long = 0,

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "eventId")
    val event: Event,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false)
    val filePath: String,

    @Column(nullable = false)
    val uploadDate: Timestamp,

    @Column(nullable = false)
    val fileType: String
) : Serializable {
    override fun hashCode(): Int {
        var result = fileId.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + uploadDate.hashCode()
        result = 31 * result + fileType.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileEvent

        if (fileId != other.fileId) return false
        if (event != other.event) return false
        if (fileName != other.fileName) return false
        if (filePath != other.filePath) return false
        if (uploadDate != other.uploadDate) return false
        if (fileType != other.fileType) return false

        return true
    }

    override fun toString(): String {
        return "FileEvent(fileId=$fileId, event=$event, fileName='$fileName', filePath='$filePath', uploadDate=$uploadDate, fileType='$fileType')"
    }
}