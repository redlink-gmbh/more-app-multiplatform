package io.redlink.more.app.android.activities.runningSchedules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.redlink.more.app.android.activities.dashboard.schedule.ScheduleViewModel
import io.redlink.more.app.android.activities.dashboard.schedule.list.ScheduleListView
import io.redlink.more.app.android.activities.taskCompletion.TaskCompletionBarViewModel
import io.redlink.more.app.android.shared_composables.ScheduleListHeader

@Composable
fun RunningSchedulesView(viewModel: ScheduleViewModel, navController: NavController, taskCompletionBarViewModel: TaskCompletionBarViewModel) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        ScheduleListHeader(
            viewModel = viewModel,
            navController = navController,
            taskCompletionBarViewModel = taskCompletionBarViewModel
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column {
            ScheduleListView(
                navController = navController,
                scheduleViewModel = viewModel,
                showButton = true
            )
        }
    }
}