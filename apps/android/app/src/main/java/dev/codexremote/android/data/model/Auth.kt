package dev.codexremote.android.data.model

import kotlinx.serialization.Serializable

/** Auth API request / response types matching packages/shared. */
@Serializable
data class LoginRequest(
    val password: String,
    val deviceLabel: String? = null,
)

@Serializable
data class LoginResponse(
    val token: String,
    val expiresAt: String,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
data class ChangePasswordResponse(
    val ok: Boolean,
    val restartScheduled: Boolean,
)
