package io.github.danilodox.warbook.data

/** Tema escolhido nas Configurações. Escuro é o padrão. */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
    ;

    /** Se a tela fica escura, dado o tema do aparelho. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        DARK -> true
        LIGHT -> false
        SYSTEM -> systemDark
    }
}

/** Nada salvo ou um nome que não existe mais (de outra versão) = escuro. */
internal fun themeModeOf(name: String?): ThemeMode = ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.DARK
