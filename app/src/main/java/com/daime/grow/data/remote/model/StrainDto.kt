package com.daime.grow.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StrainDto(
    val id: String,
    val name: String,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val tipo: String? = null,
    val thc: String? = null,
    @SerialName("tempo_floracao")
    val tempoFloracao: String? = null,
    val altura: String? = null,
    val rendimento: String? = null,
    val descricao: String? = null,
    @SerialName("buy_links")
    val buyLinks: List<String>? = null,
    val ativo: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null
)

enum class StrainTipo(val displayName: String) {
    SATIVA("Sativa"),
    INDICA("Indica"),
    HYBRID("Híbrida")
}
