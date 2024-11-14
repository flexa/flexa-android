package com.flexa.spend.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class DeepLinkParserTest {

    @Test
    fun account() {
        val link = "https://com.subhost.flexa.link/account"
        val type = DeepLinkParser.getDeepLink(link)
        assertEquals(DeepLink.Account, type)
    }

    @Test
    fun `data and privacy`() {
        val link = "https://com.me.flexa.link/account/data"
        val type = DeepLinkParser.getDeepLink(link)
        assertEquals(DeepLink.DataAndPrivacy, type)
    }

    @Test
    fun `delete account`() {
        val link = "https://com.me.flexa.link/account/delete"
        val type = DeepLinkParser.getDeepLink(link)
        assertEquals(DeepLink.DeleteAccount, type)
    }

    @Test
    fun `places to pay`() {
        val link = "https://com.me.flexa.link/explore"
        val type = DeepLinkParser.getDeepLink(link)
        assertEquals(DeepLink.PlacesToPay, type)
    }

    @Test
    fun `brand web`() {
        val link = "https://com.me.flexa.link/explore/123/4567"
        val type = DeepLinkParser.getDeepLink(link)
        assertTrue(type is DeepLink.Brands)
        val url = (type as DeepLink.Brands).url
        assertEquals("123/4567", url)
    }

    @Test
    fun `how to pay`() {
        val link = "https://com.me.flexa.link/guides/how-to-pay"
        val type = DeepLinkParser.getDeepLink(link)
        assertEquals(DeepLink.HowToPay, type)
    }

    @Test
    fun `main screen`() {
        val link = "https://com.me.flexa.link/pay"
        val type = DeepLinkParser.getDeepLink(link)
        assertEquals(DeepLink.Pay, type)
    }

    @Test
    fun `commerce session`() {
        val link = "https://com.me.flexa.link/pay/Fx552Mg3"
        val type = DeepLinkParser.getDeepLink(link)
        assertTrue(type is DeepLink.CommerceSession)
        val url = (type as DeepLink.CommerceSession).url
        assertEquals("Fx552Mg3", url)
    }

    @Test
    fun `pinned brands`() {
        val link = "https://com.me.flexa.link/pinned"
        val type = DeepLinkParser.getDeepLink(link)
        assertEquals(DeepLink.PinnedBrands, type)
    }

    @Test
    fun `report issue`() {
        val link = "https://com.me.flexa.link/report-an-issue"
        val type = DeepLinkParser.getDeepLink(link)
        assertTrue(type is DeepLink.ReportIssue)
        assertEquals(link, (type as DeepLink.ReportIssue).url)
    }

    @Test
    fun `report issue brand`() {
        val link = "https://com.me.flexa.link/report-an-issue/flexa"
        val type = DeepLinkParser.getDeepLink(link)
        assertTrue(type is DeepLink.ReportIssueBrand)
        val url = (type as DeepLink.ReportIssueBrand).url
        assertEquals("flexa", url)
    }

    @Test
    fun `support article`() {
        val link = "https://com.me.flexa.link/support/articles/3142909"
        val type = DeepLinkParser.getDeepLink(link)
        assertTrue(type is DeepLink.SupportArticle)
        val url = (type as DeepLink.SupportArticle).url
        assertEquals("articles/3142909", url)
    }

    @Test
    fun `login link`() {
        val link = "https://com.me.flexa.link/verify/7hF9v5q"
        val type = DeepLinkParser.getDeepLink(link)
        assertTrue(type is DeepLink.Login)
        val url = (type as DeepLink.Login).url
        assertEquals(link, url)
    }
}