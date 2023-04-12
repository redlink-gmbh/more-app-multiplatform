package io.redlink.more.more_app_mutliplatform.android.observations.HR

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.model.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.redlink.more.more_app_mutliplatform.observations.Observation
import io.redlink.more.more_app_mutliplatform.observations.observationTypes.PolarVerityHeartRateType

private const val TAG = "PolarHearRateObservation"
private val permissions =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        setOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN
        )
    }

class PolarHeartRateObservation(context: Context) :
    Observation(observationType = PolarVerityHeartRateType(permissions)), HeartRateListener {

    private val hasPermission = this.hasPermissions(context)

    init {
        createPolarAPI(context)
    }

    override fun start(): Boolean {
        return true
    }
    override fun stop(onCompletion: () -> Unit) {

    }

    override fun observerAccessible(): Boolean {
        return hasPermission
    }

    override fun applyObservationConfig(settings: Map<String, Any>) {
    }

    private fun hasPermissions(context: Context): Boolean {
        permissions.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    override fun onDeviceConnected() {
        Log.d(TAG, "Device connected. Listening to HR")
    }

    override fun onDeviceDisconnected() {
        scanForDevices()
    }

    override fun onHeartRateUpdate(hr: Int) {
        Log.d(TAG, "HR Updated: $hr BPM")
        if (hr > 0) {
            storeData(hr)
        }
    }

    override fun onHeartRateReady() {

    }

    companion object {
        private lateinit var api: PolarBleApi
        private var scanDisposable: Disposable? = null
        private var deviceId: String = ""
        fun createPolarAPI(context: Context) {
            api = PolarBleApiDefaultImpl.defaultImplementation(
                context,
                setOf(
                    PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                    PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                    PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                    PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                    PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                    PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                    PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
                )
            )
            api.setPolarFilter(false)
            api.setApiCallback(PolarObserverCallback())
        }


        fun scanForDevices() {
            scanDisposable = api.searchForDevice()
                .subscribe(
                    { polarDeviceInfo: PolarDeviceInfo ->
                        connectDevice(polarDeviceInfo.deviceId)
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Device scan failed. Reason $error")
                    },
                    {
                        Log.d(TAG, "complete")
                    }
                )
        }

        private fun connectDevice(device: String) {
            api.connectToDevice(device)
            deviceId = device
            scanDisposable?.dispose()
        }
    }
}