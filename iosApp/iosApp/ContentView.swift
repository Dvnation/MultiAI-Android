import SwiftUI

struct ChatMessage: Identifiable {
    let id = UUID()
    let text: String
    let isFromAI: Bool
}

struct ContentView: View {
    @State private var selectedTab = 0
    @State private var inputPrompt = ""
    @State private var isLoading = false
    @State private var messages: [ChatMessage] = []

    var activePlatformName: String {
        switch selectedTab {
        case 0: return "Gemini"
        case 1: return "Hugging Face"
        case 2: return "Grok"
        default: return "Judge AI"
        }
    }

    var body: some View {
        ZStack {
            Color(red: 0.06, green: 0.06, blue: 0.10)
                .ignoresSafeArea()

            VStack(spacing: 14) {
                // Header Bar
                HStack {
                    Text("MultiAI")
                        .font(.system(size: 24, weight: .bold))
                        .foregroundColor(.white)

                    Spacer()

                    Text("KMP Connected ⚡")
                        .font(.system(size: 11, weight: .semibold))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color(red: 0.06, green: 0.73, blue: 0.51).opacity(0.2))
                        .foregroundColor(Color(red: 0.06, green: 0.73, blue: 0.51))
                        .cornerRadius(12)
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)

                // Platform Selector
                HStack(spacing: 8) {
                    PlatformPill(name: "GEMINI", color: Color(red: 0.26, green: 0.52, blue: 0.96), isSelected: selectedTab == 0) { selectedTab = 0 }
                    PlatformPill(name: "HUGGING FACE", color: Color(red: 0.02, green: 0.02, blue: 0.05), isSelected: selectedTab == 1) { selectedTab = 1 }
                    PlatformPill(name: "GROK", color: Color(red: 0.39, green: 0.40, blue: 0.95), isSelected: selectedTab == 2) { selectedTab = 2 }
                    PlatformPill(name: "JUDGE AI", color: Color(red: 0.55, green: 0.36, blue: 0.96), isSelected: selectedTab == 3) { selectedTab = 3 }
                }
                .padding(.horizontal, 12)

                // Chat Messages View
                ScrollViewReader { scrollView in
                    ScrollView {
                        VStack(spacing: 12) {
                            if messages.isEmpty {
                                VStack(spacing: 12) {
                                    Text("🤖")
                                        .font(.system(size: 48))
                                    Text("Ask \(activePlatformName)")
                                        .font(.system(size: 18, weight: .bold))
                                        .foregroundColor(.white)
                                    Text("Type your prompt below to interact with \(activePlatformName) AI.")
                                        .font(.system(size: 13))
                                        .foregroundColor(.gray)
                                        .multilineTextAlignment(.center)
                                }
                                .padding(30)
                                .background(Color(red: 0.12, green: 0.12, blue: 0.18))
                                .cornerRadius(20)
                                .padding(.top, 40)
                            } else {
                                ForEach(messages) { msg in
                                    HStack {
                                        if !msg.isFromAI { Spacer() }
                                        Text(msg.text)
                                            .font(.system(size: 15))
                                            .padding(14)
                                            .background(msg.isFromAI ? Color(red: 0.16, green: 0.16, blue: 0.24) : Color(red: 0.39, green: 0.40, blue: 0.95))
                                            .foregroundColor(.white)
                                            .cornerRadius(16)
                                        if msg.isFromAI { Spacer() }
                                    }
                                    .padding(.horizontal, 16)
                                }
                            }

                            if isLoading {
                                HStack {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    Text("\(activePlatformName) is thinking...")
                                        .font(.system(size: 13))
                                        .foregroundColor(.gray)
                                    Spacer()
                                }
                                .padding(.horizontal, 20)
                                .padding(.vertical, 8)
                            }
                        }
                    }
                }

                // Prompt Input Box
                HStack(spacing: 10) {
                    TextField("Ask \(activePlatformName)...", text: $inputPrompt)
                        .padding(12)
                        .background(Color(red: 0.14, green: 0.14, blue: 0.20))
                        .foregroundColor(.white)
                        .cornerRadius(16)

                    Button(action: sendPrompt) {
                        Image(systemName: "paperplane.fill")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)
                            .padding(12)
                            .background(Color(red: 0.06, green: 0.73, blue: 0.51))
                            .cornerRadius(16)
                    }
                    .disabled(inputPrompt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 12)
            }
        }
    }

    func sendPrompt() {
        let trimmed = inputPrompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        messages.append(ChatMessage(text: trimmed, isFromAI: false))
        inputPrompt = ""
        isLoading = true

        // Async AI response
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
            let replyText = "\(activePlatformName) AI response to: \"\(trimmed)\""
            messages.append(ChatMessage(text: replyText, isFromAI: true))
            isLoading = false
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
                .font(.system(size: 10, weight: .bold))
                .padding(.horizontal, 8)
                .padding(.vertical, 6)
                .background(isSelected ? color : Color.white.opacity(0.1))
                .foregroundColor(.white)
                .cornerRadius(14)
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
