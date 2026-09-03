# Krushna AI (Kotlin + Jetpack Compose)

A modern native Android AI Assistant application supporting Marathi (मराठी), Hindi (हिंदी), and English with Gemini AI, real-time microphone voice input, and Text-To-Speech (TTS) audio replies.

## Features
- **Trilingual AI Chat**: Seamless communication in Marathi (मराठी), Hindi (हिंदी), and English.
- **Gemini 3.8 Flash AI**: Fast, intelligent conversational capabilities.
- **Voice Input (Speech-to-Text)**: Native Android `SpeechRecognizer` with `mr-IN`, `hi-IN`, and `en-US` locales.
- **Voice Replies (Text-to-Speech)**: Android `TextToSpeech` engine reads assistant answers aloud.
- **Modern Dark UI**: Designed for Android phones with AMOLED-friendly dark palette, smooth Compose animations, and Material 3 design tokens.
- **Quick Controls**: Send button, Clear Chat button, Voice Mute/Unmute, and Language Selector tabs.

## Project Structure
```
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/assistant/ai/
│   │   │   ├── MainActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── GeminiService.kt
│   │   │   │   └── model/ChatMessage.kt
│   │   │   ├── voice/VoiceManager.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/ChatScreen.kt
│   │   │   │   ├── viewmodel/ChatViewModel.kt
│   │   │   │   └── theme/{Color.kt, Theme.kt, Type.kt}
│   │   └── res/
│   │       ├── values/strings.xml
│   │       ├── values-mr/strings.xml (Marathi)
│   │       ├── values-hi/strings.xml (Hindi)
│   │       └── values/themes.xml
│   └── build.gradle.kts
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

## How to Run in Android Studio
1. Open **Android Studio** (Hedgehog or newer recommended).
2. Select **File > Open** and choose this project directory.
3. Add your Gemini API key:
   - In `gradle.properties` add: `GEMINI_API_KEY="your_api_key_here"`
   - Or set an environment variable `GEMINI_API_KEY`
4. Sync Gradle and press **Run (Shift + F10)** on an Android device or emulator running Android 8.0+ (API 26+).
5. Grant Microphone permission when prompted to speak in Marathi, Hindi, or English!
