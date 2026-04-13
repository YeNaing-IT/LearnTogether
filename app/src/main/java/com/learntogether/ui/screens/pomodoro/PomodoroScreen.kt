package com.learntogether.ui.screens.pomodoro

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.learntogether.data.repository.PomodoroRepository
import com.learntogether.data.repository.SettingsRepository
import com.learntogether.data.repository.UserRepository
import com.learntogether.ui.components.TLTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimerPhase { WORK, BREAK, IDLE }

data class PomodoroUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val totalSeconds: Int = 30 * 60,
    val remainingSeconds: Int = 30 * 60,
    val workMinutes: Int = 30,
    val breakMinutes: Int = 5,
    val sessionsCompleted: Int = 0,
    val isRunning: Boolean = false
)

@HiltViewModel
class PomodoroViewModel @Inject constructor(
    private val pomodoroRepository: PomodoroRepository,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            settingsRepository.pomodoroWorkMinutes.collect { mins ->
                _uiState.update { it.copy(workMinutes = mins, totalSeconds = mins * 60, remainingSeconds = mins * 60) }
            }
        }
        viewModelScope.launch { settingsRepository.pomodoroBreakMinutes.collect { mins -> _uiState.update { it.copy(breakMinutes = mins) } } }
    }

    fun startWork() {
        val mins = _uiState.value.workMinutes
        _uiState.update { it.copy(phase = TimerPhase.WORK, totalSeconds = mins * 60, remainingSeconds = mins * 60, isRunning = true) }
        startTimer()
    }

    fun startBreak() {
        val mins = _uiState.value.breakMinutes
        _uiState.update { it.copy(phase = TimerPhase.BREAK, totalSeconds = mins * 60, remainingSeconds = mins * 60, isRunning = true) }
        startTimer()
    }

    fun togglePause() {
        if (_uiState.value.isRunning) {
            timerJob?.cancel()
            _uiState.update { it.copy(isRunning = false) }
        } else {
            _uiState.update { it.copy(isRunning = true) }
            startTimer()
        }
    }

    fun reset() {
        timerJob?.cancel()
        val mins = _uiState.value.workMinutes
        _uiState.update { it.copy(phase = TimerPhase.IDLE, totalSeconds = mins * 60, remainingSeconds = mins * 60, isRunning = false) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0 && _uiState.value.isRunning) {
                delay(1000)
                _uiState.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            if (_uiState.value.remainingSeconds <= 0) onTimerComplete()
        }
    }

    private fun onTimerComplete() {
        if (_uiState.value.phase == TimerPhase.WORK) {
            viewModelScope.launch {
                val user = userRepository.getCurrentUserOnce()
                if (user != null) {
                    pomodoroRepository.saveSession(user.userId, _uiState.value.workMinutes)
                    userRepository.addLearnedMinutes(user.userId, _uiState.value.workMinutes)
                }
            }
            _uiState.update { it.copy(sessionsCompleted = it.sessionsCompleted + 1, phase = TimerPhase.IDLE, isRunning = false) }
        } else {
            _uiState.update { it.copy(phase = TimerPhase.IDLE, isRunning = false) }
        }
    }
}

@Composable
fun PomodoroScreen(
    onBack: () -> Unit,
    viewModel: PomodoroViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress = if (uiState.totalSeconds > 0) uiState.remainingSeconds.toFloat() / uiState.totalSeconds else 1f
    val minutes = uiState.remainingSeconds / 60
    val seconds = uiState.remainingSeconds % 60
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(topBar = { TLTopBar(title = "Pomodoro Timer", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Phase label
            Text(
                text = when (uiState.phase) {
                    TimerPhase.WORK -> "Focus Time"
                    TimerPhase.BREAK -> "Break Time"
                    TimerPhase.IDLE -> "Ready?"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = when (uiState.phase) {
                    TimerPhase.WORK -> primaryColor
                    TimerPhase.BREAK -> secondaryColor
                    TimerPhase.IDLE -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Timer circle
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                val arcColor = when (uiState.phase) {
                    TimerPhase.WORK -> primaryColor
                    TimerPhase.BREAK -> secondaryColor
                    TimerPhase.IDLE -> surfaceVariant
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 12.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    // Background arc
                    drawArc(color = surfaceVariant, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    // Progress arc
                    drawArc(color = arcColor, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                }
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Sessions: ${uiState.sessionsCompleted}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(40.dp))

            // Controls
            when (uiState.phase) {
                TimerPhase.IDLE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { viewModel.startWork() }, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Focus", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(onClick = { viewModel.startBreak() }, modifier = Modifier.height(52.dp), shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Outlined.Coffee, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Break")
                        }
                    }
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FilledTonalButton(onClick = { viewModel.togglePause() }, modifier = Modifier.size(64.dp), shape = CircleShape) {
                            Icon(if (uiState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                        OutlinedButton(onClick = { viewModel.reset() }, modifier = Modifier.size(64.dp), shape = CircleShape) {
                            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}
