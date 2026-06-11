<div align="center">
  <img src="logo.png" alt="SunSet" width="120" />
  <h1 align="center">SunSet</h1>
  <p align="center">A personal media server — stream your movies & TV shows anywhere.</p>
</div>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#building">Building</a> •
  <a href="#documentation">Docs</a> •
  <a href="#license">License</a>
</p>

SunSet is a self-hosted media server with a Netflix-style UI. Drop your media folders in, SunSet indexes everything, fetches metadata + posters from TMDB, and gives you a beautiful streaming interface across all your devices.

---

### Features

- **Streaming** — Video with subtitle support, playback speed, PiP, keyboard shortcuts
- **Metadata** — Automatic TMDB fetching (posters, backdrops, logos, cast, ratings)
- **Library** — Smart scanning with movie & TV show parsing (S01E01, 1x01, S1E1)
- **Multi-user** — Invite code system with admin panel
- **Progress sync** — Cloud-synced across devices
- **Responsive** — Works on desktop, tablet, and mobile
- **Theming** — Dark & light themes with subtitle styling
- **Single binary** — Backend embeds the frontend; one file to deploy

---

### Quick Start

**Linux / macOS**
```bash
curl -sL https://sudoloser.github.io/sunset/linux/install.sh | bash
```

**Windows (PowerShell)**
```powershell
irm https://sudoloser.github.io/sunset/windows/install.ps1 | iex
```

Or grab the latest binary from the [releases page](https://github.com/sudoloser/sunset/releases).

```
./sunset-server
```

Open your browser and follow the setup wizard.

#### Beta builds

Want the latest changes before they're stable?

```bash
curl -sL https://sudoloser.github.io/sunset/linux/install-beta.sh | bash
```

Beta builds are published from the `pre` branch. They may be unstable.

---

### Building

#### Backend (Rust)

```bash
# Build the frontend first
cd frontend && pnpm install && pnpm build && cd ..

# Copy frontend dist into backend
cp -r frontend/dist backend/dist

# Build the server
cd backend && cargo build --release
```

#### Frontend (TypeScript)

```bash
cd frontend
pnpm install
pnpm build     # production build
pnpm dev       # dev server with hot reload
```

#### Android (Kotlin / Jetpack Compose)

Open `android/` in Android Studio, or build via CLI:

```bash
cd android
./gradlew assembleRelease
```

> Note: Release builds require signing. Set `KEYSTORE`, `KEYSTORE_ALIAS`, and `KEYSTORE_PASSWORD` in GitHub Secrets for CI.

---

### Project Structure

```
sunset/
├── backend/          # Rust (Axum) server — API + embedded frontend
│   ├── src/          # routes, db, scanner, tmdb, playback
│   └── Cargo.toml
├── frontend/         # TypeScript + CSS web UI
│   └── src/          # components, screens, player, store
├── android/          # Kotlin + Jetpack Compose Android app
│   └── app/src/main/java/dev/sudoloser/sunset/
├── .github/
│   ├── workflows/    # CI/CD — release, beta, android builds
│   └── ISSUE_TEMPLATE/
├── CHANGELOG.md
├── LICENSE           # MIT
└── README.md
```

> Website files (`index.html`, install scripts) are on the [`docs`](https://github.com/sudoloser/sunset/tree/docs) branch and served via GitHub Pages.

---

### Documentation

Full documentation is available on the [website](https://sudoloser.github.io/sunset/), including:

- [Getting Started](https://sudoloser.github.io/sunset/#docs)
- [Library Scanning](https://sudoloser.github.io/sunset/#docs)
- [Movie & Show Parsing](https://sudoloser.github.io/sunset/#docs)
- [Player Controls](https://sudoloser.github.io/sunset/#docs)
- [Configuration](https://sudoloser.github.io/sunset/#docs)

---

### Bug Reports

Found a bug? [Open an issue](https://github.com/sudoloser/sunset/issues/new/choose) — there's a template to help you write a good report.

---

### License

MIT © 2026 sudoloser. See [LICENSE](LICENSE) for details.
