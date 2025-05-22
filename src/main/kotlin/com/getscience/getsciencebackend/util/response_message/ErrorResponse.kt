package com.getscience.getsciencebackend.util.response_message

data class ErrorResponse (
    val status: Int = 500,
    val error: String? = "Internal Error",
    val path: String? = null,
    val timestamp: String? = null
)