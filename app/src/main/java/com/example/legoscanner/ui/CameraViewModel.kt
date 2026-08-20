package com.example.legoscanner.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.legoscanner.data.Detection
import com.example.legoscanner.data.DetectionFailedException
import com.example.legoscanner.data.DetectionRepository
import com.example.legoscanner.data.DetectionStatus
import com.example.legoscanner.data.NoNetworkException
import com.example.legoscanner.data.PartRow
import com.example.legoscanner.data.PartsRepository
import com.example.legoscanner.data.ProgressStore
import com.example.legoscanner.data.HistoryStore
import com.example.legoscanner.data.RejectionReason
import com.example.legoscanner.data.RoboflowNotConfiguredException
import com.example.legoscanner.data.ScanEntry
import com.example.legoscanner.data.ScanRecord
import com.example.legoscanner.data.ScanResult
import com.example.legoscanner.data.SetStore
import com.example.legoscanner.util.PreparedImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Idle : ScanState
    data object Analyzing : ScanState
    data class Done(
        val result: ScanResult,
        val photo: Bitmap,
        val setParts: List<PartRow>
    ) : ScanState
    data class Failed(val reason: ScanErrorReason, val code: Int = 0) : ScanState
}

enum class ScanErrorReason {
    NOT_CONFIGURED,
    NO_NETWORK,
    SERVER_ERROR,
    IMAGE_READ,
    UNKNOWN
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val progressStore = ProgressStore(application)
    private val setStore = SetStore(application)
    private val historyStore = HistoryStore(application)
    private val detectionRepository = DetectionRepository()
    private val partsRepository = PartsRepository(progressStore = progressStore)

    private var currentRecordId: Long = 0L

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun analyze(image: PreparedImage) {
        _scanState.value = ScanState.Analyzing

        viewModelScope.launch {
            _scanState.value = try {
                val setParts = partsRepository.loadSetParts(setStore.setNum)
                val result = detectionRepository.detect(image.base64, image.bitmap, setParts)
                applyAutoAccepted(result)
                currentRecordId = System.currentTimeMillis()
                saveToHistory(result)
                ScanState.Done(result, image.bitmap, setParts)
            } catch (_: RoboflowNotConfiguredException) {
                ScanState.Failed(ScanErrorReason.NOT_CONFIGURED)
            } catch (_: NoNetworkException) {
                ScanState.Failed(ScanErrorReason.NO_NETWORK)
            } catch (e: DetectionFailedException) {
                ScanState.Failed(ScanErrorReason.SERVER_ERROR, e.code)
            } catch (_: Exception) {
                ScanState.Failed(ScanErrorReason.UNKNOWN)
            }
        }
    }

    fun confirm(detection: Detection) {
        increment(detection.matchedPart)
        updateStatus(detection.id) { it.copy(status = DetectionStatus.CONFIRMED) }
    }

    fun correct(detection: Detection, chosenPart: PartRow) {
        increment(chosenPart)
        updateStatus(detection.id) {
            it.copy(matchedPart = chosenPart, status = DetectionStatus.CORRECTED)
        }
    }

    fun dismiss(detection: Detection) {
        updateStatus(detection.id) { it.copy(status = DetectionStatus.DISMISSED) }
    }

    fun onImageReadFailed() {
        _scanState.value = ScanState.Failed(ScanErrorReason.IMAGE_READ)
    }

    fun reset() {
        _scanState.value = ScanState.Idle
    }

    private fun applyAutoAccepted(result: ScanResult) {
        result.detections
            .filter { it.status == DetectionStatus.AUTO_ACCEPTED }
            .forEach { increment(it.matchedPart) }
    }

    private fun increment(part: PartRow) {
        progressStore.add(setStore.setNum, part.key, delta = 1, limit = part.required)
    }

    private fun updateStatus(id: Int, transform: (Detection) -> Detection) {
        val current = _scanState.value as? ScanState.Done ?: return
        val updated = current.result.detections.map { if (it.id == id) transform(it) else it }
        val newResult = current.result.copy(detections = updated)
        _scanState.value = current.copy(result = newResult)
        saveToHistory(newResult)
    }

    private fun saveToHistory(result: ScanResult) {
        historyStore.save(
            ScanRecord(
                id = currentRecordId,
                setNum = setStore.setNum,
                timestamp = currentRecordId,
                entries = result.detections.map { it.toEntry() },
                rejectedNotInSet = result.rejected.count {
                    it.reason == RejectionReason.NOT_IN_SET
                },
                rejectedLowConfidence = result.rejected.count {
                    it.reason == RejectionReason.BELOW_THRESHOLD
                }
            )
        )
    }

    private fun Detection.toEntry() = ScanEntry(
        partNum = partNum,
        partName = matchedPart.name,
        colorName = matchedPart.colorName,
        confidence = confidence,
        colorAmbiguous = colorAmbiguous,
        status = status.name,
        correctedToPartNum = if (status == DetectionStatus.CORRECTED) matchedPart.partNum else null,
        correctedToColorName = if (status == DetectionStatus.CORRECTED) matchedPart.colorName else null
    )
}
