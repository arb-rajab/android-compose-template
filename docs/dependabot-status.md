# Dependabot status

_Last updated: 2026-10-10. Starter template extracted from `lexicon-android`; update when the state changes._

## Configuration

- Ecosystems covered: gradle (`/`), github-actions (`/`).
- Grouping: Kotlin plugins grouped as `kotlin` (they must move together).
- Schedule: weekly.
- Ignore rules: none.

## State at last update

- Open Dependabot PRs: 0 (each merged or closed only after reading its checks).
- Default-branch CI: green at last check.
- Repository settings (read live 2026-10-10): public, `is_template` true, description set, default branch `main` (the only branch kept), secret scanning, push protection and Dependabot security updates enabled, dependency graph on (SBOM endpoint 200). Non-provider secret patterns and validity checks stay off: GitHub does not offer them on this user-owned public repo.
- Open in the 2026-10-10 loop (set by the repo owner through the API; the session's proxy blocks repository-settings writes): private vulnerability reporting (read: disabled), repository topics (none), `delete_branch_on_merge` (off). Alert state (Dependabot, code scanning, secret scanning) and fork-PR approval are read with the owner's token; see Notes once recorded.

## Time-limited exemptions

- None.

## Notes

- Merge policy (deliberate choice by the repo owner, the same blanket policy as the application repos): every PR, major-version dependency bumps included, is squash-merged as soon as all of its required checks are green, confirmed per PR. This repo is a code showcase with no business or sensitive dependency, so green checks are the only gate. Red, pending or conflicted PRs are fixed or closed instead.
- `main` protection (2026-10-10, read back): pull request required (0 approvals), enforced for admins, force pushes and deletions blocked, "require branches to be up to date" off (`strict` false) on purpose. Nobody pushes to `main` directly; status-log updates go through a PR too.
- Required contexts: `lint-and-test`, `instrumented-tests (30)`, `Secret scan (gitleaks)`. Nothing else is required on purpose: osv-scanner, npm audit, dependency review and CodeQL depend on a moving advisory database or run only on some events.
- Closed unmerged on 2026-10-10, each with an explanatory comment: #2 and #3 (superseded by #4, which bumped both with a lockfile regenerated on a runner). If Dependabot recreates one, apply the merge policy; the closure is not re-raised. Dependabot does not re-propose a major after its PR is closed, so a later upgrade is a manual change.
- Dependency locking is on: a Gradle bump also needs `app/gradle.lockfile` regenerated (`./gradlew :app:dependencies --write-locks`). The sandbox cannot run Gradle, so regenerate it on a runner (a temporary workflow on the PR branch, removed before merge), never by hand.
- #5 (Gradle wrapper 9.8.0 -> 9.8.1) merged 2026-10-10 with every required check green.

## Deferred (not re-raised each pass)

- Alert reads (Dependabot, code scanning, secret scanning) and Actions settings (fork-PR approval) are blocked for Claude sessions by the proxy; they are read and written with the repo owner's token on their own machine.
- The owner keeps the token's Administration, Contents and Secret scanning permissions and does not rotate it (deliberate, 2026-10-09, reconfirmed 2026-10-10 for the template repos).
