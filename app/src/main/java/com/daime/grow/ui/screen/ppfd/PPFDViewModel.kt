package com.daime.grow.ui.screen.ppfd

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import com.daime.grow.domain.model.LightSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class PPFDMode {
    SENSOR, CAMERA
}

data class PPFDUiState(
    val lux: Float = 0f,
    val ppfd: Double = 0.0,
    val selectedSource: LightSource = LightSource.LED_WHITE,
    val calibrationMultiplier: Float = 1.0f,
    val isSensorAvailable: Boolean = true,
    val isHoldActive: Boolean = false,
    val mode: PPFDMode = PPFDMode.SENSOR
)

@HiltViewModel
class PPFDViewModel @Inject constructor(
    private val sensorManager: SensorManager
) : ViewModel(), SensorEventListener {

    private val _uiState = MutableStateFlow(PPFDUiState())
    val uiState: StateFlow<PPFDUiState> = _uiState.asStateFlow()

    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    init {
        if (lightSensor == null) {
            _uiState.update { it.copy(isSensorAvailable = false, mode = PPFDMode.CAMERA) }
        }
    }

    fun startMeasuring() {
        if (_uiState.value.mode == PPFDMode.SENSOR) {
            lightSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopMeasuring() {
        sensorManager.unregisterListener(this)
    }

    fun toggleMode() {
        val newMode = if (_uiState.value.mode == PPFDMode.SENSOR) PPFDMode.CAMERA else PPFDMode.SENSOR
        
        // Se for para sensor e não estiver disponível, não troca ou avisa (aqui vou deixar trocar, o UI trata)
        if (newMode == PPFDMode.SENSOR && lightSensor == null) return

        _uiState.update { it.copy(mode = newMode) }
        
        if (newMode == PPFDMode.SENSOR) {
            startMeasuring()
        } else {
            stopMeasuring()
        }
    }

    fun onCameraLuxChanged(luxValue: Float) {
        if (_uiState.value.mode == PPFDMode.CAMERA && !_uiState.value.isHoldActive) {
            _uiState.update { 
                it.copy(
                    lux = luxValue,
                    ppfd = calculatePPFD(luxValue, it.selectedSource, it.calibrationMultiplier)
                )
            }
        }
    }

    fun updateLightSource(source: LightSource) {
        _uiState.update { 
            val newState = it.copy(selectedSource = source)
            newState.copy(ppfd = calculatePPFD(newState.lux, source, newState.calibrationMultiplier))
        }
    }

    fun updateCalibration(multiplier: Float) {
        _uiState.update { 
            it.copy(
                calibrationMultiplier = multiplier,
                ppfd = calculatePPFD(it.lux, it.selectedSource, multiplier)
            )
        }
    }

    fun toggleHold() {
        _uiState.update { it.copy(isHoldActive = !it.isHoldActive) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT && !_uiState.value.isHoldActive) {
            val luxValue = event.values[0]
            _uiState.update { 
                it.copy(
                    lux = luxValue,
                    ppfd = calculatePPFD(luxValue, it.selectedSource, it.calibrationMultiplier)
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun calculatePPFD(lux: Float, source: LightSource, calibration: Float): Double {
        return lux * source.factor * calibration
    }

    override fun onCleared() {
        super.onCleared()
        stopMeasuring()
    }
}
