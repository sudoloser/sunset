sub init()
    m.title = m.top.findNode("title")
    m.subtitle = m.top.findNode("subtitle")
    m.urlLabel = m.top.findNode("urlLabel")
    m.urlBox = m.top.findNode("urlBox")
    m.connectBtn = m.top.findNode("connectBtn")
    m.status = m.top.findNode("status")

    ' Apply theme
    m.title.color = ThemeColor("primary")
    m.title.font = ThemeFont(GetTheme().fonts.header, 96)
    m.subtitle.color = ThemeColor("textSecondary")
    m.subtitle.font = ThemeFont(GetTheme().fonts.subheader, 32)
    m.urlLabel.color = ThemeColor("textSecondary")
    m.urlLabel.font = ThemeFont(GetTheme().fonts.small, 24)
    m.status.color = ThemeColor("error")
    m.status.font = ThemeFont(GetTheme().fonts.small, 24)
    m.status.visible = false

    ' Layout
    cx = 1920 / 2
    m.title.translation = [cx - 120, 220]
    m.title.horizAlign = "center"
    m.title.width = 240
    m.subtitle.translation = [cx - 350, 360]
    m.subtitle.horizAlign = "center"
    m.subtitle.width = 700
    m.urlLabel.translation = [cx - 480, 480]
    m.urlBox.translation = [cx - 480, 530]
    m.urlBox.width = 960
    m.urlBox.height = 72
    m.connectBtn.translation = [cx - 160, 680]
    m.connectBtn.width = 320
    m.connectBtn.height = 72
    m.connectBtn.focusedColor = ThemeColor("primary")
    m.status.translation = [cx - 480, 800]
    m.status.horizAlign = "center"
    m.status.width = 960
    m.status.wrap = true

    m.urlBox.text = AppServerUrl()
    m.connectBtn.observeField("buttonSelected", "onConnect")

    ' Focus the URL box; pressing OK opens the on-screen keyboard.
    m.urlBox.setFocus(true)
end sub

sub onConnect()
    url = Trim(m.urlBox.text)
    if url = ""
        ShowStatus("Enter the server URL first.")
        return
    end if
    if Left(url, 4) <> "http"
        url = "http://" + url
    end if
    url = url.Replace(" ", "%20")
    while url.Right(1) = "/"
        url = url.Left(url.Len() - 1)
    end while

    ShowStatus("")
    m.connectBtn.text = "Connecting..."
    m.testing = true
    m.urlBox.setFocus(false)
    m.newUrl = url

    test = CreateObject("roSGNode", "ApiTask")
    test.uri = url + "/api/status"
    test.observeField("response", "onTestResponse")
    test.control = "RUN"
    m.testTask = test
end sub

sub onTestResponse()
    if m.testTask = invalid then return
    result = { status: m.testTask.status, json: invalid }
    raw = m.testTask.response
    m.testTask = invalid
    m.testing = false
    m.connectBtn.text = "Connect"
    m.urlBox.setFocus(true)
    if raw <> invalid and raw <> "" then result.json = ParseJson(raw)

    if result.status >= 200 and result.status < 300 and result.json <> invalid
        SetAppServerUrl(m.newUrl)
        m.top.result = { action: "serverConnected" }
    else
        ShowStatus("Server answered but looks wrong (HTTP " + result.status.ToStr() + ").")
    end if
end sub

sub ShowStatus(msg as String)
    m.status.visible = msg <> ""
    m.status.text = msg
end sub

' When the OS keyboard is up, back closes it first (handled by system).
function onKeyEvent(key as String, press as Boolean) as Boolean
    if press
        if key = "OK" and m.urlBox.hasFocus()
            ' Let the TextEditBox open its keyboard
            return false
        end if
        if key = "back"
            if m.urlBox.hasFocus()
                m.urlBox.setFocus(false)
                m.connectBtn.setFocus(true)
                return true
            end if
            if m.connectBtn.hasFocus()
                m.urlBox.setFocus(true)
                return true
            end if
        end if
    end if
    return false
end function
