# CAA — Comunicador Aumentativo y Alternativo (KMP)

App CAA para niño de 5 años con autismo no verbal. Android primero, iOS via Compose Multiplatform.

## Stack
- Kotlin Multiplatform + Compose Multiplatform
- Decompose (navegación) + Koin (DI)
- SQLDelight (persistencia pictogramas)
- TTS: `TextToSpeech` (Android) / `AVSpeechSynthesizer` (iOS) vía `expect/actual`

## Estructura
```
composeApp/
  src/
    commonMain/         UI + dominio + datos compartidos
    androidMain/        TTS + DB driver + entry
    iosMain/            TTS + DB driver + entry
iosApp/                 Wrapper SwiftUI
```

## Funcionalidades
- Grid de pictogramas con `LazyVerticalGrid` (botones grandes, alto contraste)
- Barra de oración acumulativa (tap → añade → botón Hablar reproduce)
- Modo edición tras `ParentalGate` (suma matemática 11–19)
- Debounce 350ms en taps para bloqueo de toques accidentales
- Layout landscape fijo, sin animaciones distractoras

## Build
- Android: `./gradlew :composeApp:assembleDebug`
- iOS: abrir `iosApp/iosApp.xcodeproj` (crear con Xcode añadiendo `iosApp/iosApp/*.swift` + Info.plist y framework `ComposeApp`)

## Próximos pasos
- Generar wrapper Gradle (`gradle wrapper --gradle-version 8.9`)
- Reemplazar placeholder de letras por SVGs/PNGs reales en `composeResources/drawable`
- Categorías iniciales (alimentación, emociones, juegos)
- Backup/restore de la librería
- Voces personalizadas / ajuste de pitch para voz infantil
