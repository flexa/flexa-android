package com.flexa.core.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class BrandsResponse(
    val startingAfter: String? = null,
    val data: List<Brand>,
)

@Serializable
data class Brand(
    @SerialName("id")
    val id: String,
    @SerialName("category_name")
    val categoryName: String? = null,
    @SerialName("color")
    val color: String? = null,
    @SerialName("legacy_flexcodes")
    val legacyFlexcodes: List<LegacyFlexcode>? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("slug")
    val slug: String? = null,
    @SerialName("status")
    val status: String? = null,
)

@Serializable
data class LegacyFlexcode(
    @SerialName("amount")
    val amount: Amount? = null,
    @SerialName("asset")
    val asset: String? = null
) {
    @Serializable
    data class Amount(
        @SerialName("maximum")
        val maximum: String? = null,
        @SerialName("minimum")
        val minimum: String? = null
    )
}