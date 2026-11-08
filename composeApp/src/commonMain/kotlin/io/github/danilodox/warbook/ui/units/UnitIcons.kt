package io.github.danilodox.warbook.ui.units

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import io.github.danilodox.warbook.resources.Res
import io.github.danilodox.warbook.resources.unit_apprentice_warden
import io.github.danilodox.warbook.resources.unit_archer
import io.github.danilodox.warbook.resources.unit_baby_dragon
import io.github.danilodox.warbook.resources.unit_balloon
import io.github.danilodox.warbook.resources.unit_barbarian
import io.github.danilodox.warbook.resources.unit_bowler
import io.github.danilodox.warbook.resources.unit_dragon
import io.github.danilodox.warbook.resources.unit_dragon_rider
import io.github.danilodox.warbook.resources.unit_electro_dragon
import io.github.danilodox.warbook.resources.unit_electro_titan
import io.github.danilodox.warbook.resources.unit_furnace
import io.github.danilodox.warbook.resources.unit_giant
import io.github.danilodox.warbook.resources.unit_goblin
import io.github.danilodox.warbook.resources.unit_golem
import io.github.danilodox.warbook.resources.unit_headhunter
import io.github.danilodox.warbook.resources.unit_healer
import io.github.danilodox.warbook.resources.unit_hero_archer_queen
import io.github.danilodox.warbook.resources.unit_hero_barbarian_king
import io.github.danilodox.warbook.resources.unit_hero_grand_warden
import io.github.danilodox.warbook.resources.unit_hero_royal_champion
import io.github.danilodox.warbook.resources.unit_hog_rider
import io.github.danilodox.warbook.resources.unit_ice_golem
import io.github.danilodox.warbook.resources.unit_lava_hound
import io.github.danilodox.warbook.resources.unit_miner
import io.github.danilodox.warbook.resources.unit_minion
import io.github.danilodox.warbook.resources.unit_pekka
import io.github.danilodox.warbook.resources.unit_pet_diggy
import io.github.danilodox.warbook.resources.unit_pet_electro_owl
import io.github.danilodox.warbook.resources.unit_pet_frosty
import io.github.danilodox.warbook.resources.unit_pet_lassi
import io.github.danilodox.warbook.resources.unit_pet_mighty_yak
import io.github.danilodox.warbook.resources.unit_pet_phoenix
import io.github.danilodox.warbook.resources.unit_pet_poison_lizard
import io.github.danilodox.warbook.resources.unit_pet_sneezy
import io.github.danilodox.warbook.resources.unit_pet_spirit_fox
import io.github.danilodox.warbook.resources.unit_pet_unicorn
import io.github.danilodox.warbook.resources.unit_root_rider
import io.github.danilodox.warbook.resources.unit_siege_barracks
import io.github.danilodox.warbook.resources.unit_siege_battle_blimp
import io.github.danilodox.warbook.resources.unit_siege_battle_drill
import io.github.danilodox.warbook.resources.unit_siege_flame_flinger
import io.github.danilodox.warbook.resources.unit_siege_log_launcher
import io.github.danilodox.warbook.resources.unit_siege_wall_wrecker
import io.github.danilodox.warbook.resources.unit_spell_bat
import io.github.danilodox.warbook.resources.unit_spell_clone
import io.github.danilodox.warbook.resources.unit_spell_earthquake
import io.github.danilodox.warbook.resources.unit_spell_freeze
import io.github.danilodox.warbook.resources.unit_spell_frostmites
import io.github.danilodox.warbook.resources.unit_spell_haste
import io.github.danilodox.warbook.resources.unit_spell_healing
import io.github.danilodox.warbook.resources.unit_spell_ice_block
import io.github.danilodox.warbook.resources.unit_spell_invisibility
import io.github.danilodox.warbook.resources.unit_spell_jump
import io.github.danilodox.warbook.resources.unit_spell_lightning
import io.github.danilodox.warbook.resources.unit_spell_overgrowth
import io.github.danilodox.warbook.resources.unit_spell_poison
import io.github.danilodox.warbook.resources.unit_spell_rage
import io.github.danilodox.warbook.resources.unit_spell_recall
import io.github.danilodox.warbook.resources.unit_spell_revive
import io.github.danilodox.warbook.resources.unit_spell_skeleton
import io.github.danilodox.warbook.resources.unit_spell_totem
import io.github.danilodox.warbook.resources.unit_valkyrie
import io.github.danilodox.warbook.resources.unit_wall_breaker
import io.github.danilodox.warbook.resources.unit_witch
import io.github.danilodox.warbook.resources.unit_wizard
import io.github.danilodox.warbook.resources.unit_yeti
import io.github.danilodox.warbook.ui.components.UnitInitials
import io.github.danilodox.warbook.ui.components.UnitItem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Ícone do Fan Kit de cada unidade, pelo nome da API (192 px em WebP, corte quadrado).
 * Fora da tabela (equipamentos, unidades que o kit não tem) fica [UnitInitials].
 */
object UnitIcons {
    fun forName(apiName: String): DrawableResource? = icons[apiName]

    private val icons: Map<String, DrawableResource> = mapOf(
        "Barbarian" to Res.drawable.unit_barbarian,
        "Archer" to Res.drawable.unit_archer,
        "Giant" to Res.drawable.unit_giant,
        "Goblin" to Res.drawable.unit_goblin,
        "Wall Breaker" to Res.drawable.unit_wall_breaker,
        "Balloon" to Res.drawable.unit_balloon,
        "Wizard" to Res.drawable.unit_wizard,
        "Healer" to Res.drawable.unit_healer,
        "Dragon" to Res.drawable.unit_dragon,
        "P.E.K.K.A" to Res.drawable.unit_pekka,
        "Baby Dragon" to Res.drawable.unit_baby_dragon,
        "Miner" to Res.drawable.unit_miner,
        "Electro Dragon" to Res.drawable.unit_electro_dragon,
        "Yeti" to Res.drawable.unit_yeti,
        "Dragon Rider" to Res.drawable.unit_dragon_rider,
        "Electro Titan" to Res.drawable.unit_electro_titan,
        "Root Rider" to Res.drawable.unit_root_rider,
        "Minion" to Res.drawable.unit_minion,
        "Hog Rider" to Res.drawable.unit_hog_rider,
        "Valkyrie" to Res.drawable.unit_valkyrie,
        "Golem" to Res.drawable.unit_golem,
        "Witch" to Res.drawable.unit_witch,
        "Lava Hound" to Res.drawable.unit_lava_hound,
        "Bowler" to Res.drawable.unit_bowler,
        "Ice Golem" to Res.drawable.unit_ice_golem,
        "Headhunter" to Res.drawable.unit_headhunter,
        "Apprentice Warden" to Res.drawable.unit_apprentice_warden,
        "Furnace" to Res.drawable.unit_furnace,
        "Lightning Spell" to Res.drawable.unit_spell_lightning,
        "Healing Spell" to Res.drawable.unit_spell_healing,
        "Rage Spell" to Res.drawable.unit_spell_rage,
        "Jump Spell" to Res.drawable.unit_spell_jump,
        "Freeze Spell" to Res.drawable.unit_spell_freeze,
        "Clone Spell" to Res.drawable.unit_spell_clone,
        "Invisibility Spell" to Res.drawable.unit_spell_invisibility,
        "Recall Spell" to Res.drawable.unit_spell_recall,
        "Revive Spell" to Res.drawable.unit_spell_revive,
        "Totem Spell" to Res.drawable.unit_spell_totem,
        "Bag of Frostmites" to Res.drawable.unit_spell_frostmites,
        "Poison Spell" to Res.drawable.unit_spell_poison,
        "Earthquake Spell" to Res.drawable.unit_spell_earthquake,
        "Haste Spell" to Res.drawable.unit_spell_haste,
        "Skeleton Spell" to Res.drawable.unit_spell_skeleton,
        "Bat Spell" to Res.drawable.unit_spell_bat,
        "Overgrowth Spell" to Res.drawable.unit_spell_overgrowth,
        "Ice Block Spell" to Res.drawable.unit_spell_ice_block,
        "Barbarian King" to Res.drawable.unit_hero_barbarian_king,
        "Archer Queen" to Res.drawable.unit_hero_archer_queen,
        "Grand Warden" to Res.drawable.unit_hero_grand_warden,
        "Royal Champion" to Res.drawable.unit_hero_royal_champion,
        "Wall Wrecker" to Res.drawable.unit_siege_wall_wrecker,
        "Battle Blimp" to Res.drawable.unit_siege_battle_blimp,
        "Siege Barracks" to Res.drawable.unit_siege_barracks,
        "Log Launcher" to Res.drawable.unit_siege_log_launcher,
        "Flame Flinger" to Res.drawable.unit_siege_flame_flinger,
        "Battle Drill" to Res.drawable.unit_siege_battle_drill,
        "L.A.S.S.I" to Res.drawable.unit_pet_lassi,
        "Electro Owl" to Res.drawable.unit_pet_electro_owl,
        "Mighty Yak" to Res.drawable.unit_pet_mighty_yak,
        "Unicorn" to Res.drawable.unit_pet_unicorn,
        "Frosty" to Res.drawable.unit_pet_frosty,
        "Diggy" to Res.drawable.unit_pet_diggy,
        "Poison Lizard" to Res.drawable.unit_pet_poison_lizard,
        "Phoenix" to Res.drawable.unit_pet_phoenix,
        "Spirit Fox" to Res.drawable.unit_pet_spirit_fox,
        "Sneezy" to Res.drawable.unit_pet_sneezy,
    )
}

/** Ícone do Fan Kit preenchendo o espaço; sem ícone na tabela, as iniciais do nome. */
@Composable
fun UnitIcon(item: UnitItem, modifier: Modifier = Modifier) {
    val icon = UnitIcons.forName(item.key)
    if (icon == null) {
        UnitInitials(item, modifier)
    } else {
        Image(
            painterResource(icon),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize().clip(MaterialTheme.shapes.extraSmall),
        )
    }
}
