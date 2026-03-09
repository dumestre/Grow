package com.daime.grow.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class MuralUserDto(
    val id: String? = null,
    val username: String,
    val created_at: String? = null
)

@Serializable
data class MuralPostDto(
    val id: String? = null,
    val user_id: String,
    val plant_name: String,
    val strain: String?,
    val stage: String?,
    val medium: String?,
    val days: Int?,
    val photo_url: String?,
    val created_at: String? = null
)

@Serializable
data class MuralCommentDto(
    val id: String? = null,
    val post_id: String,
    val user_id: String,
    val parent_id: String? = null,
    val content: String,
    val created_at: String? = null
)

@Serializable
data class MuralLikeDto(
    val id: String? = null,
    val post_id: String,
    val user_id: String
)
