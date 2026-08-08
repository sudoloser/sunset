sub Main(args as Object)
    print "[SunSet] Channel starting"

    screen = CreateObject("roSGScreen")
    m.port = CreateObject("roMessagePort")
    screen.SetMessagePort(m.port)
    scene = screen.CreateScene("RootScene")
    screen.Show()

    ' Feed launch args into the scene for deep linking later
    if args <> invalid
        scene.launchArgs = args
    end if

    while true
        msg = wait(0, m.port)
        msgType = type(msg)
        if msgType = "roSGScreenEvent"
            if msg.isScreenClosed() then return
        end if
    end while
end sub
