<!--
Sync Impact Report
Version change: (initial) → 1.0.0
Modified principles: None (initial publication)
Added sections: Core Principles, Delivery Guardrails, Development Workflow, Governance
Removed sections: None
Templates requiring updates: ✅ .specify/templates/plan-template.md (constitution version reference)
Follow-up TODOs: None
-->

# Movie App Constitution

## Core Principles

### I. Feature Parity Restoration
- The app MUST deliver the layouts, flows, and feature scope documented in `DECOMPILED_INTEGRATION_PLAN.md` when compared to `c:/Users/aqeel/Downloads/MovieApp_java_source/`.
- Any divergence from the reference implementation MUST be tracked with a remediation item and resolved before release.
- Regression tests (manual or automated) MUST cover each restored screen before the feature is marked complete.
*Rationale: Protects the goal of recreating the original experience without drift or partial migrations.*

### II. Composable Architecture Discipline
- UI work MUST be implemented with Jetpack Compose using screen-specific composables and `viewmodel/` state holders.
- Navigation routes MUST be centralized in `ui/navigation/` to prevent orphaned flows.
- Shared UI logic MUST live in `ui/components/` or `utils/` instead of duplicating patterns across screens.
*Rationale: Keeps the modernized Compose architecture maintainable while mirroring legacy behavior.*

### III. Data Integrity & Supabase Compliance
- Supabase authentication, subtitle caching, and synchronization logic MUST call the providers under `data/` with validated inputs.
- Room database migrations MUST be authored and verified for every schema change, including upgrade/downgrade paths.
- Remote operations MUST implement retries and error surfacing consistent with `OptimizedBackendService` and logging standards.
*Rationale: Ensures the restored features remain reliable across online/offline modes and cloud sync.*

### IV. Observability & Quality Gates
- Each restored feature MUST ship with structured logging or analytics hooks that allow tracing of failures end-to-end.
- Pull requests MUST include verification steps (tests or manual checklists) demonstrating feature health.
- Critical flows (auth, playback, caching) MUST have automated tests or executable diagnostics before merging.
*Rationale: Prevents silent regressions while the codebase scales back up to the original complexity.*

### V. Accessible Cinematic UX
- UI must satisfy Material 3 accessibility guidelines: contrast ratios, focus order, and dynamic text sizing.
- Subtitle and playback settings MUST honor user preferences stored in DataStore and surface fallback options.
- Any new visual element MUST be reviewed against the reference resources directory to ensure fidelity.
*Rationale: Guarantees the cinematic experience is consistent and inclusive across devices.*

## Delivery Guardrails

- Dependencies MUST be declared in `build.gradle` with explicit version pins; surprise upgrades are prohibited without review.
- Feature branches MUST document Supabase environment variables and backend requirements in updated README or docs entries.
- APK builds MUST be smoke-tested on at least one physical device and one emulator before tagging a release.

## Development Workflow

- Every restoration effort MUST reference the relevant section of `DECOMPILED_INTEGRATION_PLAN.md` and create traceable tasks.
- Design parity reviews MUST include side-by-side screenshots or recordings comparing the Compose screen and the original UI.
- Feature completion requires: plan update → implementation → validation report → documentation refresh (README or docs/ as needed).

## Governance

- This constitution supersedes prior undocumented practices; deviations require written approval recorded in repository docs.
- Amendments MUST be proposed via pull request referencing observed issues, include a version bump rationale, and update affected templates.
- Compliance reviews occur at the close of each major integration milestone and before release candidates; non-compliant work cannot ship.
- Versioning follows semantic rules: MAJOR for breaking governance changes, MINOR for new principles/sections, PATCH for wording clarifications.

**Version**: 1.0.0 | **Ratified**: 2025-10-04 | **Last Amended**: 2025-10-04