sub init()
    m.title = m.top.findNode("title")
    m.serverLabel = m.top.findNode("serverLabel")
    m.userLabel = m.top.findNode("userLabel")
    m.userBox = m.top.findNode("userBox")
    m.passLabel = m.top.findNode("passLabel")
    m.passBox = m.top.findNode("passBox")
    m.signInBtn = m.top.findNode("signInBtn")
    m.changeServerBtn = m.top.findNode("changeServerBtn")
    m.error = m.top.findNode("error")

    m.title.color = ThemeColor("textPrimary")
    m.title.font = ThemeFont(GetTheme().fonts.header, 72)
    m.serverLabel.color = ThemeColor("textSecondary")
    m.serverLabel.font = ThemeFont(GetTheme().fonts.small, 24)
    m.userLabel.color = ThemeColor("textSecondary")
    m.userLabel.font = ThemeFont(GetTheme().fonts.small, 24)
    m.passLabel.color = ThemeColor("textSecondary")
    m.passLabel.font = ThemeFont(GetTheme().fonts.small, 24)
    m.error.color = ThemeColor("error")
    m.error.font = ThemeFont(GetTheme().fonts.small, 24)
    m.error.visible = false

    cx = 1920 / 2
    m.title.translation = [cx - 150, 160]
    m.title.horizAlign = "center"
    m.title.width = 300
    m.serverLabel.translation = [cx - 400, 280]
    m.serverLabel.horizAlign = "center"
    m.serverLabel.width = 800
    m.serverLabel.text = AppServerUrl()

    m.userLabel.translation = [cx - 480, 380]
    m.userBox.translation = [cx - 480, 430]
    m.userBox.width = 960
    m.userBox.height = 72

    m.passLabel.translation = [cx - 480, 560]
    m.passBox.translation = [cx - 480, 610]
    m.passBox.width = 960
    m.passBox.height = 72

    m.signInBtn.translation = [cx - 480, 760]
    m.signInBtn.width = 460
    m.signInBtn.height = 72
    m.signInBtn.focusedColor = ThemeColor("primary")

    m.changeServerBtn.translation = [cx - 20, 760]
    m.changeServerBtn.width = 500
    m.changeServerBtn.height = 72
    m.changeServerBtn.focusedColor = ThemeColor("primary")

    m.error.translation = [cx - 480, 880]
    m.error.horizAlign = "center"
    m.error.width = 960
    m.error.wrap = true

    m.signInBtn.observeField("buttonSelected", "onSignIn")
    m.changeServerBtn.observeField("buttonSelected", "onChangeServer")

    m.userBox.setFocus(true)
end sub

sub onSignIn()
    username = Trim(m.userBox.text)
    password = Trim(m.passBox.text)
    if username = "" or password = ""
        ShowError("Enter both a username and a password.")
        return
    end if
    ShowError("")
    m.signInBtn.text = "Signing in..."
    m.signingIn = true

    task = CreateObject("roSGNode", "ApiTask")
    task.uri = ApiEndpoint("/login")
    task.method = "POST"
    task.body = FormatJson({ username: username, password_hash: password })
    task.observeField("response", "onLoginResponse")
    task.control = "RUN"
    m.loginTask = task
end sub

sub onLoginResponse()
    if m.loginTask = invalid then return
    result = { status: m.loginTask.status, json: invalid }
    raw = m.loginTask.response
    m.loginTask = invalid
    m.signingIn = false
    m.signInBtn.text = "Sign In"
    if raw <> invalid and raw <> "" then result.json = ParseJson(raw)

    if result.json = invalid or result.status < 200 or result.status >= 300
        ShowError("Sign in failed. Check your username and password.")
        return
    end if

    user = result.json
    if user.user_id = invalid or user.user_id = ""
        ShowError("Sign in failed. Check your username and password.")
        return
    end if

    SetAppSession(user.user_id, user.username, user.is_admin = true)
    m.top.result = { action: "loggedIn" }
end sub

sub ShowError(msg as String)
    m.error.visible = msg <> ""
    m.error.text = msg
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if key = "back"
        if m.userBox.hasFocus()
            m.userBox.setFocus(false)
            m.changeServerBtn.setFocus(true)
            return true
        else if m.passBox.hasFocus()
            m.passBox.setFocus(false)
            m.userBox.setFocus(true)
            return true
        end if
        ' From buttons, go back to server setup
        m.top.result = { action: "showServerSetup" }
        return true
    end if
    if key = "OK"
        ' TextEditBoxes handle their own keyboard
        if m.userBox.hasFocus() or m.passBox.hasFocus() then return false
        if m.signInBtn.hasFocus() then onSignIn() : return true
        if m.changeServerBtn.hasFocus() then onChangeServer() : return true
    end if
    return false
end function

sub onChangeServer()
    m.top.result = { action: "showServerSetup" }
end sub
