package com.getscience.getsciencebackend.util.response_message

data class PageableResponse<T>(
    val content: List<T>,
    val hasNext: Boolean,
    val totalPages: Int,
    val totalElements: Long
)