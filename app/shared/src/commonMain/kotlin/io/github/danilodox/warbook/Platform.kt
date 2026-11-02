package io.github.danilodox.warbook

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform