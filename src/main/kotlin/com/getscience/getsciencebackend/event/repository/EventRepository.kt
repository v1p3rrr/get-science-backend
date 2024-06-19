package com.getscience.getsciencebackend.event.repository

import com.getscience.getsciencebackend.event.data.model.Event
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : JpaRepository<Event, Long> {
    fun findByOrganizerProfileId(organizerId: Long): List<Event>
    fun findByTitleContainingIgnoreCase(title: String): List<Event>
    fun findByTypeOrThemeOrLocationOrFormatOrTitleContainsIgnoreCase(type: String?, theme: String?, location: String?, format: String?, title: String?): List<Event>
    fun findEventsByTheme(theme: String): List<Event>
}
