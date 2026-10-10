# Notes for Claude Code sessions on this repo

## Android Gradle builds and emulators may be unavailable in a sandbox
A native build needs `dl.google.com` (Google's Maven) and an emulator needs `/dev/kvm`. Check in one step:
`curl -sS -o /dev/null -w '%{http_code}\n' https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml; ls /dev/kvm`.
`000`/403 plus "No such file" means blocked: do not install Gradle bits or retry. Get evidence from
`.github/workflows/android-ci.yml` instead: push, then read the job logs once the job has ended (logs 404 while a
job is running; do not cancel a run to peek).

## Build conventions that are easy to break
- AGP 9 uses built-in Kotlin: do not apply `org.jetbrains.kotlin.android`. The Kotlin plugins in
  `build.gradle.kts` (serialization, compose) must stay on the same version; Dependabot groups them.
- KSP is versioned independently of Kotlin since 2.3.0.
- `targetSdk` is deliberately lower than `compileSdk`; raise it as its own change because it opts into newer
  runtime behaviour.
- Dependency locking is on for all configurations: after changing a dependency run
  `./gradlew :app:dependencies --write-locks` and commit `app/gradle.lockfile` (osv-scanner reads it).
- ktlint (Android Studio style, 100 columns) runs in CI: `./gradlew ktlintCheck`; `./gradlew ktlintFormat` fixes
  most findings. Imports are plain lexicographic. Composable function names are exempt from the naming rule.

## Networking and offline patterns
- Retrofit throws `HttpException` (not `IOException`) for non-2xx responses. Catch both. 401/403/422 are permanent
  (never retry); 429 and 5xx are transient. `ActionRepositoryHttpExceptionTest` exercises this with `MockWebServer`.
- Never raise the OkHttp logging level above BASIC: it would log the configured auth header.
- `SyncQueueWorker` is enqueued under a unique name; a permanently failed item must be dequeued so the worker does
  not retry forever.
- The Room/DAO logic is tested with in-memory fakes (no Robolectric needed); keep new DAOs faked the same way.

## Emulator tests
Tests that make the app request a runtime permission need a helper that taps "Allow" on the system dialog (see
the equivalent `allow-permission-dialogs.sh` pattern in the Flutter template). An instrumented test that finishes
force-stops the app, so checks that must outlive the process (after a reboot) cannot be instrumented tests.

## Project memory
Durable context goes in `docs/project-memory/`.
