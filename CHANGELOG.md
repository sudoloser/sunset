# Changelog

All notable changes to the SunSet Media Server will be documented in this file.

## v0.2.0 - 6/9/2026
- Added search overlay with live filtering (episodes hidden).
- Added cloud-synced playback progress (localStorage + API).
- Added dark/light theme toggle (persisted in localStorage).
- Added subtitle styling settings (color, size, font, background, bold) with live preview.
- Added auto-updater that checks GitHub releases on startup.
- Added playback speed selector, Picture-in-Picture, and keyboard shortcuts.
- Added "Continue Watching" and "My List" rows.
- Added invite code system for multi-user.
- Added genre browsing rows on dashboard.
- Added cast avatars and star rating (5-star, localStorage).
- Added season 00 (Specials) support for TV show extras.
- Changed player controls to use `stopPropagation` to prevent overlay hiding on interaction.
- Changed player seek bar to support drag (mouse + touch).
- Changed sub-menus (speed, subtitles, episodes) to stay open while browsing.
- Changed library folder cards removed from "My Library" tab.
- Changed dashboard and search to filter out individual episodes (show series-level only).
- Changed filename/path references in docs to use `--tree` style with accurate regex patterns.
- Fixed player controls bugging out (event propagation to overlay toggle).
- Fixed `unzip` binary detection in Linux install script (wildcard search instead of exact name match).
- Fixed install script to auto-add `~/.sunset/bin` to `~/.bashrc`/`~/.zshrc`.
- Fixed install script URL to `/sunset/linux/install.sh`.
- Fixed TypeScript errors from removed props and imports.
- Internal version bumped to v0.2.0.

## v0.1.0 - 6/8/2026
- Initial release of SunSet Media Server.
- Immersive Netflix-style UI with Material 3 Expressive styling.
- TMDB integration for automatic metadata and poster art downloading.
- Rich Media Details view with cast, descriptions, and episode selector.
- Automatic subtitle recognition for `.srt` and `.vtt` files.
- Premium Video Player with playback persistence, touch controls, and subtitle support.
- Unified "Libraries" management system.
- Admin-only Settings panel.
- Multi-platform support (Linux x64/aarch64, Windows x64).
- Self-contained single-binary distribution.
