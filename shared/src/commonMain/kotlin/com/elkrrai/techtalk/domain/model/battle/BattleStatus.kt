package com.elkrrai.techtalk.domain.model.battle

/**
 * Persisted by **name** in `battle_history.status`; the data layer parses defensively
 * (`runCatching { BattleStatus.valueOf(...) }`), so an unknown value degrades to
 * "not a win" rather than crashing.
 */
enum class BattleStatus { WIN, LOSS, RESIGNED }
