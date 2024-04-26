package com.flexa.core.shared

import com.flexa.core.data.rest.RestRepository
import com.flexa.core.entity.AppAccount
import com.flexa.core.toJsonObject
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class SerializerProviderTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testAppAccount() {
        val raw = "{\n" +
                "    \"object\": \"collection\",\n" +
                "    \"url\": \"/accounts/me/app_accounts\",\n" +
                "    \"has_more\": false,\n" +
                "    \"data\": [\n" +
                "        {\n" +
                "            \"account_id\": \"1d62643880e0537462f10e4731de26a8819c6a05c766e9ca68c26f4d96101989\",\n" +
                "            \"assets\": [\n" +
                "                {\n" +
                "                    \"asset\": \"solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp/slip44:501\",\n" +
                "                    \"balance\": \"0.5\",\n" +
                "                    \"key\": {\n" +
                "                        \"expires_at\": 1717063622,\n" +
                "                        \"length\": 6,\n" +
                "                        \"prefix\": \"846056862059\",\n" +
                "                        \"secret\": \"XI3VUDNHFCJCSQI56PUDEQLXEXN3DSL7\"\n" +
                "                    },\n" +
                "                    \"label\": \"0.5 SOL\",\n" +
                "                    \"value\": {\n" +
                "                        \"asset\": \"iso4217/usd\",\n" +
                "                        \"label\": \"\$87.94 available\",\n" +
                "                        \"label_titlecase\": \"\$87.94 Available\"\n" +
                "                    }\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}"

        val jsonElement = Json.parseToJsonElement(raw)
        val data = jsonElement.jsonObject["data"] ?: JsonObject(emptyMap())
        val dto = json.decodeFromJsonElement<List<AppAccount>>(data)
        val hasMore = jsonElement.jsonObject["has_more"].toString().toBoolean()
        println(dto)
        assertFalse(hasMore)
        assertTrue(dto.isNotEmpty())
        assertEquals(
            "1d62643880e0537462f10e4731de26a8819c6a05c766e9ca68c26f4d96101989",
            dto[0].accountId
        )
    }

    @Test
    fun testDoubleToStringConversion() {
        val clazz = com.flexa.core.shared.AppAccount(
            accountId = "123", custodyModel = CustodyModel.LOCAL,  availableAssets = listOf(
                AvailableAsset(assetId = "4567", balance = 0.034)
            )
        )

        val obj = clazz.toJsonObject()
        println(obj)
        assertEquals(
            "{\"account_id\":\"123\",\"assets\":[{\"asset\":\"4567\",\"balance\":\"0.034\"}]}",
            obj.toString()
        )
    }

    @Test
    fun testNoAssetsProfile() {
        val raw = "{\n" +
                "    \"object\": \"collection\",\n" +
                "    \"url\": \"/accounts/me/app_accounts\",\n" +
                "    \"has_more\": false,\n" +
                "    \"data\": [\n" +
                "        {\n" +
                "            \"account_id\": \"6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b\"\n" +
                "        }\n" +
                "    ]\n" +
                "}"
        val jsonElement = RestRepository.json.parseToJsonElement(raw)
        val data = jsonElement.jsonObject["data"] ?: JsonObject(emptyMap())
        val dto = json.decodeFromJsonElement<List<com.flexa.core.entity.AppAccount>>(data)
        assertTrue(dto.first().availableAssets.isEmpty())
    }

    @Test
    fun `app account with null data processing`() {
        val raw = "{\n" +
                "  \"object\": \"collection\",\n" +
                "  \"url\": \"/accounts/me/app_accounts\",\n" +
                "  \"has_more\": false,\n" +
                "  \"data\": null\n" +
                "}\n"
        val jsonElement = RestRepository.json.parseToJsonElement(raw)
        val data = jsonElement.jsonObject["data"]
        val accData = if (data !is JsonNull) {
            data!!
        } else buildJsonArray {  }
        val dto =
            RestRepository.json.decodeFromJsonElement<List<com.flexa.core.entity.AppAccount>>(
                accData
            )
        println(dto)
    }
}