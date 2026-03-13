package com.daime.grow.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class AppConfigDto(
    val key: String,
    val value_bool: Boolean
)
