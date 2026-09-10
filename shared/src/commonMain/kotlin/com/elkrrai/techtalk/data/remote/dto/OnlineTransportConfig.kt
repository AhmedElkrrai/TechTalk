package com.elkrrai.techtalk.data.remote.dto

const val DEFAULT_ONLINE_SERVER_URL = "ws://127.0.0.1:8080"

data class OnlineTransportConfig(
    val serverUrl: String = DEFAULT_ONLINE_SERVER_URL
)

fun interface OnlineAuthTokenProvider {
    suspend fun getToken(): String?
}

object NoopOnlineAuthTokenProvider : OnlineAuthTokenProvider {
    override suspend fun getToken(): String? = null
}
