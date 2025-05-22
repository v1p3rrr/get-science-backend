package com.getscience.getsciencebackend.application.data.dto

import java.io.Serializable

data class ApplicationFileMetadataDTO(
    val type: String,
    val isEncrypted: Boolean,
    val docRequiredId: Long
) : Serializable