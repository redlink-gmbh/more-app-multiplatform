package io.redlink.more.app.android.activities.info

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.redlink.more.app.android.R
import io.redlink.more.app.android.activities.NavigationScreen
import io.redlink.more.app.android.activities.BLESetup.BLEConnectionActivity
import io.redlink.more.app.android.extensions.getStringResource
import io.redlink.more.app.android.extensions.showNewActivity
import io.redlink.more.app.android.shared_composables.BasicText
import io.redlink.more.app.android.shared_composables.SmallTitle
import io.redlink.more.app.android.ui.theme.MoreColors

@Composable
fun InfoView(navController: NavController, viewModel: InfoViewModel) {
    val context = LocalContext.current
    val backStackEntry = remember { navController.currentBackStackEntry }
    val route = backStackEntry?.arguments?.getString(NavigationScreen.INFO.route)
    LaunchedEffect(route) {
        viewModel.viewDidAppear()
    }
    DisposableEffect(route) {
        onDispose {
            viewModel.viewDidDisappear()
        }
    }
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Divider()
            InfoItem(
                title = getStringResource(id = R.string.info_study_details),
                imageVector = Icons.Default.Info,
                contentDescription = getStringResource(id = R.string.info_study_details_desc),
                onClick = {
                    navController.navigate(NavigationScreen.STUDY_DETAILS.route)
                }
            )
            InfoItem(
                title = getStringResource(id = R.string.info_running_observations),
                imageVector = Icons.Outlined.Autorenew,
                contentDescription = getStringResource(id = R.string.info_running_observations_desc),
                onClick = {
                    navController.navigate(NavigationScreen.RUNNING_SCHEDULES.route)
                }
            )
            InfoItem(
                title = getStringResource(id = R.string.info_completed_observations),
                imageVector = Icons.Default.Check,
                contentDescription = getStringResource(id = R.string.info_completed_observations_desc),
                onClick = {
                    navController.navigate(NavigationScreen.COMPLETED_SCHEDULES.route)
                }
            )
            InfoItem(
                title = NavigationScreen.BLUETOOTH_CONNECTION.stringRes(),
                imageVector = Icons.Default.Watch,
                contentDescription = getStringResource(id = R.string.more_ble_icon_description),
                onClick = {
                    (context as? Activity)?.let {
                        showNewActivity(it, BLEConnectionActivity::class.java)
                    }
                }
            )
            InfoItem(
                title = getStringResource(id = R.string.info_settings),
                imageVector = Icons.Default.Settings,
                contentDescription = getStringResource(id = R.string.info_consent_settings_desc),
                onClick = {
                    navController.navigate(NavigationScreen.SETTINGS.route)
                }
            )
            InfoItem(
                title = getStringResource(id = R.string.info_leave_study),
                imageVector = Icons.Default.ExitToApp,
                contentDescription = getStringResource(id = R.string.info_leave_study_desc),
                onClick = {
                    navController.navigate(NavigationScreen.LEAVE_STUDY.route)
                }
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Spacer(modifier = Modifier.height(28.dp))

                    if(viewModel.institute !== null) {
                        SmallTitle(
                            text = viewModel.institute,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    if(viewModel.contactPerson !== null) {
                        SmallTitle(
                            text = viewModel.contactPerson,
                            modifier = Modifier.fillMaxWidth(),
                            color = MoreColors.Secondary,
                            textAlign = TextAlign.Center
                        )
                    }


                    if (viewModel.contactEmail !== null)
                        BasicText(
                            text = viewModel.contactEmail,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth(),
                            color = MoreColors.Secondary,
                            textAlign = TextAlign.Center
                        )

                    if (viewModel.contactPhoneNumber != null)
                        BasicText(
                            text = viewModel.contactPhoneNumber,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth(),
                            color = MoreColors.Secondary,
                            textAlign = TextAlign.Center
                        )

                    if(viewModel.contactPhoneNumber !== null || viewModel.contactEmail !== null || viewModel.contactPerson !== null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider()

                        BasicText(
                            text = getStringResource(id = R.string.info_disclaimer),
                            color = MoreColors.Secondary,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        )
                    }

                }
            }

        }
    }
}
