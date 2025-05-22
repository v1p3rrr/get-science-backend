package com.getscience.getsciencebackend.application.data.model

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.model.FileApplication
import com.getscience.getsciencebackend.user.data.model.Profile
import java.sql.Timestamp
import jakarta.persistence.*
import java.io.Serializable


@Entity
@Table(name = "application")
data class Application(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val applicationId: Long = 0,

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "eventId")
    val event: Event,

    @ManyToOne
    @JoinColumn(name = "profile_id", referencedColumnName = "profileId")
    val profile: Profile,

    @Column(nullable = false)
    val status: String,

    @Column(nullable = false)
    val submissionDate: Timestamp,

    @Column(nullable = true, columnDefinition = "TEXT")
    val message: String? = null,

    @Column(nullable = false)
    val isObserver: Boolean,

    @Column(nullable = true, columnDefinition = "TEXT")
    val verdict: String? = null,

    @OneToMany(mappedBy = "application", cascade = [CascadeType.ALL], orphanRemoval = true)
    var fileApplications: MutableList<FileApplication> = mutableListOf()
)  : Serializable {
    override fun hashCode(): Int {
        var result = applicationId.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + profile.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + submissionDate.hashCode()
        result = 31 * result + isObserver.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + verdict.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Application

        if (applicationId != other.applicationId) return false
        if (event != other.event) return false
        if (profile != other.profile) return false
        if (status != other.status) return false
        if (submissionDate != other.submissionDate) return false
        if (verdict != other.verdict) return false
        if (isObserver != other.isObserver) return false
        if (message != other.message) return false

        return true
    }

    override fun toString(): String {
        return "Application(applicationId=$applicationId, event=$event, profile=$profile, status='$status', message=$message, verdict='$verdict, submissionDate=$submissionDate, isObserver=$isObserver)"
    }
}