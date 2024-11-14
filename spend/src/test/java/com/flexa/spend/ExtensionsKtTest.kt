package com.flexa.spend

import com.flexa.core.entity.BalanceBundle
import com.flexa.core.entity.CommerceSession
import com.flexa.core.entity.ExchangeRate
import com.flexa.core.shared.Promotion
import com.flexa.core.toBalanceBundle
import junit.framework.TestCase
import junit.framework.TestCase.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class ExtensionsKtTest {

    @Test
    fun `double from string`() {
        val str1 = "\$1201.17 available"
        val str2 = "\$1201 available"
        val res1 = str1.getAmount()
        val res2 = str2.getAmount()
        assertEquals(1201.17, res1.toDouble(), 0.0)
        assertEquals(1201.0, res2.toDouble(), 0.0)
    }

    @Test
    fun toBalanceBundle() {
        val exchangeRate = ExchangeRate(
            asset = "eip155:1/slip44:60",
            expiresAt = 1727433147,
            label = "\$2,538.39",
            precision = 6,
            price = "2538.39",
            unitOfAccount = "iso4217/USD"
        )
        val asset = com.flexa.core.shared.AvailableAsset(
            assetId = "eip155:1/slip44:60",
            balance = 0.141288818710019324,
        )
        val balanceBundle = exchangeRate.toBalanceBundle(asset)

        TestCase.assertEquals(358.64, balanceBundle?.total?.toDouble())
        TestCase.assertEquals("\$358.64", balanceBundle?.totalLabel)
        TestCase.assertNull(balanceBundle?.available)
        TestCase.assertNull(balanceBundle?.availableLabel)
    }

    @Test
    fun toBalanceBundleFaultTolerance() {
        val exchangeRate = ExchangeRate(
            asset = "eip155:1/slip44:60",
            expiresAt = 1727433147,
            label = "\$2,538.39",
            precision = 6,
            unitOfAccount = "iso4217/USD"
        )
        val asset = com.flexa.core.shared.AvailableAsset(
            assetId = "eip155:1/slip44:60",
            balance = 0.141288818710019324,
        )
        val balanceBundle = exchangeRate.toBalanceBundle(asset)

        TestCase.assertEquals(0.0, balanceBundle?.total?.toDouble())
        TestCase.assertEquals("\$0.00", balanceBundle?.totalLabel)
        TestCase.assertNull(balanceBundle?.available)
        TestCase.assertNull(balanceBundle?.availableLabel)
    }

    @Test
    fun `balance restrictions recognition`() {
        val assetLess = MockFactory.getMockSelectedAsset().asset.copy(
            balanceAvailable = 1.0,
            balanceBundle = MockFactory.getBalanceBundle()
        )
        assertTrue(assetLess.hasBalanceRestrictions())

        val assetEquals = MockFactory.getMockSelectedAsset().asset.copy(
            balanceAvailable = 1.0,
            balanceBundle = BalanceBundle(
                total = BigDecimal(3.14),
                available = BigDecimal(3.14),
                totalLabel = "",
                availableLabel = ""
            )
        )
        assertFalse(assetEquals.hasBalanceRestrictions())
    }

    @Test
    fun `minimum expire time getter`() {
        val minimumTimestamp = 1727433147L
        val exchangeRates = listOf(
            ExchangeRate(asset = "1", expiresAt = minimumTimestamp),
            ExchangeRate(asset = "2", expiresAt = minimumTimestamp + 1),
            ExchangeRate(asset = "3", expiresAt = minimumTimestamp + 2),
        )
        val res = exchangeRates.map { it.expiresAt }.getMinimum()
        assertEquals(minimumTimestamp, res)
    }

    @Test
    fun `exchange rate time difference calculation`() {
        val baseTimestamp = Instant.ofEpochMilli(1727433147 * 1000L)
        val deviceTimestamp = baseTimestamp.toEpochMilli()
        val exchangeRates = listOf(
            ExchangeRate(asset = "1", expiresAt = baseTimestamp.plusSeconds(23L).epochSecond),
            ExchangeRate(asset = "2", expiresAt = baseTimestamp.plusSeconds(24L).epochSecond),
            ExchangeRate(asset = "3", expiresAt = baseTimestamp.plusSeconds(27L).epochSecond),
            ExchangeRate(asset = "3", expiresAt = baseTimestamp.plusSeconds(17L).epochSecond),
        ).map { it.expiresAt }
        val res = exchangeRates.getExpireTimeMills(deviceTimestamp)
        assertEquals(17L * 1000, res)
    }

    @Test
    fun `exchange rate time difference with additional time calculation`() {
        val baseTimestamp = Instant.ofEpochMilli(1727433147 * 1000L)
        val deviceTimestamp = baseTimestamp.toEpochMilli()
        val exchangeRates = listOf(
            ExchangeRate(asset = "1", expiresAt = baseTimestamp.plusSeconds(23L).epochSecond),
            ExchangeRate(asset = "2", expiresAt = baseTimestamp.plusSeconds(24L).epochSecond),
            ExchangeRate(asset = "3", expiresAt = baseTimestamp.plusSeconds(27L).epochSecond),
            ExchangeRate(asset = "3", expiresAt = baseTimestamp.plusSeconds(17L).epochSecond),
        ).map { it.expiresAt }
        val additionalTimeMillis = 2000L
        val res =
            exchangeRates.getExpireTimeMills(deviceTimestamp, plusMillis = additionalTimeMillis)
        assertEquals(additionalTimeMillis + 17L * 1000, res)
    }

    @Test
    fun `amount off calculation`() {
        val promotion = Promotion(
            id = "", amountOff = "20"
        )
        val amount = "23.15"
        val res = promotion.getAmountWithDiscount(amount)
        assertEquals("3.15", res.toPlainString())
    }

    @Test
    fun `big amount off calculation`() {
        val promotion = Promotion(
            id = "", amountOff = "40"
        )
        val amount = "23.15"
        val res = promotion.getAmountWithDiscount(amount)
        assertEquals("0.00", res.toPlainString())
    }

    @Test
    fun `amount boundaries calculation`() {
        val promotion = Promotion(
            id = "", amountOff = "15", restrictions = Promotion.Restrictions(
                minimumAmount = "10"
            )
        )
        val amount = "5.15"
        val res = promotion.getAmountWithDiscount(amount)
        assertEquals("5.15", res.toPlainString())
    }

    @Test
    fun `negative amount off calculation`() {
        val promotion = Promotion(
            id = "", amountOff = "-1"
        )
        val amount = "23.15"
        val res = promotion.getAmountWithDiscount(amount)
        assertEquals("24.15", res.toPlainString())
    }

    @Test
    fun `percent off calculation`() {
        val promotion = Promotion(
            id = "", percentOff = "20"
        )
        val amount = "23.15"
        val res = promotion.getPercentAmount(amount)
        assertEquals("4.63", res.toPlainString())
    }

    @Test
    fun `percent fault tolerance`() {
        val promotion = Promotion(
            id = "", percentOff = "0"
        )
        val amount = "23.15"
        val res = promotion.getPercentAmount(amount)
        assertEquals("0.00", res.toPlainString())
    }

    @Test
    fun `negative percent fault tolerance`() {
        val promotion = Promotion(
            id = "", percentOff = "-20"
        )
        val amount = "23.15"
        val res = promotion.getPercentAmount(amount)
        assertEquals("0.00", res.toPlainString())
    }

    @Test
    fun `percent boundaries`() {
        val promotion = Promotion(
            id = "", percentOff = "50", restrictions = Promotion.Restrictions(
                maximumDiscount = "20"
            )
        )
        val amount = "50"
        val res = promotion.getAmountWithDiscount(amount)
        assertEquals("30.00", res.toPlainString())
    }

    @Test
    fun `amount off priority`() {
        val promotion = Promotion(
            id = "", percentOff = "20", amountOff = "10"
        )
        val amount = "23.15"
        val res = promotion.getAmountWithDiscount(amount)
        assertEquals("13.15", res.toPlainString())
    }

    @Test
    fun `internal url`() {
        val data = "https://brand.flexa.link/explore/pay"
        val isInternal = data.isInternal()
        assertTrue(isInternal)
    }

    @Test
    fun `external url`() {
        val data = "https://brand.spend.link/explore/pay"
        val isInternal = data.isInternal()
        assertFalse(isInternal)
    }

    @Test
    fun `need to modify`() {
        val data = "https://brand.flexa.link/explore/pay"
        val needToModify = data.needToModify()
        assertTrue(needToModify)
    }

    @Test
    fun `no need to modify`() {
        val data = "https://flexa.link/explore/pay"
        val needToModify = data.needToModify()
        assertFalse(needToModify)
    }

    @Test
    fun `no need to modify co`() {
        val data = "https://flexa.co/explore/pay"
        val needToModify = data.needToModify()
        assertFalse(needToModify)
    }

    @Test
    fun `no need to modify www`() {
        val data = "https://www.example.com"
        val needToModify = data.needToModify()
        assertFalse(needToModify)
    }

    @Test
    fun `recognize valid session with transaction`() {
        val session = CommerceSession.Data(
            id = UUID.randomUUID().toString(),
            status = "requires_transaction",
            transactions = listOf(
                CommerceSession.Data.Transaction(
                    expiresAt = Instant.now().plusSeconds(10).epochSecond,
                    status = "requested"
                )
            )
        )
        assertTrue(session.isValid())
    }

    @Test
    fun `recognize valid session with credit`() {
        val session = CommerceSession.Data(
            id = UUID.randomUUID().toString(),
            status = "requires_approval",
            credits = listOf(
                CommerceSession.Data.Credit(

                )
            ),
        )
        assertTrue(session.isValid())
    }

    @Test
    fun `recognize valid session with credit and transaction`() {
        val session = CommerceSession.Data(
            id = UUID.randomUUID().toString(),
            status = "requires_transaction",
            transactions = listOf(
                CommerceSession.Data.Transaction(
                    expiresAt = Instant.now().plusSeconds(10).epochSecond,
                    status = "requested"
                )
            ),
            credits = listOf(CommerceSession.Data.Credit()),
        )
        assertTrue(session.isValid())
    }

    @Test
    fun `recognize not valid session`() {
        val session = CommerceSession.Data(
            id = UUID.randomUUID().toString(),
            status = "completed",
            transactions = emptyList(),
            credits = emptyList(),
        )
        assertFalse(session.isValid())
    }

    @Test
    fun `pick the most recent credit`() {
        val credit1 = CommerceSession.Data.Credit(
            created = Instant.now().plusMillis(5000).epochSecond
        )
        val credit2 = CommerceSession.Data.Credit(
            created = Instant.now().plusMillis(5001).epochSecond
        )
        val session = CommerceSession.Data(
            id = UUID.randomUUID().toString(),
            status = "requires_transaction",
            credits = listOf(
                credit1, credit2
            ),
        )
        val credit = session.credit()
        assertEquals(credit2, credit)
    }
}
