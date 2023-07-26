package io.redlink.more.more_app_mutliplatform.viewModels.bluetoothConnection

import io.github.aakira.napier.Napier
import io.ktor.utils.io.core.Closeable
import io.redlink.more.more_app_mutliplatform.database.repository.BluetoothDeviceRepository
import io.redlink.more.more_app_mutliplatform.extensions.append
import io.redlink.more.more_app_mutliplatform.extensions.asClosure
import io.redlink.more.more_app_mutliplatform.extensions.clear
import io.redlink.more.more_app_mutliplatform.extensions.remove
import io.redlink.more.more_app_mutliplatform.extensions.removeWhere
import io.redlink.more.more_app_mutliplatform.extensions.set
import io.redlink.more.more_app_mutliplatform.services.bluetooth.BluetoothConnector
import io.redlink.more.more_app_mutliplatform.services.bluetooth.BluetoothConnectorObserver
import io.redlink.more.more_app_mutliplatform.services.bluetooth.BluetoothDevice
import io.redlink.more.more_app_mutliplatform.services.bluetooth.BluetoothState
import io.redlink.more.more_app_mutliplatform.util.Scope
import io.redlink.more.more_app_mutliplatform.util.Scope.repeatedLaunch
import io.redlink.more.more_app_mutliplatform.viewModels.CoreViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull

class CoreBluetoothConnectionViewModel(
    private val bluetoothConnector: BluetoothConnector,
    private val scanDuration: Long = 10000,
    private val scanInterval: Long = 5000
): CoreViewModel(), BluetoothConnectorObserver, Closeable {
    private val bluetoothDeviceRepository = BluetoothDeviceRepository(bluetoothConnector)
    val discoveredDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val connectedDevices = MutableStateFlow<Set<BluetoothDevice>>(emptySet())
    val connectingDevices = MutableStateFlow<Set<String>>(emptySet())

    val isScanning = MutableStateFlow(false)

    private var backgroundScanningEnabled = false
    private var viewActive = false

    private var scanJob: String? = null

    val bluetoothPower = MutableStateFlow(BluetoothState.ON)

    init {
        bluetoothConnector.addObserver(this)
        bluetoothConnector.replayStates()
    }

    fun enableBackgroundScanner() {
        if (!backgroundScanningEnabled) {
            backgroundScanningEnabled = true
            Scope.launch {
                if (!viewActive && bluetoothDeviceRepository.getDevices().firstOrNull()?.isNotEmpty() == true) {
                    delay(2000)
                    periodicScan(BACKGROUND_SCAN_DURATION, BACKGROUND_SCAN_INTERVAL)
                }
            }
        }
    }

    fun disableBackgroundScanner() {
        backgroundScanningEnabled = false
        if (!viewActive) {
            stopPeriodicScan()
        }
    }

    override fun viewDidAppear() {
        viewActive = true
        if (backgroundScanningEnabled) {
            stopPeriodicScan()
        }
        periodicScan()
    }

    private fun periodicScan(customScanDuration: Long = scanDuration, customScanInterval: Long = scanInterval) {
        launchScope {
            bluetoothPower.collect {
                if (it == BluetoothState.ON) {
                    startPeriodicScan(customScanDuration, customScanInterval)
                } else {
                    stopPeriodicScan()
                }
            }
        }
    }

    override fun viewDidDisappear() {
        super.viewDidDisappear()
        viewActive = false
        scanJob?.let { Scope.cancel(it) }
        scanJob = null
        discoveredDevices.clear()
        connectingDevices.clear()
        stopPeriodicScan()
        if (backgroundScanningEnabled) {
            Scope.launch {
                delay(10000L)
                if (backgroundScanningEnabled) {
                    periodicScan(BACKGROUND_SCAN_DURATION, BACKGROUND_SCAN_INTERVAL)
                }
            }
        }
    }

    private fun startPeriodicScan(customScanDuration: Long = scanDuration, customScanInterval: Long = scanInterval) {
        if (scanJob == null) {
            Napier.d { "Starting period scanner with Duration= $customScanDuration; Interval= $customScanInterval" }
            scanJob = repeatedLaunch(customScanInterval) {
                Napier.d { "Scanning..." }
                scanForDevices()
                delay(customScanDuration)
                Napier.d { "Stop Scanning..." }
                stopScanning()
            }.first
        }
    }

    fun stopPeriodicScan() {
        Napier.d { "Stopping period scanner!" }
        scanJob?.let { Scope.cancel(it) }
        scanJob = null
        bluetoothConnector.stopScanning()
    }

    fun scanForDevices() {
        bluetoothConnector.scan()
    }

    fun stopScanning() {
        bluetoothConnector.stopScanning()
    }

    fun connectToDevice(device: BluetoothDevice): Boolean {
        Napier.i { "Connecting to $device" }
        connectingDevices.append(device.address)
        return bluetoothConnector.connect(device)?.let {
            connectingDevices.remove(device.address)
            print(it)
            false
        } ?: true
    }

    fun disconnectFromDevice(device: BluetoothDevice) {
        Napier.i { "Disconnecting from $device" }
        bluetoothConnector.disconnect(device)
        bluetoothDeviceRepository.setAutoReconnect(device, false)
    }

    override fun isConnectingToDevice(bluetoothDevice: BluetoothDevice) {
        if (bluetoothDevice.address !in connectedDevices.value.mapNotNull { it.address }) {
            connectingDevices.append(bluetoothDevice.address)
        }
    }

    override fun didConnectToDevice(bluetoothDevice: BluetoothDevice) {
        if (connectedDevices.value.none { it.address == bluetoothDevice.address }) {
            connectedDevices.append(bluetoothDevice)
            connectingDevices.remove(bluetoothDevice.address)
            bluetoothDeviceRepository.setConnectionState(bluetoothDevice, true)
        }
    }

    override fun didDisconnectFromDevice(bluetoothDevice: BluetoothDevice) {
        connectedDevices.removeWhere { it.address == bluetoothDevice.address }
        bluetoothDeviceRepository.setConnectionState(bluetoothDevice, false)
    }

    override fun didFailToConnectToDevice(bluetoothDevice: BluetoothDevice) {
        connectedDevices.remove(bluetoothDevice)
        connectingDevices.remove(bluetoothDevice.address)
        bluetoothDeviceRepository.setConnectionState(bluetoothDevice, false)
        Napier.e { "Failed to connect to $bluetoothDevice" }
    }

    override fun onBluetoothStateChange(bluetoothState: BluetoothState) {
        bluetoothPower.set(bluetoothState)
    }

    override fun didDiscoverDevice(device: BluetoothDevice) {
        if (connectedDevices.value.none { it.address == device.address }
            && discoveredDevices.value.none { it.address == device.address }) {
            discoveredDevices.append(device)
            bluetoothDeviceRepository.shouldConnectToDiscoveredDevice(device) {
                if (it) {
                    connectToDevice(device)
                }
            }
        }
    }

    override fun removeDiscoveredDevice(device: BluetoothDevice) {
        discoveredDevices.removeWhere { it.address == device.address }
        connectingDevices.remove(device.address)
    }

    override fun isScanning(boolean: Boolean) {
        this.isScanning.set(boolean)
    }

    fun discoveredDevicesListChanges(providedState: (Set<BluetoothDevice>) -> Unit) =
        discoveredDevices.asClosure(providedState)

    fun connectedDevicesListChanges(providedState: (Set<BluetoothDevice>) -> Unit) =
        connectedDevices.asClosure(providedState)

    fun scanningIsChanging(providedState: (Boolean) -> Unit) = isScanning.asClosure(providedState)

    fun connectingDevicesListChanges(providedState: (Set<String>) -> Unit) =
        connectingDevices.asClosure(providedState)

    fun bluetoothStateChanged(providedState: (BluetoothState) -> Unit) = bluetoothPower.asClosure(providedState)

    override fun close() {
        scanJob?.let { Scope.cancel(it) }
        scanJob = null
        bluetoothConnector.stopScanning()
        bluetoothConnector.removeObserver(this)
    }

    companion object {
        private const val BACKGROUND_SCAN_DURATION = 2000L
        private const val BACKGROUND_SCAN_INTERVAL = 10000L
    }
}