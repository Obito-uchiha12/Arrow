package com.example.engine

/**
 * Optional rewarded ads / sponsor monetization interface with pluggable implementations.
 * Allows graceful offline fallback and clean modular extension.
 */
interface RewardAdProvider {
    val isAdAvailable: Boolean
    fun showRewardedAd(onRewarded: (RewardType) -> Unit, onFailed: () -> Unit)
}

enum class RewardType {
    EXTRA_HINT,
    REFILL_HEARTS,
    DOUBLE_STARS
}

/**
 * Default offline / non-blocking ad provider that gracefully simulates zero ads or instant reward in debug.
 */
class OfflineRewardAdProvider(
    private val allowSimulatedRewardsInDebug: Boolean = false
) : RewardAdProvider {

    override val isAdAvailable: Boolean
        get() = allowSimulatedRewardsInDebug

    override fun showRewardedAd(onRewarded: (RewardType) -> Unit, onFailed: () -> Unit) {
        if (allowSimulatedRewardsInDebug) {
            onRewarded(RewardType.EXTRA_HINT)
        } else {
            onFailed()
        }
    }
}
