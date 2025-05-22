package com.getscience.getsciencebackend.event.data.dto

import java.time.LocalDateTime

/**
 * Enum для статуса мероприятия относительно текущей даты
 */
enum class LiveStatus {
    NOT_STARTED, // Еще не началось
    IN_PROGRESS, // Идет сейчас
    COMPLETED    // Завершено
}

/**
 * DTO для запроса на фильтрацию мероприятий
 */
data class EventFilterRequest(
    val types: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val title: String? = null,
    val observersAllowed: Boolean? = null,
    val liveStatus: List<LiveStatus> = emptyList(),
    val isApplicationAvailable: Boolean? = null,
    val dateFrom: LocalDateTime? = null,
    val dateTo: LocalDateTime? = null,
    val page: Int = 0,
    val size: Int = 12
) 