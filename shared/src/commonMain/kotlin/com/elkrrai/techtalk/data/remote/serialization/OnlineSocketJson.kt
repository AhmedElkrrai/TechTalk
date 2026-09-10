package com.elkrrai.techtalk.data.remote.serialization

import kotlinx.serialization.json.Json

object OnlineSocketJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
        encodeDefaults = true
        explicitNulls = false
        classDiscriminator = "type"
    }
}
