import SwiftUI
import Shared

struct ProviderReply: Identifiable {
    let id = UUID()
    let name: String
    let text: String
}

struct ContentView: View {
    @AppStorage("multiai_gemini_key") private var geminiKey = ""
    @AppStorage("multiai_huggingface_key") private var huggingFaceKey = ""
    @AppStorage("multiai_openrouter_key") private var openRouterKey = ""
    @AppStorage("multiai_cerebras_key") private var cerebrasKey = ""
    @AppStorage("multiai_multisearch") private var multiSearchEnabled = false
    @State private var selectedTab = 0
    @State private var inputPrompt = ""
    @State private var isLoading = false
    @State private var replies: [ProviderReply] = []
    @State private var showSettings = false

    private let accent = Color(red: 0.38, green: 0.32, blue: 0.88)
    private var providerNames: [String] { ["Gemini", "Hugging Face", "OpenRouter", "Cerebras"] }
    private var activeProvider: String { providerNames[selectedTab] }

    var body: some View {
        NavigationStack {
            VStack(spacing: 14) {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("MultiAI").font(.system(size: 27, weight: .bold)).foregroundStyle(Color.primary)
                        Text("One prompt. Multiple perspectives.").font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button { showSettings = true } label: {
                        Image(systemName: "slider.horizontal.3").font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(accent).padding(11).background(accent.opacity(0.10)).clipShape(Circle())
                    }
                }
                .padding(.horizontal, 18).padding(.top, 8)

                Toggle(isOn: $multiSearchEnabled) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("MultiSearch").font(.system(size: 15, weight: .semibold))
                        Text(multiSearchEnabled ? "Searching all configured providers together" : "Using the selected provider")
                            .font(.caption).foregroundStyle(.secondary)
                    }
                }
                .tint(accent).padding(.horizontal, 18)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(providerNames.enumerated()), id: \.offset) { index, name in
                            Button { selectedTab = index } label: {
                                Text(name).font(.system(size: 12, weight: .semibold))
                                    .padding(.horizontal, 13).padding(.vertical, 9)
                                    .background(selectedTab == index ? accent : Color(.secondarySystemBackground))
                                    .foregroundStyle(selectedTab == index ? .white : .primary)
                                    .clipShape(Capsule())
                            }
                        }
                    }.padding(.horizontal, 16)
                }

                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        if replies.isEmpty {
                            ContentUnavailableView("Start a conversation", systemImage: "bubble.left.and.bubble.right",
                                description: Text("Ask \(multiSearchEnabled ? "all configured providers" : activeProvider) a question."))
                                .padding(.top, 45)
                        }
                        ForEach(replies) { reply in
                            VStack(alignment: .leading, spacing: 8) {
                                Text(reply.name.uppercased()).font(.caption.weight(.bold)).foregroundStyle(accent)
                                Text(reply.text).font(.system(size: 15)).foregroundStyle(.primary).textSelection(.enabled)
                            }
                            .padding(15).frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color(.secondarySystemBackground)).clipShape(RoundedRectangle(cornerRadius: 17))
                        }
                        if isLoading { HStack(spacing: 8) { ProgressView(); Text("Working…").font(.caption).foregroundStyle(.secondary) }.padding(.vertical, 8) }
                    }.padding(.horizontal, 16).padding(.vertical, 8)
                }

                HStack(spacing: 10) {
                    TextField("Message \(multiSearchEnabled ? "MultiAI" : activeProvider)…", text: $inputPrompt, axis: .vertical)
                        .lineLimit(1...5).padding(12)
                        .background(Color(.secondarySystemBackground)).clipShape(RoundedRectangle(cornerRadius: 16))
                    Button(action: sendPrompt) {
                        Image(systemName: "arrow.up").font(.system(size: 16, weight: .bold)).foregroundStyle(.white)
                            .frame(width: 44, height: 44).background(accent).clipShape(Circle())
                    }.disabled(inputPrompt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || isLoading)
                }.padding(.horizontal, 16).padding(.bottom, 10)
            }
            .background(Color(.systemBackground))
            .toolbar(.hidden, for: .navigationBar)
            .sheet(isPresented: $showSettings) { settingsSheet }
        }
        .tint(accent)
    }

    private var settingsSheet: some View {
        NavigationStack {
            Form {
                Section("Provider API keys") {
                    SecureField("Gemini API key", text: $geminiKey)
                    SecureField("Hugging Face token", text: $huggingFaceKey)
                    SecureField("OpenRouter API key", text: $openRouterKey)
                    SecureField("Cerebras API key", text: $cerebrasKey)
                }
                Section {
                    Text("Keys are stored locally on this device. Do not share screenshots containing them.")
                        .font(.caption).foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { showSettings = false } } }
        }
    }

    private func sendPrompt() {
        let prompt = inputPrompt.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !prompt.isEmpty, !isLoading else { return }
        inputPrompt = ""
        replies = []
        isLoading = true

        Task {
            var results: [ProviderReply] = []
            if multiSearchEnabled {
                async let gemini = AiService.shared.fetchGeminiAiResponse(prompt: prompt, apiKey: geminiKey)
                async let huggingFace = AiService.shared.fetchHuggingFaceAiResponse(prompt: prompt, apiKey: huggingFaceKey)
                async let openRouter = AiService.shared.fetchGrokAiResponse(prompt: prompt, apiKey: openRouterKey)
                async let cerebras = AiService.shared.fetchCerebrasAiResponse(prompt: prompt, apiKey: cerebrasKey)
                let values = await (gemini, huggingFace, openRouter, cerebras)
                results = [
                    ProviderReply(name: "Gemini", text: values.0),
                    ProviderReply(name: "Hugging Face", text: values.1),
                    ProviderReply(name: "OpenRouter", text: values.2),
                    ProviderReply(name: "Cerebras", text: values.3)
                ]
            } else {
                let result: String
                switch selectedTab {
                case 0: result = await AiService.shared.fetchGeminiAiResponse(prompt: prompt, apiKey: geminiKey)
                case 1: result = await AiService.shared.fetchHuggingFaceAiResponse(prompt: prompt, apiKey: huggingFaceKey)
                case 2: result = await AiService.shared.fetchGrokAiResponse(prompt: prompt, apiKey: openRouterKey)
                default: result = await AiService.shared.fetchCerebrasAiResponse(prompt: prompt, apiKey: cerebrasKey)
                }
                results = [ProviderReply(name: activeProvider, text: result)]
            }
            await MainActor.run {
                replies = results
                isLoading = false
            }
        }
    }
}

#Preview { ContentView() }
