package io.redlink.more.app.android.activities.dashboard.schedule

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.redlink.more.app.android.activities.dashboard.filter.DashboardFilterViewModel
import io.redlink.more.app.android.extensions.jvmLocalDate
import io.redlink.more.app.android.observations.AndroidDataRecorder
import io.redlink.more.app.android.observations.HR.PolarHeartRateObservation
import io.redlink.more.app.android.services.ObservationRecordingService
import io.redlink.more.more_app_mutliplatform.models.ScheduleListType
import io.redlink.more.more_app_mutliplatform.models.ScheduleModel
import io.redlink.more.more_app_mutliplatform.viewModels.dashboard.CoreDashboardFilterViewModel
import io.redlink.more.more_app_mutliplatform.viewModels.schedules.CoreScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ScheduleViewModel(
    val coreFilterModel: CoreDashboardFilterViewModel,
    dataRecorder: AndroidDataRecorder,
    val scheduleListType: ScheduleListType
) : ViewModel() {

    private val coreViewModel = CoreScheduleViewModel(
        dataRecorder,
        coreFilterModel = coreFilterModel,
        scheduleListType = scheduleListType
    )

    val polarHrReady: MutableState<Boolean> = mutableStateOf(false)

    val schedules = mutableStateMapOf<LocalDate, List<ScheduleModel>>()

    val filterModel = DashboardFilterViewModel(coreFilterModel)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            coreViewModel.scheduleModelList.collect { map ->
                val javaConvertedMap = map.mapKeys { it.key.jvmLocalDate() }
                withContext(Dispatchers.Main) {
                    updateData(javaConvertedMap)
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            PolarHeartRateObservation.hrReady.collect {
                withContext(Dispatchers.Main) {
                    polarHrReady.value = it
                }
            }
        }
    }

    fun viewDidAppear() {
        coreViewModel.viewDidAppear()
    }

    fun viewDidDisappear() {
        coreViewModel.viewDidDisappear()
    }

    fun updateTaskStates() {
        ObservationRecordingService.updateTaskStates()
    }

    fun startObservation(scheduleId: String) {
        ObservationRecordingService.start(scheduleId)
    }

    fun pauseObservation(scheduleId: String) {
        ObservationRecordingService.pause(scheduleId)
    }

    fun stopObservation(scheduleId: String) {
        ObservationRecordingService.stop(scheduleId)
    }

    private fun updateData(data: Map<LocalDate, List<ScheduleModel>>) {
        schedules.clear()
        schedules.putAll(data.toSortedMap())
    }
}