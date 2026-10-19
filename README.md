# Tai Chi Flow

Offline Tai Chi training app — Kotlin, Jetpack Compose, Material 3, Room, DataStore. No account, no network, no payments.

## Open & run
1. Android Studio (Meerkat 2024.3.1 or newer) → **Open** this folder. Let Gradle sync (it downloads Gradle 8.11.1 via the wrapper properties).
2. If Studio asks for a Gradle wrapper jar, run once in the project folder: `gradle wrapper --gradle-version 8.11.1`.
3. Run the `app` configuration on a device/emulator (Android 7.0+, API 24).

## Build the release AAB
- **Android Studio:** Build › Generate Signed App Bundle / APK › Android App Bundle → pick or create your upload key.
- **Command line:** copy `keystore.properties.example` to `keystore.properties`, fill in your keystore details, then `./gradlew :app:bundleRelease`.
  Output: `app/build/outputs/bundle/release/app-release.aab`.
- **GitHub Actions:** `.github/workflows/build-aab.yml` builds an (unsigned unless configured) bundle on tag push or manual run.

Package name is set in one place: `applicationId` in `app/build.gradle.kts` (the Kotlin `namespace` can stay as is).

## Architecture
- `domain/engine` — all logic: PracticeEngine, BalanceEngine, RhythmEngine, BreathingEngine, ProgressEngine, StreakEngine, RecommendationEngine, AchievementEngine (+ GoalEngine, RecordsEngine), FlowPrograms.
- `data` — Room database, DataStore preferences, catalogs (45 movements, 28 sessions, 17 daily challenges, 13 weekly goals, 25 achievements), demo seeder, repository.
- `ui` — Compose screens + ViewModels. Screens only render state and forward input.

## Notes
- First launch seeds demo history (Profile › Reset data clears everything; Profile › Load demo data restores it).
- Guided-session scores are based on completion, pauses and skips — the app does not use motion sensors.
- Challenge scores come from touch timing/control games; they are practice metrics, not medical measurements.
