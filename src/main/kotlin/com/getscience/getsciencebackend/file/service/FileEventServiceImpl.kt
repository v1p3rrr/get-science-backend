package com.getscience.getsciencebackend.file.service

import com.getscience.getsciencebackend.event.data.model.Event
import com.getscience.getsciencebackend.event.repository.EventRepository
import com.getscience.getsciencebackend.file.data.dto.FileEventRequest
import com.getscience.getsciencebackend.file.data.dto.FileEventResponse
import com.getscience.getsciencebackend.file.data.model.FileEvent
import com.getscience.getsciencebackend.file.repository.FileEventRepository
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class FileEventServiceImpl(
    private val fileEventRepository: FileEventRepository,
    private val eventRepository: EventRepository
) : FileEventService {

    @CachePut(value = ["fileEvents"], key = "#result.id", condition = "#result != null")
    override fun createFileEvent(fileEventRequest: FileEventRequest, event: Event): FileEvent {
        val fileEvent = fileEventRequest.toEntity(event)
        return fileEventRepository.save(fileEvent)
    }

    @Cacheable(value = ["fileEventsByEvent"], key = "#eventId", condition = "#result != null")
    override fun getFilesByEvent(eventId: Long): List<FileEventResponse> {
        val fileEvents = fileEventRepository.findByEventEventId(eventId)
        return fileEvents.map { FileEventResponse.fromEntity(it) }
    }

    @Cacheable(value = ["fileEvents"], key = "#fileId", condition = "#result != null")
    override fun getFileEventById(fileId: Long): FileEventResponse {
        val fileEvent = fileEventRepository.findById(fileId).orElseThrow {
            RuntimeException("FileEvent not found")
        }
        return FileEventResponse.fromEntity(fileEvent)
    }
}
