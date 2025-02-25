//
//  File.swift
//  iosApp
//
//  Created by Julia Mayrhauser on 07.03.23.
//  Copyright © 2023 Ludwig Boltzmann Institute for
//  Digital Health and Prevention - A research institute
//  of the Ludwig Boltzmann Gesellschaft,
//  Oesterreichische Vereinigung zur Foerderung
//  der wissenschaftlichen Forschung 
//  Licensed under the Apache 2.0 license with Commons Clause 
//  (see https://www.apache.org/licenses/LICENSE-2.0 and
//  https://commonsclause.com/).
//

import SwiftUI
import shared

struct ScheduleView: View {
    @StateObject var viewModel: ScheduleViewModel
    
    @EnvironmentObject var navigationModalState: NavigationModalState
    
    private let stringsTable = "ScheduleListView"
    var body: some View {
        VStack {
            
            ScrollView(.vertical) {
                if (viewModel.schedulesByDate.isEmpty) {
                    if viewModel.scheduleListType == ScheduleListType.running {
                        EmptyListView(text: "No running tasks currently".localize(withComment: "No running tasks in list", useTable: stringsTable))
                    } else if viewModel.scheduleListType == ScheduleListType.completed {
                        EmptyListView(text: "No tasks completed by now".localize(withComment: "No completed tasks in list", useTable: stringsTable))
                    } else {
                        EmptyListView(text: "No tasks to show".localize(withComment: "No tasks in list shown", useTable: stringsTable))
                    }
                } else {
                    LazyVStack(alignment: .leading, pinnedViews: .sectionHeaders) {
                        ForEach(viewModel.schedulesByDate.keys.sorted(), id: \.self) { key in
                            let schedules = viewModel.schedulesByDate[key, default: []]
                            if !schedules.isEmpty {
                                Section {
                                    ForEach(schedules, id: \.scheduleId) { schedule in
                                        VStack {
                                            ScheduleListItem(viewModel: viewModel, scheduleModel: schedule, showButton: viewModel.scheduleListType != .completed)
                                            Divider()
                                        }
                                    }
                                } header: {
                                    VStack(alignment: .leading) {
                                        BasicText(text: key.formattedString(), color: Color.more.primaryDark)
                                            .font(Font.more.headline)
                                        Divider()
                                    }.background(Color.more.secondaryLight)
                                }
                                .padding(.bottom)
                            } else {
                                EmptyView()
                            }
                        }
                    }
                    .background(Color.more.secondaryLight)
                }
            }
        }
        .onAppear {
            viewModel.viewDidAppear()
            //navigationModalState.closeView(screen: .taskDetails)
        }
        .onDisappear {
            viewModel.viewDidDisappear()
        }
    }
}

struct ScheduleView_Previews: PreviewProvider {
    static var previews: some View {
        MoreMainBackgroundView {
            ScheduleView(viewModel: ScheduleViewModel(scheduleListType: .all))
        } 
    }
}
