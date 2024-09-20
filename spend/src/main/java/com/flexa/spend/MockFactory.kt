package com.flexa.spend

import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AssetKey
import com.flexa.core.entity.AssetValue
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.entity.CommerceSession
import com.flexa.core.entity.Notification
import com.flexa.core.entity.Quote
import com.flexa.core.shared.Asset
import com.flexa.core.shared.Brand
import com.flexa.core.shared.SelectedAsset
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

class MockFactory {
    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
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
                    key = AssetKey(
                        prefix = "123", secret = "321", length = 3
                    ),
                    value = AssetValue(
                        "", "", "\$68.43 Available"
                    )
                )
            )

        fun getLegacyCommerceSession(): CommerceSession {
            return json.decodeFromString<CommerceSession>(
                """{
    "id": "id",
    "api_version": "2024-06-11",
    "data": {
        "account": "acc",
        "amount": "5",
        "asset": "iso4217/USD",
        "brand": {
            "category_name": "",
            "color": "#8043FF",
            "created": 1672409955,
            "id": "id",
            "legacy_flexcodes": [
                {
                    "amount": {
                        "maximum": "100.00",
                        "minimum": "5.00"
                    },
                    "asset": "iso4217/USD"
                }
            ],
            "logo_url": "",
            "name": "Flexa",
            "slug": "flexa",
            "status": "active",
            "updated": 1716498844
        },
        "created": 1725375422,
        "debits": [
            {
                "amount": "5",
                "asset": "iso4217/USD",
                "created": 1725375422,
                "id": "id",
                "intent": "intent",
                "kind": "total",
                "label": "${'$'}5.00",
                "session": "session",
                "test_mode": true,
                "updated": 1725375422
            }
        ],
        "detected_location": {},
        "id": "id",
        "intent": "intent",
        "preferences": {
            "app": "flexa",
            "payment_asset": "eip155:11155111/slip44:60"
        },
        "rate": {
            "expires_at": 1725376322,
            "label": "1 ETH = ${'$'}2,460.63"
        },
        "status": "requires_transaction",
        "test_mode": true,
        "transactions": [
            {
                "amount": "0.002032",
                "asset": "eip155:11155111/slip44:60",
                "created": 1725375422,
                "destination": {
                    "address": "address",
                    "label": "label"
                },
                "expires_at": 1725379022,
                "fee": {
                    "amount": "0.0021",
                    "asset": "eip155:11155111/slip44:60",
                    "equivalent": "${'$'}5.17",
                    "label": "0.0021 ETH",
                    "price": {
                        "amount": "0.0000001",
                        "label": "100 gwei",
                        "priority": "0.000000001"
                    },
                    "zone": "very_high"
                },
                "id": "id",
                "label": "0.002032 ETH",
                "session": "session",
                "size": "21000",
                "status": "approved",
                "test_mode": true,
                "updated": 1725375428
            }
        ],
        "updated": 1725375422
    },
    "created": 1725375429,
    "type": "commerce_session.updated"
}""".trimIndent()
            )

        }

        fun getLegacyCommerceSessionCompleted(): CommerceSession {
            val cs = getLegacyCommerceSession()
            return cs.copy(
                data = cs.data?.copy(authorization = CommerceSession.Data.Authorization(
                    details = "", instructions = "Test Data", number = "12345"
                ))
            )
        }

        fun getMockCommerceSession(): CommerceSession {
            return json.decodeFromString<CommerceSession>(
                """
                {
    "id": "event_id",
    "api_version": "2024-06-11",
    "data": {
        "account": "acc",
        "amount": "0.1",
        "asset": "iso4217/USD",
        "brand": {
            "category_name": "",
            "color": "#8043FF",
            "created": 1672411062,
            "id": "id",
            "logo_url": "https://flexa.network/img/merchants/flexa-demo-store.png",
            "name": "Flexa Demo Store",
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
                "id": "id",
                "intent_id": "intent_id",
                "kind": "total",
                "label": "${'$'}0.10",
                "session_id": "session_id",
                "test_mode": true,
                "updated": 1721215290
            }
        ],
        "id": "id",
        "intent": "intent",
        "preferences": {
            "app": "flexa",
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
                    "address": "address",
                    "label": "label"
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
                "id": "id",
                "label": "0.1 CR",
                "session": "session",
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
    "id": "id",
    "api_version": "2024-06-11",
    "data": {
        "account": "acc",
        "amount": "0.1",
        "asset": "iso4217/USD",
        "brand": {
            "category_name": "",
            "color": "#8043FF",
            "created": 1672411062,
            "id": "id",
            "logo_url": "",
            "name": "Flexa",
            "slug": "flexa",
            "status": "active",
            "updated": 1716496094
        },
        "authorization": {
            "instructions": "Scan as **Gift** or **Store Credit**",
            "number": "number",
            "details": "PIN: 1234"
        },
        "created": 1721215290,
        "debits": [
            {
                "amount": "0.1",
                "asset": "iso4217/USD",
                "created": 1721215290,
                "id": "id",
                "intent_id": "intent_id",
                "kind": "total",
                "label": "${'$'}0.10",
                "session_id": "session_id",
                "test_mode": true,
                "updated": 1721215290
            }
        ],
        "id": "id",
        "intent": "intent",
        "preferences": {
            "app": "flexa",
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
                    "address": "address",
                    "label": "label"
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
                "id": "id",
                "label": "0.1 CR",
                "session": "session",
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
                            "id": "id",
                            "logo_url": "https://flexa.network/img/merchants/flexa-demo-store.png",
                            "name": "Flexa Demo Store",
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
            id = "id",
            title = "Get started with Flexa"
        )
    }
}
