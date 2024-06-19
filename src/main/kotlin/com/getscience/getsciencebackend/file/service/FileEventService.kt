package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.data.model.FileEvent

interface FileEventService {
    fun createFileEvent(fileEventRequest: FileEventRequest, event: Event): FileEvent
    fun getFilesByEvent(eventId: Long): List<FileEventResponse>
    fun getFileEventById(fileId: Long): FileEventResponse
}