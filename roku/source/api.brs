' ***********************************************************
' SunSet API client + local session helpers.
'
' Session data is stored in the Roku registry:
'   section: sunset   key: server_url, user_id, username, is_admin
' ***********************************************************

function SunReg() as Object
    return CreateObject("roRegistrySection", "sunset")
end function

function AppServerUrl() as String
    reg = SunReg()
    url = reg.Read("server_url")
    if url = invalid then return ""
    return url
end function

function SetAppServerUrl(url as String)
    reg = SunReg()
    if url = "" then
        reg.Delete("server_url")
    else
        reg.Write("server_url", url)
    end if
    reg.Flush()
end function

function AppUserId() as String
    reg = SunReg()
    id = reg.Read("user_id")
    if id = invalid then return ""
    return id
end function

sub SetAppSession(userId as String, username as String, isAdmin as Boolean)
    reg = SunReg()
    reg.Write("user_id", userId)
    reg.Write("username", username)
    reg.Write("is_admin", isAdmin.ToStr())
    reg.Flush()
end sub

sub ClearAppSession()
    reg = SunReg()
    reg.Delete("user_id")
    reg.Delete("username")
    reg.Delete("is_admin")
    reg.Flush()
end sub

function AppUsername() as String
    reg = SunReg()
    name = reg.Read("username")
    if name = invalid then return ""
    return name
end function

' Build a full endpoint URL from a server-relative path
function ApiEndpoint(path as String) as String
    base = AppServerUrl()
    if base = "" then return ""
    base = base.Replace(" ", "%20")
    if base.right(1) = "/" then base = base.left(base.len() - 1)
    return base + "/api" + path
end function

' ---------------------------------------------------------------------------
' Synchronous JSON helpers. Blocking is acceptable here because the
' RequestTask components do the heavy work; these are used for tiny config
' fetches. Each returns an assocarray: { status, json, raw }.
' ---------------------------------------------------------------------------

function ApiGetJson(path as String) as Object
    url = ApiEndpoint(path)
    return ApiFetchUrl(url, "GET", "")
end function

function ApiPostJson(path as String, body as Object) as Object
    return ApiFetchUrl(ApiEndpoint(path), "POST", FormatJson(body))
end function

function ApiPutJson(path as String, body as Object) as Object
    return ApiFetchUrl(ApiEndpoint(path), "PUT", FormatJson(body))
end function

function ApiFetchUrl(url as String, method as String, body as String) as Object
    result = { status: 0, json: invalid, raw: "" }
    if url = "" then
        result.status = -1
        return result
    end if

    transfer = CreateObject("roUrlTransfer")
    transfer.SetUrl(url)
    transfer.SetCertificatesFile("common:/certs/ca-certificates.crt")
    transfer.InitClientCertificates()
    transfer.SetRequestHeader("Accept", "application/json")
    transfer.SetTimeout(10000)

    resp = ""
    if method = "POST"
        transfer.SetRequestHeader("Content-Type", "application/json")
        resp = transfer.PostFromString(body)
    else if method = "PUT"
        transfer.SetRequestHeader("Content-Type", "application/json")
        resp = transfer.PutFromString(body)
    else if method = "DELETE"
        resp = transfer.DeleteFromString(body)
    else
        resp = transfer.GetToString()
    end if

    result.status = transfer.GetResponseCode()
    result.raw = resp
    if resp <> "" and resp <> invalid
        parsed = ParseJson(resp)
        if parsed <> invalid then result.json = parsed
    end if
    return result
end function

' ---------------------------------------------------------------------------
' Endpoint wrappers (mirror the frontend api client)
' ---------------------------------------------------------------------------

function ApiStatus() as Object
    return ApiGetJson("/status")
end function

function ApiLogin(username as String, password as String) as Object
    body = { username: username, password_hash: password }
    return ApiPostJson("/login", body)
end function

function ApiGetUser(userId as String) as Object
    return ApiGetJson("/users/" + userId)
end function

function ApiRecentlyAdded(userId as String) as Object
    q = ""
    if userId <> "" then q = "?user_id=" + userId
    return ApiGetJson("/recently-added" + q)
end function

function ApiGetLibraries() as Object
    return ApiGetJson("/libraries")
end function

function ApiGetLibraryItems(libId as String, userId as String) as Object
    q = ""
    if userId <> "" then q = "?user_id=" + userId
    return ApiGetJson("/libraries/" + libId + "/items" + q)
end function

function ApiGetGenres() as Object
    return ApiGetJson("/genres")
end function

function ApiGetGenreItems(genre as String, userId as String) as Object
    q = ""
    if userId <> "" then q = "?user_id=" + userId
    return ApiGetJson("/genre/" + ApiEscape(genre) + q)
end function

function ApiSearch(query as String, userId as String) as Object
    q = "?q=" + ApiEscape(query)
    if userId <> "" then q = q + "&user_id=" + userId
    return ApiGetJson("/search" + q)
end function

function ApiContinueWatching(userId as String) as Object
    return ApiGetJson("/continue-watching/" + userId)
end function

function ApiGetPlayback(itemId as String) as Object
    return ApiGetJson("/playback/" + itemId)
end function

sub ApiSavePlayback(itemId as String, userId As String, timestamp As Float, duration As Float, isPlaying As Boolean)
    body = {
        item_id: itemId
        user_id: userId
        timestamp: timestamp
        duration: duration
        is_playing: isPlaying
    }
    ApiPostJson("/playback", body)
end sub

function ApiGetSubtitles(itemId as String) as Object
    return ApiGetJson("/media/" + itemId + "/subtitles")
end function

' Asset URLs (posters, backdrops). No auth required by the backend.
function ApiAssetUrl(itemId as String, assetName as String) as String
    return ApiEndpoint("/media/" + itemId + "/asset/" + ApiEscape(assetName))
end function

function ApiStreamUrl(itemId as String) as String
    return ApiEndpoint("/stream/" + itemId)
end function

function ApiSubtitleUrl(itemId as String, name as String) as String
    return ApiEndpoint("/media/" + itemId + "/subtitle/" + ApiEscape(name))
end function

' URL-encode a string for query/path use
function ApiEscape(value as String) as String
    result = CreateObject("roUrlTransfer")
    return result.Escape(value)
end function

' Pick a poster URL for a media item.
function MediaPosterUrl(item as Object) as String
    placeholder = ThemeString("posterPlaceholder")
    if item = invalid then return placeholder
    if item.id = invalid or item.id = "" then return placeholder
    url = ApiAssetUrl(item.id, "folder.jpg")
    if url = "" then return placeholder
    return url
end function

' Derive the Roku streamFormat from the media file path.
function StreamFormatFor(item as Object) as String
    if item = invalid then return "mp4"
    path = item.file_path
    if path = invalid then return "mp4"
    lower = LCase(path)
    if lower.right(3) = "mkv" then return "mkv"
    if lower.right(3) = "webm" then return "webm"
    if lower.right(4) = "m3u8" then return "hls"
    if lower.right(3) = "ts" or lower.right(4) = "m2ts" then return "hls"
    if lower.right(3) = "mov" or lower.right(4) = "m4v" then return "mp4"
    return "mp4"
end function
