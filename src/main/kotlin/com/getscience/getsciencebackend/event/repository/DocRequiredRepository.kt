package com.getscience.getsciencebackend.event.repository

import com.getscience.getsciencebackend.event.data.model.DocRequired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocRequiredRepository : JpaRepository<DocRequired, Long> {
    fun findByEventEventId(eventId: Long): List<DocRequired>
}
