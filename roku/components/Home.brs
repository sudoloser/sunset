sub init()
    m.brand = m.top.findNode("brand")
    m.welcome = m.top.findNode("welcome")
    m.searchBtn = m.top.findNode("searchBtn")
    m.logoutBtn = m.top.findNode("logoutBtn")
    m.loading = m.top.findNode("loading")
    m.errorLabel = m.top.findNode("errorLabel")
    m.homeList = m.top.findNode("homeList")

    ' Theme styling
    m.brand.color = ThemeColor("primary")
    m.brand.font = ThemeFont(GetTheme().fonts.header, 56)
    m.brand.translation = [ThemeNumber("padding"), 40]
    m.brand.horizAlign = "left"
    m.brand.width = 260

    m.welcome.color = ThemeColor("textSecondary")
    m.welcome.font = ThemeFont(GetTheme().fonts.small, 24)
    m.welcome.translation = [ThemeNumber("padding"), 110]
    m.welcome.horizAlign = "left"
    m.welcome.width = 700
    m.welcome.text = "Welcome" + IIf(AppUsername() <> "", ", " + AppUsername(), "")

    m.searchBtn.translation = [1920 - 460, 40]
    m.searchBtn.width = 200
    m.searchBtn.height = 64
    m.searchBtn.focusedColor = ThemeColor("primary")

    m.logoutBtn.translation = [1920 - 240, 40]
    m.logoutBtn.width = 200
    m.logoutBtn.height = 64
    m.logoutBtn.focusedColor = ThemeColor("primary")

    m.loading.color = ThemeColor("textSecondary")
    m.loading.font = ThemeFont(GetTheme().fonts.body, 30)
    m.loading.horizAlign = "center"
    m.loading.width = 400
    m.loading.translation = [(1920 - 400) / 2, 480]
    m.loading.visible = true

    m.errorLabel.color = ThemeColor("error")
    m.errorLabel.font = ThemeFont(GetTheme().fonts.body, 28)
    m.errorLabel.horizAlign = "center"
    m.errorLabel.width = 1400
    m.errorLabel.wrap = true
    m.errorLabel.translation = [(1920 - 1400) / 2, 460]
    m.errorLabel.visible = false

    m.homeList.visible = false
    m.homeList.translation = [0, ThemeNumber("topOffset")]

    m.searchBtn.observeField("buttonSelected", "onSearchBtn")
    m.logoutBtn.observeField("buttonSelected", "onLogoutBtn")
    m.homeList.observeField("rowItemSelected", "onItemSelected")
    m.homeList.observeField("rowItemFocused", "onItemFocused")

    ' Kick off background data load
    m.loader = CreateObject("roSGNode", "HomeLoader")
    m.loader.userId = AppUserId()
    m.loader.observeField("result", "onLoadResult")
    m.loader.observeField("error", "onLoadError")
    m.loader.control = "RUN"

    m.searchBtn.setFocus(true)
end sub

sub onLoadResult()
    rows = m.loader.result
    if rows = invalid then return
    m.loading.visible = false
    BuildContent(rows)
    m.homeList.setFocus(true)
end sub

sub onLoadError()
    m.loading.visible = false
    m.errorLabel.text = m.loader.error
    m.errorLabel.visible = true
    m.searchBtn.setFocus(true)
end sub

sub BuildContent(rows as Object)
    root = CreateObject("roSGNode", "ContentNode")
    sizes = []
    heights = []
    labels = []
    spacings = []
    labelOffsets = []

    for each row in rows
        rowNode = root.CreateChild("ContentNode")
        rowNode.title = row.title
        itemSize = [ThemeNumber("posterWidth"), ThemeNumber("posterHeight")]
        itemSpacing = [ThemeNumber("itemSpacing"), 0]
        sizes.push(itemSize)
        heights.push(ThemeNumber("posterHeight"))
        labels.push(true)
        spacings.push(itemSpacing)
        labelOffsets.push([0, ThemeNumber("itemSpacing")])

        for each item in row.items
            itemNode = rowNode.CreateChild("ContentNode")
            displayTitle = ItemDisplayTitle(item)
            itemNode.title = displayTitle
            itemNode.description = ItemDescription(item)
            itemNode.hdposterurl = MediaPosterUrl(item)
            itemNode.addField("mediaData", "assocarray", true)
            itemNode.mediaData = item
        end for
    end for

    m.homeList.content = root
    m.homeList.numRows = root.getChildCount()
    m.homeList.itemComponentName = "MediaPoster"
    m.homeList.rowItemSize = sizes
    m.homeList.rowHeights = heights
    m.homeList.showRowLabel = labels
    m.homeList.rowItemSpacing = spacings
    m.homeList.rowLabelOffset = labelOffsets
    m.homeList.itemSpacing = [0, ThemeNumber("rowSpacing")]
    m.homeList.rowLabelColor = ThemeColor("textPrimary")
    m.homeList.rowFocusAnimationStyle = "fixedFocus"
    m.homeList.focusXOffset = [ThemeNumber("padding")]
    m.homeList.visible = true
end sub

sub onItemSelected()
    sel = m.homeList.rowItemSelected
    if sel = invalid then return
    rowIdx = sel[0]
    colIdx = sel[1]
    row = m.homeList.content.getChild(rowIdx)
    if row = invalid then return
    itemNode = row.getChild(colIdx)
    if itemNode = invalid or itemNode.mediaData = invalid then return
    m.top.result = { action: "details", item: itemNode.mediaData }
end sub

sub onItemFocused()
    ' Future: hero/preview panel could react to the focused row
end sub

sub onSearchBtn()
    m.top.result = { action: "search" }
end sub

sub onLogoutBtn()
    dialog = CreateObject("roSGNode", "Dialog")
    dialog.title = "Log out"
    dialog.message = "Sign out of " + AppServerUrl() + "?"
    dialog.buttons = ["Log Out", "Cancel"]
    dialog.observeField("buttonSelected", "onLogoutDialog")
    m.logoutDialog = dialog
    m.top.dialog = dialog
end sub

sub onLogoutDialog()
    m.top.dialog.close = true
    if m.logoutDialog <> invalid and m.logoutDialog.buttonSelected = 0
        m.top.result = { action: "logout" }
    else
        m.homeList.setFocus(true)
    end if
    m.logoutDialog = invalid
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if key = "back"
        if m.top.dialog <> invalid then return false
        onLogoutBtn()
        return true
    end if
    if key = "down"
        if m.searchBtn.hasFocus() or m.logoutBtn.hasFocus()
            if m.homeList.visible
                m.homeList.setFocus(true)
                return true
            end if
        end if
    end if
    if key = "left" or key = "right"
        if m.searchBtn.hasFocus()
            m.logoutBtn.setFocus(true)
            return true
        else if m.logoutBtn.hasFocus()
            m.searchBtn.setFocus(true)
            return true
        end if
    end if
    if key = "up"
        if m.homeList.hasFocus()
            m.searchBtn.setFocus(true)
            return true
        end if
    end if
    return false
end function

' ---------------------------------------------------------------------------
' Item display helpers
' ---------------------------------------------------------------------------

function ItemDisplayTitle(item as Object) as String
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

function ItemDescription(item as Object) as String
    if item = invalid then return ""
    desc = item.description
    if desc = invalid or desc = "" then desc = item.genres
    if desc = invalid or desc = "" then desc = "No description available."
    return desc
end function

function IIf(cond as Boolean, a as Object, b as Object) as Object
    if cond then return a else return b
end function
