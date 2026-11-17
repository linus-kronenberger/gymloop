package com.example.gymloop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform