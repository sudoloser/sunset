' ***********************************************************
' RootScene: navigation hub. Owns the screen stack and routes
' between screens based on auth/config state.
' ***********************************************************

sub init()
    ThemeLoad()
    m.screenStack = []

    ' Background for every screen
    m.background = CreateObject("roSGNode", "Rectangle")
    m.background.color = ThemeColor("background")
    m.background.width = 1920
    m.background.height = 1080
    m.background.translation = [0, 0]
    m.top.appendChild(m.background)

    m.top.observeField("launchArgs", "onLaunchArgs")
    RouteInitial()
end sub

' Decide where to start based on stored config / session
sub RouteInitial()
    if AppServerUrl() = ""
        PushScreen("ServerSetup", {})
        return
    end if
    CheckServerStatus()
end sub

sub CheckServerStatus()
    task = CreateObject("roSGNode", "ApiTask")
    task.uri = ApiEndpoint("/status")
    task.observeField("response", "onStatusResponse")
    task.control = "RUN"
    m.statusTask = task
end sub

sub onStatusResponse()
    if m.statusTask = invalid then return
    result = { status: m.statusTask.status, json: invalid }
    raw = m.statusTask.response
    m.statusTask = invalid
    if raw <> invalid and raw <> "" then result.json = ParseJson(raw)

    if result.status >= 200 and result.status < 300 and result.json <> invalid
        if result.json.setup_complete = true
            if AppUserId() <> ""
                PushScreen("Home", {})
            else
                PushScreen("Login", {})
            end if
        else
            ShowSetupRequiredMessage()
        end if
    else
        ShowConnectionProblem()
    end if
end sub

sub ShowConnectionProblem()
    dialog = CreateObject("roSGNode", "Dialog")
    dialog.title = "Can't reach server"
    dialog.message = "Could not connect to the SunSet server at " + AppServerUrl() + ". Check that it is running and reachable from this device."
    dialog.buttons = ["Retry", "Change Server"]
    dialog.observeField("buttonSelected", "onConnProblem")
    m.connDialog = dialog
    m.top.dialog = dialog
end sub

sub onConnProblem()
    m.top.dialog.close = true
    if m.connDialog <> invalid and m.connDialog.buttonSelected = 1
        PushScreen("ServerSetup", {})
    else
        CheckServerStatus()
    end if
    m.connDialog = invalid
end sub

sub ShowSetupRequiredMessage()
    dialog = CreateObject("roSGNode", "Dialog")
    dialog.title = "Setup required"
    dialog.message = "This server has not been set up yet. Complete the first-time setup in a web browser at " + AppServerUrl() + ", then come back."
    dialog.buttons = ["OK"]
    dialog.observeField("buttonSelected", "onSetupMsg")
    m.setupDialog = dialog
    m.top.dialog = dialog
end sub

sub onSetupMsg()
    m.top.dialog.close = true
    m.setupDialog = invalid
end sub

sub onLaunchArgs()
    ' Future deep linking can route here
end sub

' ---------------------------------------------------------------------------
' Screen stack management
' ---------------------------------------------------------------------------

sub PushScreen(name as String, opts as Object)
    node = CreateObject("roSGNode", name)
    for each key in opts
        node[key] = opts[key]
    end for
    node.observeField("result", "onScreenResult")

    ' Hide the current top screen underneath
    if m.screenStack.count() > 0
        top = m.screenStack.peek()
        top.visible = false
    end if

    m.top.appendChild(node)
    m.screenStack.push(node)
    node.visible = true
    node.SetFocus(true)
end sub

sub PopScreen()
    if m.screenStack.count() = 0 then return
    top = m.screenStack.pop()
    m.top.removeChild(top)
    if m.screenStack.count() > 0
        prev = m.screenStack.peek()
        prev.visible = true
        prev.SetFocus(true)
    else
        RouteInitial()
    end if
end sub

sub ResetToScreen(name as String, opts as Object)
    while m.screenStack.count() > 0
        top = m.screenStack.pop()
        m.top.removeChild(top)
    end while
    PushScreen(name, opts)
end sub

sub onScreenResult()
    if m.screenStack.count() = 0 then return
    node = m.screenStack.peek()
    if node = invalid or node.result = invalid then return
    result = node.result

    action = result.action
    if action = "serverConnected"
        PushScreen("Login", {})
    else if action = "loggedIn"
        PushScreen("Home", {})
    else if action = "showServerSetup"
        PushScreen("ServerSetup", {})
    else if action = "logout"
        ClearAppSession()
        ResetToScreen("Login", {})
    else if action = "details"
        PushScreen("Details", { item: result.item })
    else if action = "play"
        opts = { item: result.item }
        if result.resume <> invalid then opts.resume = result.resume
        PushScreen("VideoScene", opts)
    else if action = "search"
        PushScreen("Search", {})
    else if action = "back"
        PopScreen()
    else if action = "home"
        ResetToScreen("Home", {})
    end if
end sub
