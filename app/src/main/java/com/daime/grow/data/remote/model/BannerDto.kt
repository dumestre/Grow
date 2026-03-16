package com.daime.grow.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BannerDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean? = true,
    @SerialName("link_url") val linkUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
