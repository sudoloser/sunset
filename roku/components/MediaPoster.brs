sub init()
    m.poster = m.top.findNode("poster")
    m.focusBorder = m.top.findNode("focusBorder")
    m.title = m.top.findNode("title")

    m.posterWidth = ThemeNumber("posterWidth")
    m.posterHeight = ThemeNumber("posterHeight")
    m.titleHeight = ThemeNumber("posterTitleHeight")

    m.poster.width = m.posterWidth
    m.poster.height = m.posterHeight
    m.poster.translation = [0, 0]

    m.focusBorder.width = m.posterWidth
    m.focusBorder.height = m.posterHeight
    m.focusBorder.color = "0x00000000"
    m.focusBorder.visible = false

    m.title.width = m.posterWidth
    m.title.height = m.titleHeight
    m.title.translation = [0, m.posterHeight]
    m.title.color = ThemeColor("textSecondary")
    m.title.font = ThemeFont(GetTheme().fonts.small, ThemeNumber("posterTitleFontSize"))
    m.title.vertAlign = "top"
end sub

sub itemContentChanged()
    item = m.top.itemContent
    if item = invalid then return

    url = item.hdposterurl
    if url = invalid or url = "" then url = ThemeString("posterPlaceholder")
    m.poster.uri = url

    t = item.Title
    if t = invalid then t = ""
    m.title.text = t
    m.item = item
end sub

sub itemFocusedChanged()
    if m.top.itemFocused
        m.focusBorder.visible = true
        m.focusBorder.color = ThemeColor("focusBorder")
        m.title.color = ThemeColor("textPrimary")
        m.poster.opacity = 1.0
    else
        m.focusBorder.visible = false
        m.title.color = ThemeColor("textSecondary")
        m.poster.opacity = 0.85
    end if
end sub
