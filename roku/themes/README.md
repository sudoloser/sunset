# SunSet Roku Themes ("CSS plugins")

SceneGraph has no CSS, so SunSet styles its entire UI from a single JSON
stylesheet instead. The theme files are your "plugin": edit them to restyle
the whole channel — colors, fonts, layout sizes and image paths — without
touching any BrightScript.

## Files

| File | Purpose |
| --- | --- |
| `default.json` | Bundled default theme. Don't edit this if you want to keep upstream updates. |
| `theme.json` | **Optional override.** Create this file. Any key you set here is deep-merged over the defaults. |

Sideload the channel with your `theme.json` to apply a custom look. The merge
is per-key, so you only need to include the values you want to change.

## Schema

```jsonc
{
  "name": "My Theme",        // just a label, not used by the app
  "colors": {
    "background": "#000000",     // app background
    "surface": "#141414",        // panels / cards
    "primary": "#E50914",        // brand + focus highlight
    "onPrimary": "#FFFFFF",
    "textPrimary": "#FFFFFF",
    "textSecondary": "#B3B3B3",
    "border": "#333333",
    "focusBorder": "#E50914",    // selected poster ring
    "overlay": "#80000000",      // details scrim
    "error": "#FF5252"
  },
  "fonts": {
    "header": "LargeBoldSystemFont",   // Roku system font names, e.g.
    "subheader": "MediumBoldSystemFont",// LargeBoldSystemFont, MediumBoldSystemFont,
    "body": "MediumSystemFont",        // LargeSystemFont, MediumSystemFont,
    "small": "SmallSystemFont"         // SmallSystemFont
  },
  "layout": {
    "padding": 60,                // left/right gutter
    "topOffset": 150,             // home grid start Y
    "posterWidth": 240,
    "posterHeight": 360,
    "itemSpacing": 20,            // gap between posters in a row
    "rowSpacing": 30,             // gap between rows
    "posterTitleHeight": 40,
    "posterTitleFontSize": 24,
    "titleFontSize": 56,
    "subtitleFontSize": 30,
    "bodyFontSize": 26,
    "overhangHeight": 120,
    "buttonWidth": 320,
    "buttonHeight": 72
  },
  "images": {
    "logo": "",                               // optional logo overlay
    "posterPlaceholder": "pkg:/images/poster-placeholder.png",
    "splash": "pkg:/images/splash-screen_fhd.png"
  }
}
```

## Example: a light theme

```json
{
  "colors": {
    "background": "#F5F5F7",
    "surface": "#FFFFFF",
    "primary": "#0A84FF",
    "focusBorder": "#0A84FF",
    "textPrimary": "#1D1D1F",
    "textSecondary": "#6E6E73"
  }
}
```

Place that in `themes/theme.json`, sideload, done.

## Notes

- All colors are 6-digit hex (`#RRGGBB`).
- Fonts use Roku's built-in system font names; custom fonts can be added by
  bundling a TTF and pointing `images.fonts.<name>` at `pkg:/fonts/...`
  (not yet wired to every label — system fonts cover the current layout).
- Everything reads the theme at screen `init()` time, so a theme change only
  needs a channel restart.
