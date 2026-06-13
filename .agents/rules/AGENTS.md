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
Always build the ones you changed to make sure that it compiles correctly.
if you edited the backend, run the backend command. if you edited the frontend, run the frontend command.
building android locally is not needed as i have it built using github actions.


Build commands:
- Backend: `cargo check'
- Frontend `pnpm build`

NEVER try building Android locally.
---
When changing the frontend, you are REQUIRED to also port that change to the Android front end.
---

After each major feature, bug fix (not from same version bugs) add it to @changelog.md under the latest  version
