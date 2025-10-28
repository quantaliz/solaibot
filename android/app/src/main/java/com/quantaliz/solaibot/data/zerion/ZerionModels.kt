/*
 * Updates by Quantaliz PTY LTD, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.quantaliz.solaibot.data.zerion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Zerion API data models based on JSON:API specification.
 * See: https://developers.zerion.io/reference
 */

// ===== Portfolio Models =====

@Serializable
data class ZerionPortfolioResponse(
    val data: ZerionPortfolioData,
    val links: ZerionLinks? = null
)

@Serializable
data class ZerionPortfolioData(
    val id: String,
    val type: String,
    val attributes: ZerionPortfolioAttributes
)

@Serializable
data class ZerionPortfolioAttributes(
    @SerialName("total") val total: ZerionAssetValue,
    @SerialName("positions_distribution_by_type") val positionsDistributionByType: Map<String, ZerionAssetValue>? = null,
    @SerialName("positions_distribution_by_chain") val positionsDistributionByChain: Map<String, ZerionAssetValue>? = null
)

@Serializable
data class ZerionAssetValue(
    @SerialName("quantity") val quantity: String? = null,
    @SerialName("value") val value: Double? = null
)

// ===== Positions Models =====

@Serializable
data class ZerionPositionsResponse(
    val data: List<ZerionPositionData>,
    val links: ZerionLinks? = null,
    val included: List<ZerionIncludedResource>? = null
)

@Serializable
data class ZerionPositionData(
    val id: String,
    val type: String,
    val attributes: ZerionPositionAttributes,
    val relationships: ZerionPositionRelationships? = null
)

@Serializable
data class ZerionPositionAttributes(
    @SerialName("position_type") val positionType: String,
    @SerialName("quantity") val quantity: ZerionQuantity,
    @SerialName("value") val value: Double? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("changes") val changes: ZerionChanges? = null,
    @SerialName("fungible_info") val fungibleInfo: ZerionFungibleInfo? = null,
    @SerialName("flags") val flags: ZerionFlags? = null
)

@Serializable
data class ZerionQuantity(
    @SerialName("int") val int: String,
    @SerialName("decimals") val decimals: Int,
    @SerialName("float") val float: Double,
    @SerialName("numeric") val numeric: String
)

@Serializable
data class ZerionChanges(
    @SerialName("absolute_1d") val absolute1d: Double? = null,
    @SerialName("percent_1d") val percent1d: Double? = null
)

@Serializable
data class ZerionFungibleInfo(
    @SerialName("name") val name: String? = null,
    @SerialName("symbol") val symbol: String? = null,
    @SerialName("icon") val icon: ZerionIcon? = null,
    @SerialName("flags") val flags: ZerionFlags? = null,
    @SerialName("implementations") val implementations: List<ZerionImplementation>? = null
)

@Serializable
data class ZerionIcon(
    @SerialName("url") val url: String
)

@Serializable
data class ZerionFlags(
    @SerialName("verified") val verified: Boolean? = null,
    @SerialName("is_trash") val isTrash: Boolean? = null
)

@Serializable
data class ZerionImplementation(
    @SerialName("chain_id") val chainId: String,
    @SerialName("address") val address: String? = null,
    @SerialName("decimals") val decimals: Int
)

@Serializable
data class ZerionPositionRelationships(
    @SerialName("chain") val chain: ZerionRelationship? = null,
    @SerialName("fungible") val fungible: ZerionRelationship? = null
)

@Serializable
data class ZerionRelationship(
    @SerialName("data") val data: ZerionRelationshipData? = null,
    @SerialName("links") val links: ZerionRelationshipLinks? = null
)

@Serializable
data class ZerionRelationshipData(
    @SerialName("type") val type: String,
    @SerialName("id") val id: String
)

@Serializable
data class ZerionRelationshipLinks(
    @SerialName("related") val related: String? = null
)

// ===== Transactions Models =====

@Serializable
data class ZerionTransactionsResponse(
    val data: List<ZerionTransactionData>,
    val links: ZerionLinks? = null,
    val included: List<ZerionIncludedResource>? = null
)

@Serializable
data class ZerionTransactionData(
    val id: String,
    val type: String,
    val attributes: ZerionTransactionAttributes,
    val relationships: ZerionTransactionRelationships? = null
)

@Serializable
data class ZerionTransactionAttributes(
    @SerialName("operation_type") val operationType: String,
    @SerialName("hash") val hash: String,
    @SerialName("mined_at_block") val minedAtBlock: Long? = null,
    @SerialName("mined_at") val minedAt: String,
    @SerialName("sent_from") val sentFrom: String,
    @SerialName("sent_to") val sentTo: String,
    @SerialName("status") val status: String,
    @SerialName("nonce") val nonce: Int? = null,
    @SerialName("fee") val fee: ZerionFee? = null,
    @SerialName("transfers") val transfers: List<ZerionTransfer>? = null,
    @SerialName("approvals") val approvals: List<ZerionApproval>? = null,
    @SerialName("application_metadata") val applicationMetadata: ZerionApplicationMetadata? = null,
    @SerialName("flags") val flags: ZerionTransactionFlags? = null
)

@Serializable
data class ZerionFee(
    @SerialName("fungible_info") val fungibleInfo: ZerionFungibleInfo? = null,
    @SerialName("quantity") val quantity: ZerionQuantity,
    @SerialName("price") val price: Double? = null,
    @SerialName("value") val value: Double? = null
)

@Serializable
data class ZerionTransfer(
    @SerialName("fungible_info") val fungibleInfo: ZerionFungibleInfo? = null,
    @SerialName("nft_info") val nftInfo: ZerionNftInfo? = null,
    @SerialName("direction") val direction: String,
    @SerialName("quantity") val quantity: ZerionQuantity,
    @SerialName("value") val value: Double? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("sender") val sender: String? = null,
    @SerialName("recipient") val recipient: String? = null
)

@Serializable
data class ZerionNftInfo(
    @SerialName("name") val name: String? = null,
    @SerialName("interface") val nftInterface: String? = null,
    @SerialName("content") val content: ZerionNftContent? = null
)

@Serializable
data class ZerionNftContent(
    @SerialName("preview") val preview: ZerionIcon? = null,
    @SerialName("detail") val detail: ZerionIcon? = null
)

@Serializable
data class ZerionApproval(
    @SerialName("fungible_info") val fungibleInfo: ZerionFungibleInfo? = null,
    @SerialName("quantity") val quantity: ZerionQuantity? = null
)

@Serializable
data class ZerionApplicationMetadata(
    @SerialName("contract_address") val contractAddress: String? = null,
    @SerialName("method") val method: ZerionMethod? = null
)

@Serializable
data class ZerionMethod(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null
)

@Serializable
data class ZerionTransactionFlags(
    @SerialName("is_trash") val isTrash: Boolean? = null
)

@Serializable
data class ZerionTransactionRelationships(
    @SerialName("chain") val chain: ZerionRelationship? = null,
    @SerialName("dapp") val dapp: ZerionRelationship? = null
)

// ===== Common Models =====

@Serializable
data class ZerionLinks(
    @SerialName("self") val self: String? = null,
    @SerialName("next") val next: String? = null,
    @SerialName("prev") val prev: String? = null
)

@Serializable
data class ZerionIncludedResource(
    val id: String,
    val type: String,
    val attributes: Map<String, kotlinx.serialization.json.JsonElement>? = null
)

@Serializable
data class ZerionErrorResponse(
    val errors: List<ZerionError>
)

@Serializable
data class ZerionError(
    val status: String? = null,
    val title: String? = null,
    val detail: String? = null
)
