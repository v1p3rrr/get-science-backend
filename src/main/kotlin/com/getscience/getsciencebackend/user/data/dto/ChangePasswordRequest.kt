package com.getscience.getsciencebackend.user.data.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank
    val currentPassword: String,
    @field:NotBlank
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val newPassword: String
)
