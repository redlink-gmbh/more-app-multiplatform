package io.redlink.more.app.android.activities.dashboard.schedule.list

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import io.github.aakira.napier.Napier
import io.grpc.Context
import io.redlink.more.app.android.activities.dashboard.schedule.ScheduleViewModel
import io.redlink.more.app.android.shared_composables.ScheduleList
import io.redlink.more.more_app_mutliplatform.models.ScheduleListType


@Composable
fun ScheduleListView(navController: NavController, scheduleViewModel: ScheduleViewModel, type: ScheduleListType) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        if (scheduleViewModel.getScheduleMap(type).isNotEmpty()) {
            scheduleViewModel.getScheduleMap(type).toSortedMap().let { schedules ->
                ScheduleList(schedules = schedules, navController = navController, viewModel = scheduleViewModel)
            }
        }
    }
}