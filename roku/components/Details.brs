sub init()
    m.backdrop = m.top.findNode("backdrop")
    m.scrim = m.top.findNode("scrim")
    m.title = m.top.findNode("title")
    m.meta = m.top.findNode("meta")
    m.genres = m.top.findNode("genres")
    m.description = m.top.findNode("description")
    m.cast = m.top.findNode("cast")
    m.playBtn = m.top.findNode("playBtn")

    m.backdrop.translation = [0, 0]
    m.backdrop.width = 1920
    m.backdrop.height = 620
    m.backdrop.uri = ThemeString("posterPlaceholder")

    m.scrim.translation = [0, 0]
    m.scrim.width = 1920
    m.scrim.height = 620

    padding = ThemeNumber("padding")
    m.title.color = ThemeColor("textPrimary")
    m.title.font = ThemeFont(GetTheme().fonts.header, 64)
    m.title.translation = [padding, 660]
    m.title.width = 1400

    m.meta.color = ThemeColor("textSecondary")
    m.meta.font = ThemeFont(GetTheme().fonts.subheader, 30)
    m.meta.translation = [padding, 760]
    m.meta.width = 1400

    m.genres.color = ThemeColor("primary")
    m.genres.font = ThemeFont(GetTheme().fonts.subheader, 28)
    m.genres.translation = [padding, 820]
    m.genres.width = 1400

    m.description.color = ThemeColor("textPrimary")
    m.description.font = ThemeFont(GetTheme().fonts.body, 28)
    m.description.translation = [padding, 880]
    m.description.width = 1400
    m.description.height = 320

    m.cast.color = ThemeColor("textSecondary")
    m.cast.font = ThemeFont(GetTheme().fonts.small, 24)
    m.cast.translation = [padding, 1220]
    m.cast.width = 1400
    m.cast.height = 160

    m.playBtn.translation = [padding, 970]
    m.playBtn.width = 360
    m.playBtn.height = 80
    m.playBtn.focusedColor = ThemeColor("primary")
    m.playBtn.observeField("buttonSelected", "onPlay")

    m.top.observeField("item", "onItemChanged")
    m.resumeSeconds = 0
    m.playBtn.setFocus(true)
end sub

sub onItemChanged()
    item = m.top.item
    if item = invalid then return

    title = ItemDetailsTitle(item)
    m.title.text = title

    meta = ""
    if item.year <> invalid and item.year > 0
        meta = item.year.ToStr()
    end if
    if item.rating <> invalid and item.rating > 0
        if meta <> "" then meta = meta + "  |  "
        meta = meta + FormatRating(item.rating)
    end if
    if item.media_type = "episode"
        if meta <> "" then meta = meta + "  |  "
        s = item.season
        e = item.episode
        if s <> invalid and e <> invalid
            meta = meta + "Season " + s.ToStr() + ", Episode " + e.ToStr()
        end if
    end if
    m.meta.text = meta

    if item.genres <> invalid and item.genres <> ""
        m.genres.text = item.genres
    end if

    if item.description <> invalid and item.description <> ""
        m.description.text = item.description
    else
        m.description.text = "No description available."
    end if

    if item.cast <> invalid and item.cast <> ""
        m.cast.text = "Cast: " + item.cast
    end if

    ' Backdrop
    backdropUrl = ApiAssetUrl(item.id, "landscape.jpg")
    if backdropUrl <> "" then m.backdrop.uri = backdropUrl

    ' Resume support
    m.resumeSeconds = 0
    playback = ApiGetPlayback(item.id)
    if playback.json <> invalid
        ts = playback.json.timestamp
        if ts <> invalid and ts > 0
            m.resumeSeconds = Int(ts)
        end if
    end if
    if m.resumeSeconds > 0
        m.playBtn.text = "Resume"
    else
        m.playBtn.text = "Play"
    end if
end sub

sub onPlay()
    item = m.top.item
    if item = invalid then return
    m.top.result = { action: "play", item: item, resume: m.resumeSeconds }
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if key = "back"
        m.top.result = { action: "back" }
        return true
    end if
    return false
end function

function ItemDetailsTitle(item as Object) as String
    if item = invalid then return ""
    if item.media_type = "episode" and item.show_title <> invalid and item.show_title <> ""
        base = item.show_title
        s = item.season
        e = item.episode
        if s <> invalid and e <> invalid
            return base + " S" + Right("0" + s.ToStr(), 2) + " E" + Right("0" + e.ToStr(), 2)
        end if
        return base
    end if
    if item.title <> invalid then return item.title
    return ""
end function

function FormatRating(rating as Float) as String
    if rating = invalid then return ""
    return "Rating " + rating.ToStr()
end function
