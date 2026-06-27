package com.rodrigoleao.gramado2026.data.model

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object NavigateBack : UiEvent()
    // Used when deleting an item that should navigate further than just one step back
    object NavigateAfterDelete : UiEvent()
}
