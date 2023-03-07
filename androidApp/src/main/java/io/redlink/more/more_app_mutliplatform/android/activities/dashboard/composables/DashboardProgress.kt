package io.redlink.more.more_app_mutliplatform.android.activities.dashboard.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.redlink.more.more_app_mutliplatform.android.activities.dashboard.DashboardViewModel
import io.redlink.more.more_app_mutliplatform.android.shared_composables.ActivityProgressView

@Composable
fun DashboardProgress(model: DashboardViewModel) {
    val context = LocalContext.current
    Column(modifier = Modifier
        .fillMaxWidth()
    ) {
        ActivityProgressView(
            finishedTasks = model.finishedTasks.value,
            totalTasks = model.totalTasks.value
        )
    }
}