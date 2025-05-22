package com.getscience.getsciencebackend.user.data.dto

import com.getscience.getsciencebackend.user.data.RoleType
import jakarta.validation.constraints.*


data class RegisterRequest(
    @field:NotBlank
    val firstName: String,
    @field:NotBlank
    val lastName: String,
    @field:NotNull
    val role: RoleType,
    @field:Email
    @field:NotBlank
    val email: String,
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String
)