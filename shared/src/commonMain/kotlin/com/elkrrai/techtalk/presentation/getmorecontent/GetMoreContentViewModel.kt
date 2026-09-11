package com.elkrrai.techtalk.presentation.getmorecontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elkrrai.techtalk.domain.repository.TechTalkRepository
import com.elkrrai.techtalk.presentation.getmorecontent.state.GetMoreContentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GetMoreContentViewModel(private val repository: TechTalkRepository) : ViewModel() {

    private val _state = MutableStateFlow(GetMoreContentState())
    val state: StateFlow<GetMoreContentState> = _state.asStateFlow()

    fun onImportTipPackJson(json: String) {
        runBusy {
            val result = repository.importTipPack(json)
            buildString {
                append("Imported ${result.tipsImported} tips")
                append(" (${result.topicsCreated} new topics, ${result.topicsMatched} matched)")
                if (result.unknownTechnologies.isNotEmpty()) {
                    append(". Unknown technologies: ${result.unknownTechnologies.joinToString()}")
                }
            }
        }
    }

    fun onImportBattlePackJson(json: String) {
        runBusy {
            val count = repository.importBattlePack(json)
            "Imported $count questions"
        }
    }

    fun onExportAllTips() {
        runBusy {
            val count = repository.exportAllTipsByTopic()
            "Exported $count tip pack file(s)"
        }
    }

    fun onExportAllBattles() {
        runBusy {
            val count = repository.exportAllBattlesByTechs()
            "Exported $count battle pack file(s)"
        }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    private fun runBusy(block: suspend () -> String) {
        viewModelScope.launch {
            _state.update { it.copy(isBusy = true) }
            val message = runCatching { block() }.getOrElse { "Failed: ${it.message ?: it::class.simpleName}" }
            _state.update { it.copy(isBusy = false, message = message) }
        }
    }
}
