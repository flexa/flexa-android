package com.flexa.spend

import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AssetValue
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.entity.CommerceSession
import com.flexa.core.entity.Notification
import com.flexa.core.entity.Quote
import com.flexa.core.shared.Asset
import com.flexa.core.shared.Brand
import com.flexa.core.shared.SelectedAsset
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

class MockFactory {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun getMockConfig(): List<AppAccount> =
            listOf(
                AppAccount(
                    accountId = UUID.randomUUID().toString(),
                    displayName = "Example Wallet",
                    icon = "https://flexa.network/static/4bbb1733b3ef41240ca0f0675502c4f7/d8419/flexa-logo%403x.png",
                    availableAssets = arrayListOf(
                        AvailableAsset(
                            assetId = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp/slip44:501",
                            balance = "0.5",
                        ),
                        AvailableAsset(
                            assetId = "eip155:1/slip44:60",
                            balance = "0.025",
                        ),
                    )
                )
            )

        fun getMockQuote(): Quote {
            val feePrice = String.format("%.2f", Random.nextDouble(0.50, 0.70))
            val price = String.format("%.2f", Random.nextDouble(2516.0, 2523.0))
            val expireTime =
                Instant.now().plusSeconds(Random.nextLong(5, 10)).toEpochMilli() / 1000
            return json.decodeFromString<Quote>(
                """
                {
                    "amount": "0.141288845675685478",
                    "asset": "eip155:1/slip44:60",
                    "fee": {
                        "amount": "0.00003153",
                        "asset": "eip155:1/slip44:60",
                        "equivalent": "${'$'}$feePrice",
                        "label": "0.000032 ETH",
                        "price": {
                            "amount": "0.000000007",
                            "label": "6–8 gwei",
                            "priority": "0.00000000119"
                        },
                        "zone": "low"
                    },
                    "label": "0.141288 ETH",
                    "unit_of_account": "iso4217/USD",
                    "value": {
                        "amount": "251.48",
                        "label": "${'$'}251.48",
                        "rate": {
                            "label": "1 ETH = ${'$'}$price",
                            "expires_at": "$expireTime"
                        }
                    }
                }
                """.trimIndent()
            )

        }

        fun getMockSelectedAsset(): SelectedAsset =
            SelectedAsset(
                accountId = "1",
                asset = AvailableAsset(
                    assetId = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp/slip44:501",
                    balance = "0.5",
                    label = "0.5 SOL",
                    assetData = Asset(
                        id = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp/slip44:501",
                        displayName = "SOL"
                    ),
                    value = AssetValue(
                        "", "", "\$68.43 Available"
                    )
                )
            )

        fun getMockCommerceSession(): CommerceSession {
            return json.decodeFromString<CommerceSession>(
                """
                {
    "id": "event_vXJG4w8cFmV2QGh5GR2f4",
    "object": "event",
    "api_version": "2024-06-11",
    "data": {
        "account": "acct_9jqfmxR4c8rPfGjHC4Vgg",
        "amount": "0.1",
        "asset": "iso4217/USD",
        "brand": {
            "category_name": "",
            "color": "#8043FF",
            "created": 1672411062,
            "id": "brand_MQPXwmr56gcVwqh94Pjmc",
            "logo_url": "https://flexa.network/img/merchants/flexa-demo-store.png",
            "name": "Flexa Demo Store",
            "object": "brand",
            "slug": "flexa-demo-store",
            "status": "active",
            "updated": 1716496094
        },
        "created": 1721215290,
        "debits": [
            {
                "amount": "0.1",
                "asset": "iso4217/USD",
                "created": 1721215290,
                "id": "debit_4MM883rW6H23rmqXwQmWm",
                "intent_id": "ci_mwJQ8GvHFMvrV3FVM3C6P",
                "kind": "total",
                "label": "${'$'}0.10",
                "object": "debit",
                "session_id": "cs_GQVvQmxWQvjMPp2wcJpgm",
                "test_mode": true,
                "updated": 1721215290
            }
        ],
        "id": "cs_GQVvQmxWQvjMPp2wcJpgm",
        "intent": "ci_mwJQ8GvHFMvrV3FVM3C6P",
        "object": "commerce_session",
        "preferences": {
            "app": "flexa-playground",
            "payment_asset": "eip155:1/erc20:0xe7ae9b78373d0D54BAC81a85525826Fd50a1E2d3"
        },
        "rate": {
            "expires_at": -62135596800,
            "label": "1 CR = ${'$'}1.00"
        },
        "status": "pending",
        "test_mode": true,
        "transactions": [
            {
                "amount": "0.1",
                "asset": "eip155:1/erc20:0xe7ae9b78373d0D54BAC81a85525826Fd50a1E2d3",
                "created": 1721215290,
                "destination": {
                    "address": "eip155:1:0xaD12Eeb636086C446C5626320F6e001b92eB255d",
                    "label": "0xaD12Eeb636086C446C5626320F6e001b92eB255d"
                },
                "expires_at": 1721218890,
                "fee": {
                    "amount": "0.003",
                    "asset": "eip155:1/slip44:60",
                    "equivalent": "${'$'}10.40",
                    "label": "0.003 ETH",
                    "price": {
                        "amount": "0.000000015",
                        "label": "<1 gwei",
                        "priority": "0.000000004464273208"
                    },
                    "zone": "medium"
                },
                "id": "tx_R58PVfr6h7gvq3hfjPg4R",
                "label": "0.1 CR",
                "object": "transaction",
                "session": "cs_GQVvQmxWQvjMPp2wcJpgm",
                "size": "200000",
                "status": "requested",
                "test_mode": true,
                "updated": 1721215290
            }
        ],
        "updated": 1721215290
    },
    "created": 1721215292,
    "type": "commerce_session.updated"
}
                """.trimIndent()
            )
        }

        fun getMockCommerceSessionCompleted(): CommerceSession {
            return json.decodeFromString<CommerceSession>(
                """
                {
    "id": "event_vXJG4w8cFmV2QGh5GR2f4",
    "object": "event",
    "api_version": "2024-06-11",
    "data": {
        "account": "acct_9jqfmxR4c8rPfGjHC4Vgg",
        "amount": "0.1",
        "asset": "iso4217/USD",
        "brand": {
            "category_name": "",
            "color": "#8043FF",
            "created": 1672411062,
            "id": "brand_MQPXwmr56gcVwqh94Pjmc",
            "logo_url": "https://flexa.network/img/merchants/flexa-demo-store.png",
            "name": "Flexa Demo Store",
            "object": "brand",
            "slug": "flexa-demo-store",
            "status": "active",
            "updated": 1716496094
        },
        "authorization": {
            "instructions": "Scan as **Gift** or **Store Credit**",
            "number": "6012345678901234",
            "details": "PIN: 1234"
        },
        "created": 1721215290,
        "debits": [
            {
                "amount": "0.1",
                "asset": "iso4217/USD",
                "created": 1721215290,
                "id": "debit_4MM883rW6H23rmqXwQmWm",
                "intent_id": "ci_mwJQ8GvHFMvrV3FVM3C6P",
                "kind": "total",
                "label": "${'$'}0.10",
                "object": "debit",
                "session_id": "cs_GQVvQmxWQvjMPp2wcJpgm",
                "test_mode": true,
                "updated": 1721215290
            }
        ],
        "id": "cs_GQVvQmxWQvjMPp2wcJpgm",
        "intent": "ci_mwJQ8GvHFMvrV3FVM3C6P",
        "object": "commerce_session",
        "preferences": {
            "app": "flexa-playground",
            "payment_asset": "eip155:1/erc20:0xe7ae9b78373d0D54BAC81a85525826Fd50a1E2d3"
        },
        "rate": {
            "expires_at": -62135596800,
            "label": "1 CR = ${'$'}1.00"
        },
        "status": "pending",
        "test_mode": true,
        "transactions": [
            {
                "amount": "0.1",
                "asset": "eip155:1/erc20:0xe7ae9b78373d0D54BAC81a85525826Fd50a1E2d3",
                "created": 1721215290,
                "destination": {
                    "address": "eip155:1:0xaD12Eeb636086C446C5626320F6e001b92eB255d",
                    "label": "0xaD12Eeb636086C446C5626320F6e001b92eB255d"
                },
                "expires_at": 1721218890,
                "fee": {
                    "amount": "0.003",
                    "asset": "eip155:1/slip44:60",
                    "equivalent": "${'$'}10.40",
                    "label": "0.003 ETH",
                    "price": {
                        "amount": "0.000000015",
                        "label": "<1 gwei",
                        "priority": "0.000000004464273208"
                    },
                    "zone": "medium"
                },
                "id": "tx_R58PVfr6h7gvq3hfjPg4R",
                "label": "0.1 CR",
                "object": "transaction",
                "session": "cs_GQVvQmxWQvjMPp2wcJpgm",
                "size": "200000",
                "status": "requested",
                "test_mode": true,
                "updated": 1721215290
            }
        ],
        "updated": 1721215290
    },
    "created": 1721215292,
    "type": "commerce_session.updated"
}
                """.trimIndent()
            )
        }

        fun getMockBrand(): Brand {
            return json.decodeFromString<Brand>(
                """
                    {
                            "category_name": "",
                            "color": "#8043FF",
                            "created": 1672411062,
                            "id": "brand_MQPXwmr56gcVwqh94Pjmc",
                            "logo_url": "https://flexa.network/img/merchants/flexa-demo-store.png",
                            "name": "Flexa Demo Store",
                            "object": "brand",
                            "slug": "flexa-demo-store",
                            "status": "active",
                            "updated": 1716496094
                        }
                """.trimIndent()
            )
        }

        fun getAppNotification(): Notification = Notification(
            action = Notification.Action(
                label = "Learn How to Pay",
                url = "https://app.flexa.link/flexa/how-to-pay"
            ),
            body = "Pay your favorite places directly from your wallet—no card required.",
            iconUrl = "https://flexa.media/icons/flexa.svg",
            id = "appnotif_mPJJfPmc33xXhQ4G9Wgj2",
            objectX = "app_notification",
            title = "Get started with Flexa"
        )
    }
}
