/*
 * Copyright LBI-DHP and/or licensed to LBI-DHP under one or more
 * contributor license agreements (LBI-DHP: Ludwig Boltzmann Institute
 * for Digital Health and Prevention -- A research institute of the
 * Ludwig Boltzmann Gesellschaft, Österreichische Vereinigung zur
 * Förderung der wissenschaftlichen Forschung).
 * Licensed under the Apache 2.0 license with Commons Clause
 * (see https://www.apache.org/licenses/LICENSE-2.0 and
 * https://commonsclause.com/).
 */
package io.redlink.more.app.android.activities.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.redlink.more.app.android.MoreApplication
import io.redlink.more.app.android.activities.BLESetup.BLEConnectionActivity
import io.redlink.more.app.android.activities.dashboard.DashboardViewModel
import io.redlink.more.app.android.activities.dashboard.schedule.ScheduleViewModel
import io.redlink.more.app.android.activities.info.InfoViewModel
import io.redlink.more.app.android.activities.leaveStudy.LeaveStudyViewModel
import io.redlink.more.app.android.activities.notification.NotificationViewModel
import io.redlink.more.app.android.activities.notification.filter.NotificationFilterViewModel
import io.redlink.more.app.android.activities.observations.limeSurvey.LimeSurveyActivity
import io.redlink.more.app.android.activities.observations.questionnaire.QuestionnaireViewModel
import io.redlink.more.app.android.activities.setting.SettingsViewModel
import io.redlink.more.app.android.activities.studyDetails.StudyDetailsViewModel
import io.redlink.more.app.android.activities.studyDetails.observationDetails.ObservationDetailsViewModel
import io.redlink.more.app.android.activities.taskCompletion.TaskCompletionBarViewModel
import io.redlink.more.app.android.activities.tasks.TaskDetailsViewModel
import io.redlink.more.more_app_mutliplatform.models.AlertDialogModel
import io.redlink.more.more_app_mutliplatform.models.ScheduleListType
import io.redlink.more.more_app_mutliplatform.models.StudyState
import io.redlink.more.more_app_mutliplatform.viewModels.dashboard.CoreDashboardFilterViewModel
import io.redlink.more.more_app_mutliplatform.viewModels.notifications.CoreNotificationFilterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(context: Context) : ViewModel() {
    val tabIndex = mutableStateOf(0)
    val showBackButton = mutableStateOf(false)
    val navigationBarTitle = mutableStateOf("")

    val studyIsUpdating = mutableStateOf(false)
    val studyState = mutableStateOf(StudyState.NONE)
    val finishText = mutableStateOf<String?>(null)

    private var initFinished = false

    val notificationViewModel: NotificationViewModel
    val notificationFilterViewModel: NotificationFilterViewModel
    val manualTasks =
        ScheduleViewModel(CoreDashboardFilterViewModel(), MoreApplication.shared!!.dataRecorder, ScheduleListType.MANUALS)
    val runningSchedulesViewModel: ScheduleViewModel by lazy {
        ScheduleViewModel(
            CoreDashboardFilterViewModel(),
            MoreApplication.shared!!.dataRecorder,
            ScheduleListType.RUNNING
        )
    }
    val completedSchedulesViewModel: ScheduleViewModel by lazy {
        ScheduleViewModel(
            CoreDashboardFilterViewModel(),
            MoreApplication.shared!!.dataRecorder,
            ScheduleListType.COMPLETED
        )
    }
    val dashboardViewModel = DashboardViewModel(manualTasks)
    val settingsViewModel: SettingsViewModel by lazy { SettingsViewModel() }
    val studyDetailsViewModel: StudyDetailsViewModel by lazy { StudyDetailsViewModel() }
    val leaveStudyViewModel: LeaveStudyViewModel by lazy { LeaveStudyViewModel() }

    val taskCompletionBarViewModel = TaskCompletionBarViewModel()

    val infoVM: InfoViewModel by lazy {
        InfoViewModel()
    }

    private val simpleQuestionnaireViewModel by lazy {
        QuestionnaireViewModel()
    }

    private val taskDetailsViewModel: TaskDetailsViewModel by lazy {
        TaskDetailsViewModel(MoreApplication.shared!!.dataRecorder)
    }
    val alertDialogOpen = mutableStateOf<AlertDialogModel?>(null)


    init {
        viewModelScope.launch(Dispatchers.IO) {
            MoreApplication.shared!!.mainContentCoreViewModel.alertDialogModel.collect {
                withContext(Dispatchers.Main) {
                    alertDialogOpen.value = it
                }
            }
        }
        viewModelScope.launch {
            MoreApplication.shared!!.studyIsUpdating.collect {
                studyIsUpdating.value = it
            }
        }
        viewModelScope.launch {
            MoreApplication.shared!!.currentStudyState.collect {
                finishText.value = MoreApplication.shared!!.finishText
                studyState.value = it
                if (it == StudyState.ACTIVE && initFinished) {
                    showBLESetup(context)
                }
            }
        }
        val coreNotificationFilterViewModel = CoreNotificationFilterViewModel()
        notificationViewModel = NotificationViewModel(coreNotificationFilterViewModel)
        notificationFilterViewModel = NotificationFilterViewModel(coreNotificationFilterViewModel)
        showBLESetup(context)
        initFinished = true
    }

    private fun showBLESetup(context: Context) {
        MoreApplication.shared!!.showBleSetup().let { (firstTime, hasBLEObservations) ->
            if (hasBLEObservations) {
                if (firstTime) {
                    openBLESetupActivity(context)
                }
            }
        }
    }

    fun getTaskDetailsVM(scheduleId: String) =
        taskDetailsViewModel.apply { setSchedule(scheduleId) }

    fun viewDidAppear() {
    }

    fun openLimesurvey(context: Context, activityResultLauncher: ActivityResultLauncher<Intent>, scheduleId: String?, observationId: String?, notificationId: String?) {
        (context as? Activity)?.let { activity ->
            val intent = Intent(activity, LimeSurveyActivity::class.java)
            intent.putExtra(
                LimeSurveyActivity.LIME_SURVEY_ACTIVITY_SCHEDULE_ID,
                scheduleId
            )
            intent.putExtra(
                LimeSurveyActivity.LIME_SURVEY_ACTIVITY_OBSERVATION_ID,
                observationId
            )
            intent.putExtra(LimeSurveyActivity.LIME_SURVEY_ACTIVITY_NOTIFICATION_ID, notificationId)
            activityResultLauncher.launch(intent)
        }
    }

    fun creteNewSimpleQuestionViewModel(scheduleId: String? = null, observationId: String? = null, notificationId: String?): QuestionnaireViewModel {
        if (scheduleId != null || observationId != null) {
            simpleQuestionnaireViewModel.apply {
                if (!scheduleId.isNullOrBlank()) {
                    setScheduleId(scheduleId, notificationId)
                } else if (!observationId.isNullOrBlank()) {
                    setObservationId(observationId, notificationId)
                }
            }
        }
        return simpleQuestionnaireViewModel
    }

    fun createObservationDetailView(observationId: String): ObservationDetailsViewModel {
        return ObservationDetailsViewModel(observationId)
    }

    private fun openBLESetupActivity(context: Context) {
        (context as? Activity)?.let {
            val intent = Intent(context, BLEConnectionActivity::class.java)
            intent.putExtra(BLEConnectionActivity.SHOW_DESCR_PART2, true)
            it.startActivity(intent)
        }
    }
}