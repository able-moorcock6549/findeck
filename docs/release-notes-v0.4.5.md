# Release Notes v0.4.5

## Summary

`v0.4.5` closes the current Paseo comparison execution plan for findeck.

This release focuses on making session switching, session detail, and long-history handling feel lighter and more deliberate across Web and Android, while also tightening a few mobile-detail ergonomics discovered during final testing.

## Highlights

- Completed the current `Paseo` comparison plan through `Phase 5`
- Shifted session detail entry toward lighter `summary + tail + history pagination` behavior
- Made Web and Android session switching feel more like entering another thread in the same workspace
- Improved long-history handling so recent content stays foregrounded and older history expands progressively
- Strengthened state language for transition, recovery, degraded sync, and settled output
- Made Android session detail lighter on first screen by folding repo status by default
- Folded Android memory citation output into a collapsible block instead of showing raw structured markup

## User-Facing Improvements

- Existing sessions enter through a steadier shell-first path instead of depending on one heavy full-detail read.
- Session lists now make `当前项目`、`当前会话`、`最近活跃` easier to distinguish.
- Long-history sessions keep the current turn and recent history closer to the user, with older history expanded in smaller steps.
- Recovery and degraded transport states now read more like process states than abrupt failures.
- Android session detail now uses a smaller default history window and a folded repo panel for a lighter first screen.

## Versioning

- Workspace packages: `0.4.5`
- Android `versionName`: `0.4.5`
- Android `versionCode`: `12`

## Validation

- `npm run build --workspace @findeck/shared`
- `npm run build --workspace @findeck/server`
- `npm run typecheck --workspace @findeck/web`
- `npm run build --workspace @findeck/web`
- `cd apps/android && ./gradlew :app:compileDebugKotlin`
- `cd apps/android && ./gradlew :app:assembleDebug`

## Release Docs

- [docs/paseo-gap-improvement-plan.md](/Users/fainal/Documents/GitHub/findeck/docs/paseo-gap-improvement-plan.md)
- [docs/session-transition-phase-summary.md](/Users/fainal/Documents/GitHub/findeck/docs/session-transition-phase-summary.md)
- [docs/session-transition-release-summary.md](/Users/fainal/Documents/GitHub/findeck/docs/session-transition-release-summary.md)
- [docs/session-transition-acceptance-checklist.md](/Users/fainal/Documents/GitHub/findeck/docs/session-transition-acceptance-checklist.md)

## Known Follow-Up

- Deeper long-history virtualization remains a post-release enhancement, not a blocker for this plan closure.
- Cross-platform status-component abstraction can continue to evolve after this release baseline.
- Voice compatibility on some Android device paths remains follow-up work outside this session-transition release focus.

## Delivery Artifact

- Dated APK output: `YYYY-MM-DD_findeck_v0.4.5.apk`
