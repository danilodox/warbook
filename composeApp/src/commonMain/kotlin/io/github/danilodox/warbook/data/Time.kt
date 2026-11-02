package io.github.danilodox.warbook.data

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Agora, em ms desde a época. Os repositórios recebem o relógio por parâmetro para os testes. */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
