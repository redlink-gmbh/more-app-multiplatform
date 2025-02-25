//
//  PolarConnector.swift
//  More
//
//  Created by Jan Cortiel on 24.04.23.
//  Copyright © 2023 Ludwig Boltzmann Institute for
//  Digital Health and Prevention - A research institute
//  of the Ludwig Boltzmann Gesellschaft,
//  Oesterreichische Vereinigung zur Foerderung
//  der wissenschaftlichen Forschung 
//  Licensed under the Apache 2.0 license with Commons Clause 
//  (see https://www.apache.org/licenses/LICENSE-2.0 and
//  https://commonsclause.com/).
//

import CoreBluetooth
import Foundation
import PolarBleSdk
import RxSwift
import shared
import UIKit

class PolarConnector: NSObject, BluetoothConnector {
    var specificBluetoothConnectors: KotlinMutableDictionary<NSString, BluetoothConnector> = KotlinMutableDictionary()
    var bluetoothState: BluetoothState = .on
    
    var discovered: KotlinMutableSet<BluetoothDevice> = KotlinMutableSet()
    var connected: KotlinMutableSet<BluetoothDevice> = KotlinMutableSet()
    
    var delegate: BLEConnectorDelegate?
    private var scanningWithUnknownBLEState = false
    private var devicesSubscription: Disposable? = nil
    
    lazy var polarApi: PolarBleApi = { [weak self] in
        var api = PolarBleApiDefaultImpl
            .polarImplementation(DispatchQueue.main,
                                features: [
                                    .feature_hr,
                                    .feature_battery_info,
                                    .feature_device_info,
                                    .feature_polar_offline_recording,
                                    .feature_polar_online_streaming,
                                    .feature_polar_sdk_mode,
                                    .feature_polar_device_time_setup,
                                ])
        
        if let self {
            api.observer = self
            api.polarFilter(false)
            api.deviceInfoObserver = self
            api.deviceFeaturesObserver = self
            api.powerStateObserver = self
        }
        
        return api
    }()
    
    var observer: KotlinMutableSet<BluetoothConnectorObserver> = KotlinMutableSet()
    
    var scanning = false {
        didSet {
            isScanning(boolean: scanning)
        }
    }
    
    func addSpecificBluetoothConnector(key: String, connector: BluetoothConnector) {
        specificBluetoothConnectors[key] = connector
    }

    func connect(device: BluetoothDevice) -> KotlinError? {
        if let deviceId = device.deviceId {
            do {
                try polarApi.connectToDevice(deviceId)
                return nil
            } catch {
                print(error)
                return KotlinError(message: error.localizedDescription)
            }
        }
        return KotlinError(message: "No valid device ID")
    }

    func disconnect(device: BluetoothDevice) {
        if let deviceId = device.deviceId {
            do {
                try polarApi.disconnectFromDevice(deviceId)
            } catch {
                print(error)
            }
        }
    }
    
    func scan() {
        if CBManager.authorization == .restricted || CBManager.authorization == .denied {
            AppDelegate.shared.mainContentCoreViewModel.openAlertDialog(model: AlertDialogModel(title: "Required Permissions Were Not Granted", message: "This study requires one or more sensor permissions to function correctly. You may choose to decline these permissions; however, doing so may result in the application and study not functioning fully or as expected. Would you like to navigate to settings to allow the app access to these necessary permissions?", positiveTitle: "Proceed to Settings", negativeTitle: "Proceed Without Granting Permissions", onPositive: {
                if let url = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(url) {
                    UIApplication.shared.open(url, options: [:], completionHandler: nil)
                }
                AppDelegate.shared.mainContentCoreViewModel.closeAlertDialog()
            }, onNegative: {
                AppDelegate.shared.mainContentCoreViewModel.closeAlertDialog()
            }))
        }
        else if !scanning && self.observer.count > 0 && bluetoothState == BluetoothState.on {
            print("Polar: Starting the scan...")
            DispatchQueue.main.async { [weak self] in
                if let self {
                    self.scanning = true
                    self.devicesSubscription = self.polarApi.searchForDevice().subscribe(onNext: { device in
                        self.didDiscoverDevice(device: BluetoothDevice.fromPolarDevice(polarInfo: device))
                    }, onError: { error in
                        print(error)
                        self.scanning = false
                    }, onDisposed: {
                        self.scanning = false
                    })
                }
            }
        }
    }

    func stopScanning() {
        DispatchQueue.main.async { [weak self] in
            if let self, self.scanning {
                print("Polar: Stopping the scan and cleaning up...")
                self.devicesSubscription?.dispose()
                self.polarApi.cleanup()
                self.scanning = false
            }
        }
    }

    func close() {
        
    }
    
    func isConnectingToDevice(bluetoothDevice: BluetoothDevice) {
        updateObserver { $0.isConnectingToDevice(bluetoothDevice: bluetoothDevice) }
    }
    
    func didConnectToDevice(bluetoothDevice: BluetoothDevice) {
        if let address = bluetoothDevice.address, !address.isEmpty && !connected.contains(where: { ($0 as? BluetoothDevice)?.address == address }) {
            connected.add(bluetoothDevice)
            removeDiscoveredDevice(device: bluetoothDevice)
        }
        updateObserver{ $0.didConnectToDevice(bluetoothDevice: bluetoothDevice)}
    }
    
    func didDisconnectFromDevice(bluetoothDevice: BluetoothDevice) {
        if let device = connected.filter({ ($0 as? BluetoothDevice)?.address == bluetoothDevice.address}).first {
            connected.remove(device)
        }
        PolarVerityHeartRateObservation.polarDeviceDisconnected()
        updateObserver{ $0.didDisconnectFromDevice(bluetoothDevice: bluetoothDevice)}
    }
    
    func didFailToConnectToDevice(bluetoothDevice: BluetoothDevice) {
        updateObserver{ $0.didFailToConnectToDevice(bluetoothDevice: bluetoothDevice)}
    }
    
    func removeDiscoveredDevice(device: BluetoothDevice) {
        if let address = device.address, let device = discovered.filter({($0 as? BluetoothDevice)?.address == address}).first {
            discovered.remove(device)
        }
        updateObserver{ $0.removeDiscoveredDevice(device: device)}
    }
    
    func didDiscoverDevice(device: BluetoothDevice) {
        if let address = device.address, !address.isEmpty && !connected.contains(where: { ($0 as? BluetoothDevice)?.address == address }) && !discovered.contains(where: {($0 as? BluetoothDevice)?.address == address}) {
            discovered.add(device)
        }
        updateObserver{ $0.didDiscoverDevice(device: device)}
    }
    
    func isScanning(boolean: Bool) {
        if boolean != scanning {
            scanning = boolean
        }
        updateObserver{ $0.isScanning(boolean: boolean)}
    }
    
    func onBluetoothStateChange(bluetoothState: BluetoothState) {
        self.bluetoothState = bluetoothState
        updateObserver{ $0.onBluetoothStateChange(bluetoothState: bluetoothState)}
    }
    
    func addObserver(bluetoothConnectorObserver: BluetoothConnectorObserver) {
        self.observer.add(bluetoothConnectorObserver)
        if self.observer.count > 0 {
            replayStates()
        }
    }
    
    func removeObserver(bluetoothConnectorObserver: BluetoothConnectorObserver) {
        self.observer.remove(bluetoothConnectorObserver)
        if self.observer.count == 0 {
            stopScanning()
        }
    }
    
    func updateObserver(action: @escaping (BluetoothConnectorObserver) -> Void) {
        observer.forEach{
            if let observer = $0 as? BluetoothConnectorObserver {
                action(observer)
            }
        }
    }
    
    func replayStates() {
        print("Polar Connector: Replaying states...")
        onBluetoothStateChange(bluetoothState: self.bluetoothState)
        connected.forEach { self.didConnectToDevice(bluetoothDevice: $0 as! BluetoothDevice)}
        discovered.forEach{ self.didDiscoverDevice(device: $0 as! BluetoothDevice)}
        isScanning(boolean: scanning)
    }
    
}

extension PolarConnector: PolarBleApiObserver {
    func deviceDisconnected(_ identifier: PolarBleSdk.PolarDeviceInfo, pairingError: Bool) {
        print("Polar disconnected: \(identifier.name). Had paring error: \(pairingError)")
        self.didDisconnectFromDevice(bluetoothDevice: BluetoothDevice.fromPolarDevice(polarInfo: identifier))
    }
    
    func deviceConnecting(_ identifier: PolarBleSdk.PolarDeviceInfo) {
        print("Polar connecting: \(identifier.name)")
        self.isConnectingToDevice(bluetoothDevice: BluetoothDevice.fromPolarDevice(polarInfo: identifier))
    }
    
    func deviceConnected(_ identifier: PolarDeviceInfo) {
        print("Polar connected: \(identifier.name)")
        self.didConnectToDevice(bluetoothDevice: BluetoothDevice.fromPolarDevice(polarInfo: identifier))
    }
}

extension PolarConnector: PolarBleApiPowerStateObserver {
    func blePowerOn() {
        print("Polar power on")
        self.onBluetoothStateChange(bluetoothState: .on)
        Task { [weak self] in
            self?.scan()
            try await Task.sleep(nanoseconds: 1_000_000_000)
            self?.stopScanning()
        }
    }
    
    func blePowerOff() {
        print("Polar power off")
        self.onBluetoothStateChange(bluetoothState: .off)
        self.connected.forEach { [weak self] device in
            if let device = device as? BluetoothDevice {
                self?.didDisconnectFromDevice(bluetoothDevice: device)
            }
        }
        self.discovered.forEach { [weak self] device in
            if let device = device as? BluetoothDevice {
                self?.removeDiscoveredDevice(device: device)
            }
        }
        stopScanning()
    }
}

extension PolarConnector: PolarBleApiDeviceFeaturesObserver {
    // Deprecated
    func hrFeatureReady(_ identifier: String) {
        print("HR ready!")
    }
    
    // Deprecated
    func ftpFeatureReady(_ identifier: String) {
        print("FTP Feature ready!")
    }
    
    // Deprecated
    func streamingFeaturesReady(_ identifier: String, streamingFeatures: Set<PolarBleSdk.PolarDeviceDataType>) {
        print("Stream Features ready!")
    }
    
    func bleSdkFeatureReady(_ identifier: String, feature: PolarBleSdk.PolarBleSdkFeature) {
        if feature == .feature_hr {
            print("HR ready")
            PolarVerityHeartRateObservation.setHRReady()
        }
    }
    
    
}

extension PolarConnector: PolarBleApiDeviceInfoObserver {
    func batteryLevelReceived(_ identifier: String, batteryLevel: UInt) {
        print("Battery level for \(identifier): \(batteryLevel)")
    }
    
    func disInformationReceived(_ identifier: String, uuid: CBUUID, value: String) {
        print("Disinformation received by \(identifier): \(uuid); \(value)")
    }
   
}

extension PolarConnector: PolarBleApiLogger {
    func message(_ str: String) {
        print("Polar logger: \(str)")
    }
}
