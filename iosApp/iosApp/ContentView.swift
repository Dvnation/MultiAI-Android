import SwiftUI

struct ContentView: View {
    @State private var selectedTab = 0
    @State private var searchPrompt = ""

    var body: some View {
        ZStack {
            Color(red: 0.06, green: 0.06, blue: 0.10)
                .ignoresSafeArea()

            VStack(spacing: 20) {
                // Header Bar
                HStack {
                    Text("MultiAI")
                        .font(.system(size: 26, weight: .bold))
                        .foregroundColor(.white)

                    Spacer()

                    Text("iOS Build ⚡")
                        .font(.system(size: 12, weight: .semibold))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color(red: 0.06, green: 0.73, blue: 0.51).opacity(0.2))
                        .foregroundColor(Color(red: 0.06, green: 0.73, blue: 0.51))
                        .cornerRadius(12)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)

                // Platform Switcher Bar
                HStack(spacing: 12) {
                    PlatformPill(name: "GEMINI", color: Color(red: 0.26, green: 0.52, blue: 0.96), isSelected: selectedTab == 0) { selectedTab = 0 }
                    PlatformPill(name: "HUGGING FACE", color: Color(red: 0.02, green: 0.02, blue: 0.05), isSelected: selectedTab == 1) { selectedTab = 1 }
                    PlatformPill(name: "GROK", color: Color(red: 0.39, green: 0.40, blue: 0.95), isSelected: selectedTab == 2) { selectedTab = 2 }
                    PlatformPill(name: "JUDGE AI", color: Color(red: 0.55, green: 0.36, blue: 0.96), isSelected: selectedTab == 3) { selectedTab = 3 }
                }
                .padding(.horizontal, 16)

                Spacer()

                // Central Card
                VStack(spacing: 16) {
                    Text("🤖")
                        .font(.system(size: 54))

                    Text("MultiAI for iOS")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.white)

                    Text("Compiled via GitHub Actions Cloud macOS runner.")
                        .font(.system(size: 14))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                }
                .padding(28)
                .background(Color(red: 0.12, green: 0.12, blue: 0.18))
                .cornerRadius(24)
                .padding(.horizontal, 20)

                Spacer()
            }
        }
    }
}

struct PlatformPill: View {
    let name: String
    let color: Color
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(name)
                .font(.system(size: 11, weight: .bold))
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(isSelected ? color : Color.white.opacity(0.1))
                .foregroundColor(.white)
                .cornerRadius(16)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
