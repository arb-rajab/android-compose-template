package com.example.starterapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire types for the backend API. Keep field names identical to the server's JSON (snake_case)
 * and map them with [SerialName].
 */

@Serializable
data class ActionRequestDto(val payload: String)

@Serializable
data class ActionResponseDto(val id: String, @SerialName("created_at") val createdAt: String)
