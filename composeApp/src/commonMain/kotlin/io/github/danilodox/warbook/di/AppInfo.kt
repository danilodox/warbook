package io.github.danilodox.warbook.di

/** Dados do app que cada plataforma informa ao iniciar. [serverUrl] muda por ambiente (emulador, celular, produção). */
data class AppInfo(val versionName: String, val serverUrl: String)
