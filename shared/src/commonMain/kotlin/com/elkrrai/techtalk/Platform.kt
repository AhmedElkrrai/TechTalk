package com.elkrrai.techtalk

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform