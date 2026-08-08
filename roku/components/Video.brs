sub init()
    m.video = m.top.findNode("video")
    m.errorLabel = m.top.findNode("errorLabel")
    m.errorLabel.color = ThemeColor("error")
    m.errorLabel.font = ThemeFont(GetTheme().fonts.body, 28)
    m.errorLabel.horizAlign = "center"
    m.errorLabel.width = 1500
    m.errorLabel.wrap = true
    m.errorLabel.translation = [(1920 - 1500) / 2, 420]
    m.errorLabel.visible = false

    m.video.observeField("state", "onVideoState")
    m.video.observeField("position", "onPosition")
    m.video.observeField("errorCode", "onVideoError")

    m.savedOnce = false
    m.triedTranscode = false
    m.top.observeField("item", "onItemChanged")
end sub

sub onItemChanged()
    item = m.top.item
    if item = invalid then return

    m.triedTranscode = false

    content = CreateObject("roSGNode", "ContentNode")
    content.url = ApiStreamUrl(item.id)
    content.streamFormat = StreamFormatFor(item)
    content.title = ItemTitleForVideo(item)
    if item.description <> invalid then content.description = item.description

    ' Attach subtitle tracks if the backend has any
    m.subtitleNames = []
    subs = ApiGetSubtitles(item.id)
    if subs.json <> invalid and subs.json.count() > 0
        for each name in subs.json
            track = content.CreateChild("ContentNode")
            track.url = ApiSubtitleUrl(item.id, name)
            track.trackname = name
            track.tracktype = "subs"
            m.subtitleNames.push(name)
        end for
        content.subtitleconfig = { Trackname: m.subtitleNames[0] }
    end if

    m.video.content = content

    resume = m.top.resume
    if resume <> invalid and resume > 0
        m.video.position = resume
    end if

    m.video.setFocus(true)
    m.video.control = "play"
end sub

sub onVideoState()
    state = m.video.state
    if state = "playing"
        m.errorLabel.visible = false
    else if state = "finished"
        SavePlayback()
        m.top.result = { action: "back" }
    else if state = "error"
        if not m.triedTranscode
            m.triedTranscode = true
            RetryWithTranscode()
            return
        end if
        code = m.video.errorCode
        if code = invalid then code = 0
        m.errorLabel.text = "Playback failed (" + code.ToStr() + "). Press BACK to go back."
        m.errorLabel.visible = true
        m.video.setFocus(true)
    end if
end sub

' Some Roku models lack H.265/HEVC decode (or the file has DTS/AC3 audio).
' Retry once through the server's H.264 transcode, starting near the current position.
sub RetryWithTranscode()
    item = m.top.item
    if item = invalid or item.id = invalid then return

    start = 0
    if m.video.position <> invalid and m.video.position > 0
        start = Int(m.video.position)
    end if

    content = CreateObject("roSGNode", "ContentNode")
    content.url = ApiTranscodeUrl(item.id, start)
    content.streamFormat = "mp4"
    content.title = ItemTitleForVideo(item)
    if item.description <> invalid then content.description = item.description

    m.video.content = content
    m.video.setFocus(true)
    m.video.control = "play"
end sub

sub onPosition()
    ' throttle saves: only every ~10 seconds of playback
    cur = m.video.position
    if cur <> invalid and cur > 0
        last = m.lastSavePos
        if last = invalid or cur - last >= 10
            m.lastSavePos = cur
            SavePlayback()
        end if
    end if
end sub

sub SavePlayback()
    item = m.top.item
    if item = invalid then return
    if m.savedOnce and m.video.state <> "finished" then return
    m.savedOnce = true
    ApiSavePlayback(item.id, AppUserId(), m.video.position, m.video.duration, m.video.state = "playing")
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if key = "back"
        SavePlayback()
        m.top.result = { action: "back" }
        return true
    end if
    return false
end function

function ItemTitleForVideo(item as Object) as String
    if item = invalid then return "SunSet"
    if item.media_type = "episode" and item.show_title <> invalid and item.show_title <> ""
        s = item.season
        e = item.episode
        if s <> invalid and e <> invalid
            return item.show_title + " S" + Right("0" + s.ToStr(), 2) + " E" + Right("0" + e.ToStr(), 2)
        end if
        return item.show_title
    end if
    if item.title <> invalid then return item.title
    return "SunSet"
end function
