import SwiftUI
import shared

struct ContentView: View {
    @EnvironmentObject var viewModel: ContentViewModel
    var body: some View {
        VStack {
            if viewModel.hasCredentials {
                MainTabView()
                    .environmentObject(viewModel)
                    .onAppear{
                        viewModel.loadData()
                    }
            } else {
                MoreMainBackground {
                    VStack {
                        if viewModel.loginViewScreenNr == 0 {
                            LoginView(model: viewModel.loginViewModel)
                        } else {
                            ConsentView(viewModel: viewModel.consentViewModel)
                        }
                    }
                } topBarContent: {
                    EmptyView()
                }
            }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .environmentObject(ContentViewModel())
    }
}
