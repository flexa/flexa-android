package com.flexa.core.domain.data

import com.flexa.core.data.rest.HeadersBundle

internal interface ITokenProvider {

    fun getTokenExpiration(): Long

    fun getToken(): String

    fun setToken(token: String?)

    fun getRefreshToken(headersBundle: HeadersBundle): String
}
