package com.example.legoscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.legoscanner.data.InvalidApiKeyException
import com.example.legoscanner.data.NoNetworkException
import com.example.legoscanner.data.PartRow
import com.example.legoscanner.data.PartsRepository
import com.example.legoscanner.data.RateLimitException
import com.example.legoscanner.data.SetNotFoundException
import com.example.legoscanner.data.SetStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data class Loading(val setNum: String) : HomeUiState
    data class Success(val setNum: String, val parts: List<PartRow>) : HomeUiState
    data class Error(val setNum: String, val reason: ErrorReason) : HomeUiState
}

enum class ErrorReason {
    INVALID_KEY,
    SET_NOT_FOUND,
    RATE_LIMIT,
    NO_NETWORK,
    EMPTY_INVENTORY,
    UNKNOWN
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PartsRepository()
    private val setStore = SetStore(application)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading(setStore.setNum))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val currentSetNum: String get() = setStore.setNum

    init {
        load(setStore.setNum)
    }

    fun onSetNumberEntered(input: String) {
        if (input.isBlank()) return
        val setNum = SetStore.normalize(input)
        setStore.setNum = setNum
        load(setNum)
    }

    fun retry() = load(setStore.setNum)

    private fun load(setNum: String) {
        _uiState.value = HomeUiState.Loading(setNum)

        viewModelScope.launch {
            _uiState.value = try {
                val parts = repository.loadSetParts(setNum)
                if (parts.isEmpty()) {
                    HomeUiState.Error(setNum, ErrorReason.EMPTY_INVENTORY)
                } else {
                    HomeUiState.Success(setNum, parts)
                }
            } catch (e: InvalidApiKeyException) {
                HomeUiState.Error(setNum, ErrorReason.INVALID_KEY)
            } catch (e: SetNotFoundException) {
                HomeUiState.Error(setNum, ErrorReason.SET_NOT_FOUND)
            } catch (e: RateLimitException) {
                HomeUiState.Error(setNum, ErrorReason.RATE_LIMIT)
            } catch (e: NoNetworkException) {
                HomeUiState.Error(setNum, ErrorReason.NO_NETWORK)
            } catch (e: Exception) {
                HomeUiState.Error(setNum, ErrorReason.UNKNOWN)
            }
        }
    }
}
