# SHAHRVAND documentation

These documents describe the repository as it exists on the `uiux/v1-prototype` branch. They are based on the source files and GitHub Actions runs inspected on **2026-11-24**.

## Documents

- [Technical status](TECHNICAL_STATUS.md) — repository branches, implemented components, current behaviour, validation evidence, and known limitations.
- [Architecture](ARCHITECTURE.md) — Android, browser, admin mock, local storage, and deployment boundaries.
- [Build and deployment](BUILD_AND_DEPLOYMENT.md) — exact build requirements, commands, CI workflows, and GitHub Pages notes.
- [UI/UX iteration notes](UI_UX_V1.md) — historical iteration notes; not the authoritative source for current technical values.

## Important scope note

The default branch is `main`. The Android app, browser prototype, admin mock, and this documentation set are on `uiux/v1-prototype`. That branch has not been merged into `main`. Do not assume that files visible on this branch are present in the default branch or currently served by GitHub Pages.

## Source of truth

For implementation details, inspect the code and configuration linked from the technical documents. A feature is not considered server-backed merely because a UI screen exists. The current prototype uses local device/browser state; there is no connected application backend, remote authentication, evidence upload, or production leaderboard.
