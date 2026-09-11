package com.elkrrai.techtalk.presentation.battle.online.mapper

import com.elkrrai.techtalk.domain.model.online.OnlineAnswerOption
import com.elkrrai.techtalk.presentation.battle.component.BattleAnswerOptionUi

/** Always `isCorrect = false` — only the server knows which answer is correct;
 * that's revealed later via an `AnswerResult` event. The server's String [answerId]s
 * are coerced to the shared [BattleAnswerOptionUi]'s Long id via a stable hash; the
 * ViewModel keeps its own id -> original-string map to submit the right answer back. */
fun List<OnlineAnswerOption>.toBattleAnswerOptionUi(): List<BattleAnswerOptionUi> =
    map { BattleAnswerOptionUi(id = it.answerId.hashCode().toLong(), text = it.text, isCorrect = false) }
