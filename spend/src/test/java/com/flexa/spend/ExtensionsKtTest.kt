package com.flexa.spend

import com.flexa.core.entity.AppAccount
import com.flexa.core.entity.AssetKey
import com.flexa.core.entity.AvailableAsset
import com.flexa.core.shared.SelectedAsset
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsKtTest {

    val appAccounts = listOf(
        AppAccount(
            accountId = "123", availableAssets = arrayListOf(
                AvailableAsset(
                    assetId = "eip155:1/erc20:0x4d224452801ACEd8B2F0aebE155379bb5D594381",
                    balance = ".5",
                    key = AssetKey(prefix = "cxvxcv",secret = "123",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:42220/erc20:0x765DE816845861e75A25fCA122bb6898B8B1282a",
                    balance = ".5",
                    key = AssetKey(prefix = "ewr",secret = "456",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:1/erc20:0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    balance = ".5",
                    key = AssetKey(prefix = "ewr",secret = "789",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:1/erc20:0xdeFA4e8a7bcBA345F687a2f1456F5Edd9CE97202",
                    balance = ".5",
                    key = AssetKey(prefix = "ewr",secret = "101",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:1/erc20:0xaA7a9CA87d3694B5755f213B5D04094b8d0F0A6F",
                    balance = ".5",
                    key = AssetKey(prefix = "ewr",secret = "112",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:43113/slip44:9000\"",
                    balance = ".5",
                    livemode = false,
                    key = AssetKey(prefix = "qac,hjh",secret = "131",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:11155111/slip44:60",
                    balance = ".5",
                    livemode = false
                ),
            )
        ),
        AppAccount(
            accountId = "111", availableAssets = arrayListOf(
                AvailableAsset(
                    assetId = "eip155:1/erc20:0x4d224452801ACEd8B2F0aebE155379bb5D594381",
                    balance = ".5",
                    key = AssetKey(prefix = "cxvxcv",secret = "001",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:42220/erc20:0x765DE816845861e75A25fCA122bb6898B8B1282a",
                    balance = ".5",
                ),
                AvailableAsset(
                    assetId = "eip155:1/erc20:0xdAC17F958D2ee523a2206206994597C13D831ec7",
                    balance = ".5",
                    key = AssetKey(prefix = "ewr",secret = "002",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:1/erc20:0xdeFA4e8a7bcBA345F687a2f1456F5Edd9CE97202",
                    balance = ".5",
                ),
                AvailableAsset(
                    assetId = "eip155:1/erc20:0xaA7a9CA87d3694B5755f213B5D04094b8d0F0A6F",
                    balance = ".5",
                    key = AssetKey(prefix = "ewr",secret = "005",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:43113/slip44:9000\"",
                    balance = ".5",
                    livemode = false,
                    key = AssetKey(prefix = "qac,hjh",secret = "006",length = 5)
                ),
                AvailableAsset(
                    assetId = "eip155:11155111/slip44:60",
                    balance = ".5",
                    livemode = false
                ),
            )
        ),

    )

    @Test
    fun `pick asset with a key`() {
        val selectedAsset = SelectedAsset(
            appAccounts[1].accountId,
            appAccounts[1].availableAssets[2]
        )
        val res = appAccounts.getKey(selectedAsset)
        assertEquals("002", res?.secret)
    }

    @Test
    fun `should find the first asset with a key`() {
        val selectedAsset = SelectedAsset(
            appAccounts[1].accountId,
            appAccounts[1].availableAssets[1]
        )
        val res = appAccounts.getKey(selectedAsset)
        assertEquals("123", res?.secret)
    }

    @Test
    fun `should find the first asset with a key by livemode`() {
        val selectedAsset = SelectedAsset(
            appAccounts[1].accountId,
            appAccounts[1].availableAssets[6]
        )
        val res = appAccounts.getKey(selectedAsset)
        assertEquals("131", res?.secret)
    }

    @Test
    fun `should search into the right account`() {
        val selectedAsset = SelectedAsset(
            appAccounts[0].accountId,
            appAccounts[0].availableAssets[6]
        )
        val res = appAccounts.getKey(selectedAsset)
        assertEquals("131", res?.secret)
    }

    @Test
    fun `double from string`() {
        val str1 = "\$1201.17 available"
        val str2 = "\$1201 available"
        val res1 = str1.getAmount()
        val res2 = str2.getAmount()
        assertEquals(1201.17, res1, 0.0)
        assertEquals(1201.0, res2, 0.0)
    }
}
