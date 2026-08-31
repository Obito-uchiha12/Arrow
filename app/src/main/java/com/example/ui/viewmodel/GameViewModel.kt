package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectsManager
import com.example.data.AppDatabase
import com.example.data.LevelProgressEntity
import com.example.data.ProgressRepository
import com.example.engine.LevelRepository
import com.example.engine.PuzzleGeometry
import com.example.engine.PuzzleSolver
import com.example.model.Arrow
import com.example.model.ArrowState
import com.example.model.BoardTheme
import com.example.model.LevelCategory
import com.example.model.PuzzleLevel
import com.example.model.UserSettings
import com.example.ui.components.BlockedArrowAnimation
import com.example.ui.components.FlyingArrowAnimation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ProgressRepository(db.levelProgressDao(), db.userSettingsDao())
    val soundManager = SoundEffectsManager(application)

    // User settings and progress flows from Room
    val userSettings: StateFlow<UserSettings> = repository.userSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    val allProgress: StateFlow<List<LevelProgressEntity>> = repository.allProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedCount: StateFlow<Int> = repository.completedLevelsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStars: StateFlow<Int> = repository.totalStars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val highestUnlockedLevel: StateFlow<Int> = repository.highestUnlockedLevel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // Game state
    private val _currentLevelId = MutableStateFlow(1)
    val currentLevelId: StateFlow<Int> = _currentLevelId.asStateFlow()

    private val _currentLevel = MutableStateFlow(LevelRepository.getLevel(1))
    val currentLevel: StateFlow<PuzzleLevel> = _currentLevel.asStateFlow()

    private val _remainingArrows = MutableStateFlow<List<Arrow>>(emptyList())
    val remainingArrows: StateFlow<List<Arrow>> = _remainingArrows.asStateFlow()

    private val _arrowStates = MutableStateFlow<Map<Int, ArrowState>>(emptyMap())
    val arrowStates: StateFlow<Map<Int, ArrowState>> = _arrowStates.asStateFlow()

    private val _clearableArrowIds = MutableStateFlow<Set<Int>>(emptySet())
    val clearableArrowIds: StateFlow<Set<Int>> = _clearableArrowIds.asStateFlow()

    private val _isDeadlocked = MutableStateFlow(false)
    val isDeadlocked: StateFlow<Boolean> = _isDeadlocked.asStateFlow()

    private val _flyingAnimations = MutableStateFlow<List<FlyingArrowAnimation>>(emptyList())
    val flyingAnimations: StateFlow<List<FlyingArrowAnimation>> = _flyingAnimations.asStateFlow()

    private val _blockedDisplacements = MutableStateFlow<Map<Int, BlockedArrowAnimation>>(emptyMap())
    val blockedDisplacements: StateFlow<Map<Int, BlockedArrowAnimation>> = _blockedDisplacements.asStateFlow()

    private val _hintArrowId = MutableStateFlow<Int?>(null)
    val hintArrowId: StateFlow<Int?> = _hintArrowId.asStateFlow()

    // Hearts / Lives (Default 3 hearts)
    val maxHearts = 3
    private val _currentHearts = MutableStateFlow(3)
    val currentHearts: StateFlow<Int> = _currentHearts.asStateFlow()

    private val _isOutOfHearts = MutableStateFlow(false)
    val isOutOfHearts: StateFlow<Boolean> = _isOutOfHearts.asStateFlow()

    private val _isVictory = MutableStateFlow(false)
    val isVictory: StateFlow<Boolean> = _isVictory.asStateFlow()

    private val _starsEarned = MutableStateFlow(3)
    val starsEarned: StateFlow<Int> = _starsEarned.asStateFlow()

    private val _movesCount = MutableStateFlow(0)
    val movesCount: StateFlow<Int> = _movesCount.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _selectedCategory = MutableStateFlow(LevelCategory.ALL)
    val selectedCategory: StateFlow<LevelCategory> = _selectedCategory.asStateFlow()

    private val _isLevelSelectScreen = MutableStateFlow(false)
    val isLevelSelectScreen: StateFlow<Boolean> = _isLevelSelectScreen.asStateFlow()

    private val _isSettingsSheetOpen = MutableStateFlow(false)
    val isSettingsSheetOpen: StateFlow<Boolean> = _isSettingsSheetOpen.asStateFlow()

    private val _isTutorialOpen = MutableStateFlow(false)
    val isTutorialOpen: StateFlow<Boolean> = _isTutorialOpen.asStateFlow()

    private val undoStack = mutableListOf<Arrow>()
    private var timerJob: Job? = null
    private var comboPitch = 1.0f

    // Session token to cancel & ignore stale animation callbacks on level restart/next
    private var currentLevelSessionId = 0
    private val activeAnimationJobs = mutableListOf<Job>()

    init {
        viewModelScope.launch {
            val lastPlayed = repository.getLastPlayedLevel()
            val highestUnlocked = repository.getHighestUnlockedLevel()
            val completed = repository.getCompletedLevelsList()
            if (lastPlayed == 1 && highestUnlocked == 1 && completed.isEmpty()) {
                _isTutorialOpen.value = true
            }
            loadLevel(lastPlayed)
        }

        viewModelScope.launch {
            userSettings.collect { settings ->
                soundManager.soundEnabled = settings.soundEnabled
                soundManager.hapticsEnabled = settings.hapticsEnabled
            }
        }
    }

    fun loadLevel(levelId: Int) {
        currentLevelSessionId++
        val thisSessionId = currentLevelSessionId

        // Cancel any pending animations from previous level session
        synchronized(activeAnimationJobs) {
            activeAnimationJobs.forEach { it.cancel() }
            activeAnimationJobs.clear()
        }

        val lvl = LevelRepository.getLevel(levelId)
        _currentLevelId.value = levelId
        _currentLevel.value = lvl
        _remainingArrows.value = lvl.arrows
        _arrowStates.value = lvl.arrows.associate { it.id to ArrowState.ACTIVE }
        _flyingAnimations.value = emptyList()
        _blockedDisplacements.value = emptyMap()
        _hintArrowId.value = null
        _isVictory.value = false
        _isOutOfHearts.value = false
        _currentHearts.value = maxHearts
        _movesCount.value = 0
        _elapsedSeconds.value = 0
        undoStack.clear()
        comboPitch = 1.0f

        recalculateAvailability(lvl.arrows)

        startTimer()
        LevelRepository.prewarm(levelId, radius = 4)

        viewModelScope.launch {
            repository.updateLastPlayedLevel(levelId)
        }
    }

    private fun recalculateAvailability(arrows: List<Arrow>) {
        if (arrows.isEmpty()) {
            _clearableArrowIds.value = emptySet()
            _isDeadlocked.value = false
            return
        }
        val clearable = PuzzleSolver.findClearableArrows(arrows)
        val clearableIds = clearable.map { it.id }.toSet()
        _clearableArrowIds.value = clearableIds
        _isDeadlocked.value = clearableIds.isEmpty()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_isVictory.value && !_isLevelSelectScreen.value && !_isOutOfHearts.value) {
                    _elapsedSeconds.value += 1
                }
            }
        }
    }

    /**
     * Touch entry point for an arrow.
     * Enforces the strict Arrow State Machine:
     * - Only ACTIVE arrows accept interaction
     * - MOVING, BLOCKED, or REMOVED arrows ignore taps
     * - Transitions:
     *   ACTIVE -> MOVING (on clear) -> REMOVED (on flight finish)
     *   ACTIVE -> BLOCKED (on blocked) -> ACTIVE (on recoil finish)
     */
    fun onArrowTapped(arrow: Arrow) {
        onArrowTappedById(arrow.id)
    }

    fun onArrowTappedById(arrowId: Int) {
        if (_isVictory.value || _isOutOfHearts.value) return

        val thisSessionId = currentLevelSessionId
        val currentState = _arrowStates.value[arrowId] ?: return

        // Ignore taps on non-ACTIVE arrows (prevents duplicate animation or rapid double-tap race)
        if (currentState != ArrowState.ACTIVE) {
            return
        }

        val currentRemaining = _remainingArrows.value
        val targetArrow = currentRemaining.find { it.id == arrowId } ?: return

        // Validate collision against currently active board arrows
        val collision = PuzzleGeometry.checkArrowCollision(targetArrow, currentRemaining)
        val animSpeed = userSettings.value.animationSpeed

        if (!collision.isBlocked) {
            // STATE TRANSITION: ACTIVE -> MOVING
            _arrowStates.value = _arrowStates.value + (arrowId to ArrowState.MOVING)

            // Remove from board immediately
            val updated = currentRemaining.filter { it.id != arrowId }
            _remainingArrows.value = updated
            undoStack.add(targetArrow)
            _movesCount.value += 1

            if (_hintArrowId.value == arrowId) {
                _hintArrowId.value = null
            }

            // Recalculate availability of remaining arrows
            recalculateAvailability(updated)

            soundManager.playClearSound(comboPitch)
            soundManager.vibrateSuccess()
            comboPitch = min(2.2f, comboPitch + 0.06f)

            // Launch flying outward animation
            val flightDuration = animSpeed.flightDurationMs.toLong()
            val flyingAnim = FlyingArrowAnimation(
                arrow = targetArrow,
                startTimeMs = System.currentTimeMillis(),
                durationMs = flightDuration
            )
            _flyingAnimations.value = _flyingAnimations.value + flyingAnim

            val job = viewModelScope.launch {
                try {
                    delay(flightDuration)
                } finally {
                    // Safe cleanup with session token check
                    if (currentLevelSessionId == thisSessionId) {
                        _flyingAnimations.value = _flyingAnimations.value.filter { it.arrow.id != arrowId }
                        // STATE TRANSITION: MOVING -> REMOVED
                        _arrowStates.value = _arrowStates.value + (arrowId to ArrowState.REMOVED)

                        // Evaluate win condition exactly once when all arrows are removed
                        if (_remainingArrows.value.isEmpty() && !_isVictory.value) {
                            handleVictory()
                        }
                    }
                }
            }
            synchronized(activeAnimationJobs) {
                activeAnimationJobs.add(job)
            }
        } else {
            // STATE TRANSITION: ACTIVE -> BLOCKED
            _arrowStates.value = _arrowStates.value + (arrowId to ArrowState.BLOCKED)
            comboPitch = 1.0f
            soundManager.playBlockedSound()
            soundManager.vibrateBlocked()
            _movesCount.value += 1

            if (!userSettings.value.zenMode) {
                val newHearts = (_currentHearts.value - 1).coerceAtLeast(0)
                _currentHearts.value = newHearts
                if (newHearts == 0) {
                    _isOutOfHearts.value = true
                }
            }

            // Spring displacement animation towards obstacle
            val blockedDuration = animSpeed.blockedDurationMs.toLong()
            val maxNudge = min(collision.minDistance * 0.35f, 0.45f)
            val blockedAnim = BlockedArrowAnimation(
                arrowId = arrowId,
                maxNudge = maxNudge,
                startTimeMs = System.currentTimeMillis(),
                durationMs = blockedDuration
            )
            _blockedDisplacements.value = _blockedDisplacements.value + (arrowId to blockedAnim)

            val job = viewModelScope.launch {
                try {
                    delay(blockedDuration + 100L)
                } finally {
                    if (currentLevelSessionId == thisSessionId) {
                        _blockedDisplacements.value = _blockedDisplacements.value - arrowId
                        // Re-enable interaction: BLOCKED -> ACTIVE
                        if (_arrowStates.value[arrowId] == ArrowState.BLOCKED) {
                            _arrowStates.value = _arrowStates.value + (arrowId to ArrowState.ACTIVE)
                        }
                    }
                }
            }
            synchronized(activeAnimationJobs) {
                activeAnimationJobs.add(job)
            }
        }
    }

    private fun handleVictory() {
        if (_isVictory.value) return
        _isVictory.value = true
        timerJob?.cancel()
        soundManager.playVictoryFanfare()
        soundManager.vibrateVictory()

        val time = _elapsedSeconds.value
        val moves = _movesCount.value
        val level = _currentLevel.value
        val calculatedStars = when {
            moves <= level.arrowCount -> 3
            moves <= level.arrowCount + 4 -> 2
            else -> 1
        }
        _starsEarned.value = calculatedStars

        viewModelScope.launch {
            repository.completeLevel(level.id, time, moves)
        }
    }

    fun onUndo() {
        if (undoStack.isEmpty() || _isVictory.value) return
        val lastArrow = undoStack.removeAt(undoStack.lastIndex)
        val updated = _remainingArrows.value + lastArrow
        _remainingArrows.value = updated
        _arrowStates.value = _arrowStates.value + (lastArrow.id to ArrowState.ACTIVE)
        _hintArrowId.value = null
        comboPitch = 1.0f
        soundManager.playUndoSound()
        recalculateAvailability(updated)

        // If out of hearts, restore 1 heart and resume
        if (_isOutOfHearts.value) {
            _currentHearts.value = 1
            _isOutOfHearts.value = false
        }
    }

    /**
     * HINT 1: Highlight an arrow that can currently move.
     * Consumes 1 highlight hint inventory.
     */
    fun onHighlightHint() {
        if (_isVictory.value || _isOutOfHearts.value) return
        val hintArrow = PuzzleSolver.getHint(_remainingArrows.value) ?: return

        viewModelScope.launch {
            val settings = userSettings.value
            if (settings.highlightHintsCount <= 0) {
                // Free refill offline if depleted
                repository.addHints(highlight = 3, autoMove = 0)
            }
            repository.consumeHighlightHint()

            _hintArrowId.value = hintArrow.id
            soundManager.playHintSound()
            soundManager.vibrateSuccess()
        }
    }

    /**
     * HINT 2: Show and perform one valid move automatically.
     * Consumes 1 auto-move hint inventory.
     */
    fun onAutoMoveHint() {
        if (_isVictory.value || _isOutOfHearts.value) return
        val hintArrow = PuzzleSolver.getHint(_remainingArrows.value) ?: return

        viewModelScope.launch {
            val settings = userSettings.value
            if (settings.autoMoveHintsCount <= 0) {
                // Free refill offline if depleted
                repository.addHints(highlight = 0, autoMove = 3)
            }
            repository.consumeAutoMoveHint()

            // Highlight target arrow briefly
            _hintArrowId.value = hintArrow.id
            soundManager.playHintSound()
            delay(220)

            // Perform single move
            onArrowTapped(hintArrow)
        }
    }

    fun onRestartLevel() {
        loadLevel(_currentLevelId.value)
    }

    fun onNextLevel() {
        val nextId = min(_currentLevelId.value + 1, LevelRepository.TOTAL_LEVELS)
        loadLevel(nextId)
    }

    fun dismissOutOfHeartsDialog() {
        _isOutOfHearts.value = false
        loadLevel(_currentLevelId.value)
    }

    fun enableZenModeFromDialog() {
        _isOutOfHearts.value = false
        val updated = userSettings.value.copy(zenMode = true)
        updateSettings(updated)
    }

    fun selectCategory(category: LevelCategory) {
        _selectedCategory.value = category
    }

    fun showLevelSelect() {
        _isLevelSelectScreen.value = true
    }

    fun hideLevelSelect() {
        _isLevelSelectScreen.value = false
    }

    fun openSettings() {
        _isSettingsSheetOpen.value = true
    }

    fun closeSettings() {
        _isSettingsSheetOpen.value = false
    }

    fun openTutorial() {
        _isTutorialOpen.value = true
    }

    fun closeTutorial() {
        _isTutorialOpen.value = false
    }

    fun updateSettings(settings: UserSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }
}
