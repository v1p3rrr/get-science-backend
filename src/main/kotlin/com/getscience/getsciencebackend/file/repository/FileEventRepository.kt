package com.getscience.getsciencebackend.file.repository

import com.getscience.getsciencebackend.file.data.model.FileEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FileEventRepository : JpaRepository<FileEvent, Long> {
    fun findByEventEventId(eventId: Long): List<FileEvent>
}