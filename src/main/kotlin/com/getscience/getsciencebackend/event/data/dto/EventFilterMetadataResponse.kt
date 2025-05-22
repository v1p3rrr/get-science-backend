package com.getscience.getsciencebackend.event.data.dto

import com.getscience.getsciencebackend.event.data.model.EventFormat
import com.getscience.getsciencebackend.event.data.model.EventType
import java.io.Serializable

/**
 * DTO с метаданными для построения фильтров событий
 */
data class EventFilterMetadataResponse(
    val themes: List<String>,
    val types: List<EventType>,
    val formats: List<EventFormat>,
    val locations: List<String>
) : Serializable 