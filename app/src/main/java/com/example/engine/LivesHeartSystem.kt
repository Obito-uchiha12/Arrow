package com.example.engine

/**
 * Manages player hearts/lives state, cooldown regeneration, and hit penalties.
 */
data class LivesState(
    val currentLives: Int = MAX_LIVES,
    val maxLives: Int = MAX_LIVES,
    val lastRegenTimestamp: Long = System.currentTimeMillis(),
    val isInfiniteLives: Boolean = false
) {
    val isGameOver: Boolean get() = !isInfiniteLives && currentLives <= 0
    val canPlay: Boolean get() = isInfiniteLives || currentLives > 0

    companion object {
        const val MAX_LIVES = 5
        const val REGEN_INTERVAL_SECONDS = 300L // 5 minutes per heart
    }
}

object LivesHeartSystem {

    fun computeUpdatedLives(state: LivesState, currentTimeMillis: Long = System.currentTimeMillis()): LivesState {
        if (state.isInfiniteLives || state.currentLives >= state.maxLives) {
            return state.copy(lastRegenTimestamp = currentTimeMillis)
        }

        val elapsedMillis = (currentTimeMillis - state.lastRegenTimestamp).coerceAtLeast(0L)
        val intervalMillis = LivesState.REGEN_INTERVAL_SECONDS * 1000L
        val livesToAdd = (elapsedMillis / intervalMillis).toInt()

        if (livesToAdd > 0) {
            val newLives = (state.currentLives + livesToAdd).coerceAtMost(state.maxLives)
            val remainderMillis = elapsedMillis % intervalMillis
            val newTimestamp = currentTimeMillis - remainderMillis
            return state.copy(currentLives = newLives, lastRegenTimestamp = newTimestamp)
        }

        return state
    }

    fun consumeLife(state: LivesState): LivesState {
        if (state.isInfiniteLives) return state
        val updated = computeUpdatedLives(state)
        val remaining = (updated.currentLives - 1).coerceAtLeast(0)
        return updated.copy(currentLives = remaining)
    }

    fun refillFull(state: LivesState): LivesState {
        return state.copy(
            currentLives = state.maxLives,
            lastRegenTimestamp = System.currentTimeMillis()
        )
    }
}
