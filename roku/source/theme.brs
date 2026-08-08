' ***********************************************************
' Theme manager.
'
' The UI is styled entirely from the theme files, which act
' as a CSS-like stylesheet for SceneGraph:
'
'   pkg:/themes/default.json   - bundled default stylesheet
'   pkg:/themes/theme.json     - OPTIONAL user override ("plugin").
'
' Drop a theme.json into the themes/ folder (or replace
' default.json) to restyle the whole channel without touching
' any BrightScript: colors, fonts, layout and image paths.
' Any keys you include are merged over the defaults.
' ***********************************************************

function ThemeLoad() as Object
    g = GetGlobalAA()
    g.sunsetTheme = {}
    defaults = ReadAsciiFile("pkg:/themes/default.json")
    if defaults <> invalid and defaults.len() > 0
        parsed = ParseJson(defaults)
        if parsed <> invalid then g.sunsetTheme = parsed
    end if

    override = ReadAsciiFile("pkg:/themes/theme.json")
    if override <> invalid and override.len() > 0
        parsed = ParseJson(override)
        if parsed <> invalid then ThemeMerge(g.sunsetTheme, parsed)
    end if
    return g.sunsetTheme
end function

sub ThemeMerge(dst as Object, src as Object)
    for each key in src
        if type(src[key]) = "roAssociativeArray" and type(dst[key]) = "roAssociativeArray"
            ThemeMerge(dst[key], src[key])
        else
            dst[key] = src[key]
        end if
    end for
end sub

function GetTheme() as Object
    g = GetGlobalAA()
    if g.sunsetTheme = invalid then return ThemeLoad()
    return g.sunsetTheme
end function

function ThemeColor(key as String) as String
    colors = GetTheme().colors
    if colors <> invalid and colors[key] <> invalid then return colors[key]
    return "#FFFFFF"
end function

function ThemeNumber(key as String) as Integer
    layout = GetTheme().layout
    if layout <> invalid and layout[key] <> invalid then return layout[key]
    return 0
end function

function ThemeString(key as String) as String
    images = GetTheme().images
    if images <> invalid and images[key] <> invalid then return images[key]
    return ""
end function

' Returns an roFont for the given system font name + size, or
' a fallback default font.
function ThemeFont(name as String, size as Integer) as Object
    registry = CreateObject("roFontRegistry")
    bold = name.instr("Bold") > 0
    font = registry.GetDefaultFont(size, bold, false)
    if font <> invalid then return font
    return registry.GetDefaultFont(size, false, false)
end function
