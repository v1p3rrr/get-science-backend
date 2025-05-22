package com.getscience.getsciencebackend.user.data.dto

data class JwtResponse(
    val accessToken: String,
    val refreshToken: String
)
