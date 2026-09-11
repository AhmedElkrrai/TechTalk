package com.elkrrai.techtalk.presentation.battle.state

import com.elkrrai.techtalk.domain.model.online.BattleTimeControl

fun BattleTimeControl.getLabel(): String = when (this) {
    BattleTimeControl.ONE_MINUTE -> "1 min"
    BattleTimeControl.TWO_MINUTES -> "2 min"
    BattleTimeControl.THREE_MINUTES -> "3 min"
    BattleTimeControl.INFINITY -> "No limit"
}
