package com.tv.streamapp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrandingConfig(
    @SerialName("appName")
    val appName: String? = null,
    @SerialName("logoUrl")
    val logoUrl: String? = null,
    @SerialName("bannerUrl")
    val bannerUrl: String? = null,
    @SerialName("backgroundUrl")
    val backgroundUrl: String? = null,
    @SerialName("primaryColor")
    val primaryColor: String? = null,
    @SerialName("welcomeMessage")
    val welcomeMessage: String? = null
)

@Serializable
data class PlaylistResponse(
    @SerialName("branding")
    val branding: BrandingConfig? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("categories")
    val categories: List<CategoryGroup> = emptyList()
)

@Serializable
data class CategoryGroup(
    @SerialName("name")
    val name: String,
    @SerialName("iconUrl")
    val iconUrl: String? = null,
    @SerialName("channels")
    val channels: List<ChannelRaw> = emptyList()
)

@Serializable
data class ChannelRaw(
    @SerialName("name")
    val name: String,
    @SerialName("url")
    val url: String,
    @SerialName("logo")
    val logo: String? = null,
    @SerialName("id")
    val id: String? = null
)

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String?,
    val category: String,
    val isFavorite: Boolean = false
) {
    companion object {
        fun fromRaw(raw: ChannelRaw, category: String, isFavorite: Boolean = false): Channel {
            val stableId = raw.id?.takeIf { it.isNotBlank() } ?: "${category}_${raw.name}_${raw.url.hashCode()}"
            return Channel(
                id = stableId,
                name = raw.name.trim(),
                url = raw.url.trim(),
                logo = raw.logo?.trim()?.takeIf { it.isNotBlank() },
                category = category.trim(),
                isFavorite = isFavorite
            )
        }
    }
}