package com.getscience.getsciencebackend.event.data.dto

data class ReviewerDto(
    val profileId: Long?,
    val email: String,
    val firstName: String,
    val lastName: String
)