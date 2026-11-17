package com.fraccetta.scopabriscola.ui.result

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fraccetta.scopabriscola.R
import com.fraccetta.scopabriscola.data.prefs.UserPreferences
import com.fraccetta.scopabriscola.data.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ResultUiState(
    val imagePath: String? = null,
    val message: String = "",
    val playerName: String = ""
)

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val userPrefs: UserPreferences,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val winner: String = savedStateHandle["winner"] ?: "2"
    private val gameType: String = savedStateHandle["gameType"] ?: "scopa"

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState

    init {
        viewModelScope.launch {
            val playerName = userPrefs.playerName.first()

            // Pick random image from assets/result
            val prefix = if (winner == "1") "win_" else "lose_"
            val files = withContext(Dispatchers.IO) {
                try {
                    context.assets.list("result")
                        ?.filter { it.startsWith(prefix) && (it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".webp")) }
                        ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            val imagePath = if (files.isNotEmpty()) {
                "result/${files.random()}"
            } else {
                null
            }

            // Pick random ironic message
            val messagesArray = if (winner == "1") {
                context.resources.getStringArray(R.array.win_messages)
            } else {
                context.resources.getStringArray(R.array.lose_messages)
            }
            val message = if (messagesArray.isNotEmpty()) messagesArray.random() else ""

            // Record result in stats
            val isSolo = com.fraccetta.scopabriscola.data.GameSession.isSolo
            statsRepository.recordResult(
                playerName = playerName,
                gameType = gameType,
                won = winner == "1",
                points = com.fraccetta.scopabriscola.data.GameSession.lastSessionPoints,
                scope = com.fraccetta.scopabriscola.data.GameSession.lastSessionScope,
                isSolo = isSolo,
                opponentName = com.fraccetta.scopabriscola.data.GameSession.opponentName ?: "",
                maxConsecutiveScope = com.fraccetta.scopabriscola.data.GameSession.lastMaxConsecutiveScope
            )
            com.fraccetta.scopabriscola.data.GameSession.isSolo = false
            com.fraccetta.scopabriscola.data.GameSession.lastMaxConsecutiveScope = 0

            _uiState.value = ResultUiState(
                imagePath = imagePath,
                message = message,
                playerName = playerName
            )
        }
    }
}
