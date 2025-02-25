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
package io.redlink.more.app.android.activities.consent

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.redlink.more.app.android.MoreApplication
import io.redlink.more.app.android.R
import io.redlink.more.app.android.extensions.getSecureID
import io.redlink.more.app.android.extensions.stringResource
import io.redlink.more.more_app_mutliplatform.models.AlertDialogModel
import io.redlink.more.more_app_mutliplatform.models.PermissionModel
import io.redlink.more.more_app_mutliplatform.services.extensions.toMD5
import io.redlink.more.more_app_mutliplatform.services.network.RegistrationService
import io.redlink.more.more_app_mutliplatform.viewModels.permission.CorePermissionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ConsentViewModelListener {
    fun credentialsStored()
    fun decline()
}

class ConsentViewModel(
    registrationService: RegistrationService,
    private val consentViewModelListener: ConsentViewModelListener
) : ViewModel() {
    private val coreModel = CorePermissionViewModel(registrationService, stringResource(R.string.consent_information))
    private var consentInfo: String? = null

    val permissionModel =
        mutableStateOf(PermissionModel("Title", "Participation Info", "Study Consent Info", emptyList()))
    val loading = mutableStateOf(false)
    val error = mutableStateOf<String?>(null)
    val permissions = mutableSetOf<String>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            coreModel.permissionModel.collect {
                withContext(Dispatchers.Main) {
                    permissionModel.value = it
                    permissions.addAll(MoreApplication.shared!!.observationFactory.studySensorPermissions())
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            coreModel.loadingFlow.collect {
                withContext(Dispatchers.Main) {
                    loading.value = it
                }
            }
        }
    }

    fun setConsentInfo(info: String) {
        this.consentInfo = info
    }

    fun acceptConsent(context: Context) {
        consentInfo?.let { info ->
            getSecureID(context)?.let { uniqueDeviceId ->
                coreModel.acceptConsent(info.toMD5(), uniqueDeviceId,
                    onSuccess = {
                        consentViewModelListener.credentialsStored()
                        MoreApplication.shared!!.newLogin()
                    }, onError = {
                        error.value = it?.message
                    })
            }
        }
    }

    fun openPermissionDeniedAlertDialog(context: Context) {
        MoreApplication.shared!!.mainContentCoreViewModel.openAlertDialog(AlertDialogModel(
            title = stringResource(R.string.required_permissions_not_granted_title),
            message = stringResource(R.string.required_permission_not_granted_message),
            positiveTitle = stringResource(R.string.proceed_to_settings_button),
            negativeTitle = stringResource(R.string.proceed_without_granting_button),
            onPositive = {
                MoreApplication.openSettings.value = true
                MoreApplication.shared!!.mainContentCoreViewModel.closeAlertDialog()
            },
            onNegative = {
                acceptConsent(context)
                MoreApplication.shared!!.mainContentCoreViewModel.closeAlertDialog()
            }
        ))
    }

    fun openNotificationPermissionDeniedAlertDialog() {
        MoreApplication.shared!!.mainContentCoreViewModel.openAlertDialog(AlertDialogModel(
            title = stringResource(R.string.notification_permission_not_granted_title),
            message = stringResource(R.string.notification_permission_not_granted_message),
            positiveTitle = stringResource(R.string.proceed_to_settings_button),
            negativeTitle = stringResource(R.string.proceed_without_granting_button),
            onPositive = {
                MoreApplication.openSettings.value = true
                MoreApplication.shared!!.mainContentCoreViewModel.closeAlertDialog()
            },
            onNegative = {
                MoreApplication.shared!!.mainContentCoreViewModel.closeAlertDialog()
            }
        ))
    }

    fun decline() {
        consentViewModelListener.decline()
    }

    fun buildConsentModel() {
        coreModel.buildConsentModel()
    }
}
