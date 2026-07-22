# Minesweeper

A Compose Multiplatform recreation of classic Windows XP Minesweeper for Android, iOS, Desktop, Web/JS, and Web/Wasm.

The game includes the classic Beginner, Intermediate, and Expert fields, validated Custom fields, first-click safety, flags, question marks, chording, local best times, English/Simplified Chinese resources, and a responsive two-axis scrolling board. The gray bevels, digital counters, face states, flags, and mines are drawn from shared Compose code rather than copied bitmap assets.

## Controls

- Pointer: primary click reveals, secondary click changes the mark, and middle or primary+secondary click chords a revealed clue.
- Touch: tap reveals, long press changes the mark, and tapping a revealed clue chords it.
- Restart with the face button. F2 also restarts on targets that deliver the key event.
- After detonating a mine, choose Undo Last Move from the Game menu or use the visible undo button to return to the position before the losing move.

## Run

- Android: `./gradlew :androidApp:assembleDebug`
- Desktop: `./gradlew :desktopApp:run`
- Web/Wasm: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
- Web/JS fallback: `./gradlew :webApp:jsBrowserDevelopmentRun`
- iOS: open `iosApp` in Xcode and run the app from macOS.

iOS targets are enabled unless `local.properties` contains `minesweeper.enable.ios=false`.

## Verify

```text
./gradlew :shared:testAndroidHostTest :shared:jvmTest :shared:jsTest :shared:wasmJsTest
./gradlew :androidApp:assembleDebug :androidApp:lintDebug
./gradlew :desktopApp:createDistributable
./gradlew :webApp:jsBrowserProductionWebpack :webApp:wasmJsBrowserProductionWebpack :webApp:composeCompatibilityBrowserDistribution
```

iOS compilation and runtime validation require a macOS/Xcode environment and are not part of the local Windows gate.
