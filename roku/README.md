# SunSet Roku Channel

A SceneGraph client for SunSet, the self-hosted media server. Browse your
libraries, search, resume playback, subtitles and more — all from the couch.

## Features

- **Connect to any SunSet server** — enter your server's URL on first launch
- **Login** with your existing SunSet account (session is remembered)
- **Home** — Continue Watching, Recently Added, Genre rows, and one row per
  library
- **Search** across all titles
- **Details** — backdrop, description, cast, rating, genres, Resume/Play
- **Playback** with resume support and progress synced back to the server
  - MP4, MKV (H.264/AAC), WebM and HLS via the backend's stream endpoint
  - SRT/VTT subtitles served from the backend
- **Themable UI** — every color, font, size and image is driven by a JSON
  "stylesheet" so you can restyle the channel like a CSS plugin.
  See [`themes/README.md`](themes/README.md).

## Layout

```
manifest            Channel metadata (title, splash, artwork)
source/
  main.brs          Entry point
  api.brs           API client + session helpers (Roku registry)
  theme.brs         Theme loader + helpers
components/
  RootScene         Navigation hub / screen stack
  ServerSetup       Server URL entry
  Login             Sign in
  Home              RowList browsing
  MediaPoster       Row item (poster + title + focus ring)
  Details           Item details + play/resume
  VideoScene        Video playback + subtitles
  Search            Search + results
  HomeLoader        Background task that aggregates home rows
  ApiTask           Background HTTP task
themes/
  default.json      Default stylesheet
  README.md         Theme/"CSS plugin" guide
images/             Channel artwork + placeholder posters
```

## Sideloading

1. Zip the contents of this folder (the `manifest` must be at the zip root).
2. Enable **Developer mode** on your Roku (Home ×3, up ×2, right ×3, etc. or
   via the Roku settings) and note the IP + dev password.
3. Push the zip:

   ```
   curl -f -u roku:roku -F "mysubmit=Install" -F "archive=@sunset.zip" \
     http://<roku-ip>/plugin_install
   ```

   or use the **Roku Developer Suite** VS Code extension.

4. Open the channel; the first screen asks for your SunSet server URL.

## Notes

- The channel talks to the server over plain HTTP by default (typical for a
  home network). It is set up to use the same API as the web app, so it works
  against any SunSet server at `http://<host>:7867`.
- MKV files play when the video track is H.264/AAC (Roku's built-in support).
  Audio-only or exotic codecs require remuxing/transcoding on the server.
- Playback progress is saved to the server every ~10s, so you can resume on
  any other SunSet client.
