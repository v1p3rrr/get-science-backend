package com.getscience.getsciencebackend.event.data.model

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "doc_required")
data class DocRequired(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val docRequiredId: Long = 0,

    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    val extension: String,

    @Column(nullable = false)
    val description: String,

    @Column(nullable = false)
    val mandatory: Boolean,

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "eventId")
    val event: Event
)  : Serializable {
    override fun hashCode(): Int {
        var result = docRequiredId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + extension.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + mandatory.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DocRequired

        if (docRequiredId != other.docRequiredId) return false
        if (type != other.type) return false
        if (extension != other.extension) return false
        if (description != other.description) return false
        if (mandatory != other.mandatory) return false

        return true
    }

    override fun toString(): String {
        return "DocRequired(docRequiredId=$docRequiredId, type='$type', extension='$extension', description='$description', mandatory=$mandatory)"
    }
}
