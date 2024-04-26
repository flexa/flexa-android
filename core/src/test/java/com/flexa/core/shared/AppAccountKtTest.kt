package com.flexa.core.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAccountKtTest {

    val appAccounts = listOf(
        AppAccount(
            accountId = "123",
            CustodyModel.LOCAL,
            availableAssets = listOf(
                AvailableAsset(
                    assetId = "eip155:1/erc20:0x4d224452801ACEd8B2F0aebE155379bb5D594381",
                    balance = .5
                ),
                AvailableAsset(
                    assetId = "no-name-coin",
                    balance = 23.71
                ),
                AvailableAsset(
                    assetId = "eip155:42220/erc20:0x765DE816845861e75A25fCA122bb6898B8B1282a",
                    balance = 23.71
                ),
            )
        ),
        AppAccount(
            accountId = "123",
            CustodyModel.LOCAL,
            availableAssets = listOf(
                AvailableAsset(
                    assetId = "eip155:1/erc20:0x4d224452801ACEd8B2F0aebE155379bb5D594381",
                    balance = .5
                ),
            )
        ),

    )
    val assets = listOf(
        Asset(id = "eip155:1/erc20:0x4d224452801ACEd8B2F0aebE155379bb5D594381"),
        Asset(id = "eip155:42220/erc20:0x765DE816845861e75A25fCA122bb6898B8B1282a"),
        Asset(id = "polkadot:91b171bb158e2d3848fa23a9f1c25182/slip44:354"),
    )

    @Test
    fun filterAssets() {
        val accounts = appAccounts.filterAssets(assets)
        val accountAssets = accounts.first().availableAssets
        assertEquals(2, accountAssets.size)
        assertTrue(accountAssets.any { it.assetId ==  "eip155:1/erc20:0x4d224452801ACEd8B2F0aebE155379bb5D594381"})
        assertTrue(accountAssets.any { it.assetId ==  "eip155:42220/erc20:0x765DE816845861e75A25fCA122bb6898B8B1282a"})
    }

    @Test
    fun distinctAppAccountsById() {
        val accounts = appAccounts.filterAssets(assets)
        assertEquals(1, accounts.size)
        assertEquals(2, accounts.first().availableAssets.size)
    }
}