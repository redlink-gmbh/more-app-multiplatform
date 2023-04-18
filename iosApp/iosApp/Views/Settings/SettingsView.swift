//
//  SettingsView.swift
//  iosApp
//
//  Created by Julia Mayrhauser on 08.03.23.
//  Copyright © 2023 Redlink GmbH. All rights reserved.
//

import SwiftUI
import shared

struct SettingsView: View {
    @StateObject var viewModel: SettingsViewModel
    @State var exitButton = Color.more.important
    
    private let stringTable = "SettingsView"
    private let navigationStrings = "Navigation"
    
    var body: some View {
        MoreMainBackgroundView {
            VStack(alignment: .leading) {
                Text(String.localizedString(forKey: "settings_text", inTable: stringTable, withComment: "information about accepted permissions"))
                    .foregroundColor(.more.secondary)
                    .padding(.bottom)
                MoreActionButton(disabled: .constant(false)) {
                    viewModel.reloadStudyConfig()
                } label: {
                    Text(String.localizedString(forKey: "refresh_study_config", inTable: stringTable, withComment: "button to refresh study configuration"))
                }
                if let permissions = viewModel.permissionModel {
                    ConsentList(permissionModel: .constant(permissions))
                        .padding(.top)
                }
                BasicNavLinkButton(backgroundColor: $exitButton) {
                    ExitStudyLevelOneView(viewModel: viewModel)
                } label: {
                    Text(String.localizedString(forKey: "leave_study", inTable: stringTable, withComment: "button to refresh study configuration")).foregroundColor(Color.more.white)
                }.padding(.bottom)
                
                
                Spacer()
            }
        } topBarContent: {
            EmptyView()
        }
        .customNavigationTitle(with: NavigationScreens.settings.localize(useTable: navigationStrings, withComment: "Settings Screen"))
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(viewModel: SettingsViewModel())
    }
}
