package com.getscience.getsciencebackend.user.data.dto

import com.getscience.getsciencebackend.user.data.RoleTypes

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val role: RoleTypes,
    val email: String,
    val password: String
)