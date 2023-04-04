package io.redlink.more.more_app_mutliplatform.android.activities.dashboard.schedule

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.redlink.more.more_app_mutliplatform.android.extensions.jvmLocalDate
import io.redlink.more.more_app_mutliplatform.android.observations.AndroidDataRecorder
import io.redlink.more.more_app_mutliplatform.android.services.ObservationRecordingService
import io.redlink.more.more_app_mutliplatform.models.ScheduleModel
import io.redlink.more.more_app_mutliplatform.viewModels.schedules.CoreScheduleViewModel
import io.redlink.more.more_app_mutliplatform.models.ScheduleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ScheduleViewModel(androidDataRecorder: AndroidDataRecorder) : ViewModel() {
    private val coreViewModel = CoreScheduleViewModel(androidDataRecorder)

    val schedules = mutableStateMapOf<LocalDate, List<ScheduleModel>>()

    val activeScheduleState = mutableStateMapOf<String, ScheduleState>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            coreViewModel.scheduleModelList.collect { map ->
                val javaConvertedMap = map.mapKeys { it.key.jvmLocalDate() }
                withContext(Dispatchers.Main) {
                    updateData(javaConvertedMap)
                }
            }
        }
    }

    fun updateTaskStates(context: Context) {
        ObservationRecordingService.updateTaskStates(context)
    }

    fun startObservation(context: Context, scheduleId: String) {
        ObservationRecordingService.start(context, scheduleId)
        activeScheduleState[scheduleId] = ScheduleState.RUNNING
    }

    fun pauseObservation(context: Context, scheduleId: String) {
        ObservationRecordingService.pause(context, scheduleId)
        activeScheduleState[scheduleId] = ScheduleState.PAUSED
    }

    fun stopObservation(context: Context, scheduleId: String) {
        ObservationRecordingService.stop(context, scheduleId)
    }

    private fun updateData(data: Map<LocalDate, List<ScheduleModel>>) {
        schedules.clear()
        schedules.putAll(data.toSortedMap())
    }

    private fun removeSchedule(scheduleId: String) {
        coreViewModel.removeSchedule(scheduleId = scheduleId)
    }
}