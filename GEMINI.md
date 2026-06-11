# SunSet Media server

This server is a media server built in Rust & Typescript.
---
### Code Stack
- Backend is built using Rust
- Frontend is built using CSS & TypeScript
- Android is built using Kotlin & Jetpack Compose

backend is in @backend
frontend is in @frontend
Android is in @android
---
Always build both to make sure that it compiles correctly.

Build commands:
- Backend: `cargo check'
- Frontend `pnpm build`

NEVER try building Android locally.
---
When changing the frontend, you are REQUIRED to also port that change to the Android front end.
---

After each major feature, bug fix (not from same version bugs) add it to @changelog.md under the latest  version
if the fix is regarding ANYTHING other than android or the frontend/backend, it should not be included. currently, v0.1.0 is not out yet and will not be out for a bit, so do not add any changelog changes.
