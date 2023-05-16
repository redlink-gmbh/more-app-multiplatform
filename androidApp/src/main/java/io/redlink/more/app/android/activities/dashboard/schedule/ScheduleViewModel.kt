package io.redlink.more.app.android.activities.dashboard.schedule

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
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
import kotlinx.coroutines.flow.combine
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

    val schedulesByDate = mutableStateMapOf<LocalDate, List<ScheduleModel>>()

    val filterModel = DashboardFilterViewModel(coreFilterModel)

    init {
        viewModelScope.launch {
            coreViewModel.scheduleListState.collect { (added, removed, updated) ->
                val idsToRemove = removed + updated.map { it.scheduleId }.toSet()
                schedulesByDate.forEach { (date, schedules) ->
                    schedulesByDate[date] = schedules.filterNot { it.scheduleId in idsToRemove }
                }
                val schemasToAdd = added + updated
                schemasToAdd.groupBy { it.start.jvmLocalDate() }.forEach { (date, schedules) ->
                    schedulesByDate[date] = schedulesByDate.getOrDefault(date, emptyList()) + schedules
                }
                schedulesByDate.entries.removeIf { it.value.isEmpty() }
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
        coreViewModel.start(scheduleId)
    }

    fun pauseObservation(scheduleId: String) {
        coreViewModel.pause(scheduleId)
    }

    fun stopObservation(scheduleId: String) {
        coreViewModel.stop(scheduleId)
    }
}