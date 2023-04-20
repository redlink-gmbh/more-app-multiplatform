//
//  ScheduleViewModel.swift
//  iosApp
//
//  Created by Julia Mayrhauser on 07.03.23.
//  Copyright © 2023 Redlink GmbH. All rights reserved.
//

import shared

class ScheduleViewModel: ObservableObject {
    let recorder = IOSDataRecorder()
    let filterViewModel: DashboardFilterViewModel
    private let coreModel: CoreScheduleViewModel
    
    private var currentFilters: FilterModel? = nil
    private var originalSchedules: [Int64: [ScheduleModel]] = [:]
    @Published var runningSchedules: [Int64: [ScheduleModel]] = [:]
    @Published var completedSchedules: [Int64: [ScheduleModel]] = [:]
    @Published var schedules: [Int64: [ScheduleModel]] = [:] {
        didSet {
            scheduleDates = Array(schedules.keys.sorted())
        }
    }
    
    @Published var scheduleDates: [Int64] = []
    
    init(observationFactory: IOSObservationFactory, dashboardFilterViewModel: DashboardFilterViewModel) {
        self.filterViewModel = dashboardFilterViewModel
        self.coreModel = CoreScheduleViewModel(dataRecorder: recorder)
        self.loadSchedules()
    }
    
    func start(scheduleId: String) {
        coreModel.start(scheduleId: scheduleId)
    }
    
    func pause(scheduleId: String) {
        coreModel.pause(scheduleId: scheduleId)
    }
    
    func stop(scheduleId: String) {
        coreModel.stop(scheduleId: scheduleId)
    }
    
    func applyFilters() {
        filterViewModel.setDateFilterValue()
        filterViewModel.setObservationTypeFilters()
        schedules = filterViewModel.coreModel.applyFilter(scheduleModelList: originalSchedules.convertToKotlinLong()).converttoInt64()
    }
    
    func loadSchedules() {
        coreModel.onScheduleModelListChange { [weak self] scheduleMap in
            if let self {
                self.schedules = scheduleMap.reduce([:]) { partialResult, pair -> [Int64: [ScheduleModel]] in
                    var result = partialResult
                    result[Int64(truncating: pair.key)] = pair.value
                    return result
                }
                self.originalSchedules = self.schedules
            }
        }
    }
    
    func loadRunningSchedules() {
        coreModel.getRunningSchedules { [weak self] scheduleMap in
            if let self {
                self.runningSchedules = scheduleMap.reduce([:]) { partialResult, pair -> [Int64: [ScheduleModel]] in
                    var result = partialResult
                    result[Int64(truncating: pair.key)] = pair.value
                    return result
                }
            }
        }
    }
    
    func loadCompletedSchedules() {
        coreModel.getCompletedSchedules { [weak self] scheduleMap in
            if let self {
                self.completedSchedules = scheduleMap.reduce([:]) { partialResult, pair -> [Int64: [ScheduleModel]] in
                    var result = partialResult
                    result[Int64(truncating: pair.key)] = pair.value
                    return result
                }
            }
        }
    }
    
    func getSchedules(key: Int64, type: ScheduleListType) -> [ScheduleModel] {
        if type == ScheduleListType.running {
            return runningSchedules[key] ?? []
        } else if type == ScheduleListType.completed {
            return completedSchedules[key] ?? []
        } else {
            return schedules[key] ?? []
        }
    }
}

extension Dictionary<KotlinLong, [ScheduleModel]> {
    func converttoInt64() -> [Int64: [ScheduleModel]] {
        reduce([:]) { partialResult, pair -> [Int64: [ScheduleModel]] in
            var result = partialResult
            result[Int64(truncating: pair.key)] = pair.value
            return result
        }
    }
}

extension Dictionary<Int64, [ScheduleModel]> {
    func convertToKotlinLong() -> [KotlinLong: [ScheduleModel]] {
        reduce([:]) { partialResult, pair -> [KotlinLong: [ScheduleModel]] in
            var result = partialResult
            result[KotlinLong(value: pair.key)] = pair.value
            return result
        }
    }
}
