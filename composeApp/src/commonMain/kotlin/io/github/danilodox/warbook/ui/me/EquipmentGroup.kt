package io.github.danilodox.warbook.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.me_equipment_other
import io.github.danilodox.warbook.resources.me_group_maxed
import io.github.danilodox.warbook.ui.components.UnitGrid
import io.github.danilodox.warbook.ui.components.UnitItem
import io.github.danilodox.warbook.ui.theme.Spacing
import io.github.danilodox.warbook.ui.theme.tabular
import io.github.danilodox.warbook.ui.units.UnitIcon
import io.github.danilodox.warbook.ui.units.UnitLevelTable
import org.jetbrains.compose.resources.stringResource

/** Equipamentos de um herói; [hero] `null` = equipamento que a tabela não conhece. */
internal data class EquipmentGroup(val hero: UnitItem?, val items: List<UnitItem>)

/**
 * Separa os equipamentos por herói, na ordem dos heróis do jogador; herói que o jogador não tem
 * (ainda) vem depois, e o que a tabela não conhece fica por último.
 */
internal fun groupEquipment(
    equipment: List<UnitItem>,
    heroes: List<UnitItem>,
    levels: UnitLevelTable?,
    heroItem: (String) -> UnitItem,
): List<EquipmentGroup> {
    val byHero = equipment.groupBy { levels?.heroOf(it.key) }
    val known = heroes.map { it.key }
    val order = known + byHero.keys.filterNotNull().filter { it !in known } + listOf(null)
    return order.mapNotNull { key ->
        val items = byHero[key] ?: return@mapNotNull null
        EquipmentGroup(key?.let { name -> heroes.firstOrNull { it.key == name } ?: heroItem(name) }, items)
    }
}

/** Um grupo: retrato e nome do herói, quantos no máximo do CV, e a grade dos equipamentos. */
@Composable
internal fun EquipmentGroupBlock(group: EquipmentGroup, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            val hero = group.hero
            if (hero != null) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) { UnitIcon(hero) }
            }
            Text(
                hero?.name ?: stringResource(Res.string.me_equipment_other),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.me_group_maxed, group.items.count { it.isMax }, group.items.size),
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        UnitGrid(group.items)
    }
}
