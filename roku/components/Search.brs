sub init()
    m.title = m.top.findNode("title")
    m.queryLabel = m.top.findNode("queryLabel")
    m.queryBox = m.top.findNode("queryBox")
    m.searchBtn = m.top.findNode("searchBtn")
    m.loading = m.top.findNode("loading")
    m.noResults = m.top.findNode("noResults")
    m.results = m.top.findNode("results")

    m.title.color = ThemeColor("textPrimary")
    m.title.font = ThemeFont(GetTheme().fonts.header, 56)
    m.title.translation = [ThemeNumber("padding"), 40]

    m.queryLabel.color = ThemeColor("textSecondary")
    m.queryLabel.font = ThemeFont(GetTheme().fonts.small, 24)
    m.queryLabel.translation = [ThemeNumber("padding"), 160]

    m.queryBox.translation = [ThemeNumber("padding"), 220]
    m.queryBox.width = 900
    m.queryBox.height = 72

    m.searchBtn.translation = [ThemeNumber("padding") + 940, 220]
    m.searchBtn.width = 220
    m.searchBtn.height = 72
    m.searchBtn.focusedColor = ThemeColor("primary")

    m.loading.color = ThemeColor("textSecondary")
    m.loading.font = ThemeFont(GetTheme().fonts.body, 28)
    m.loading.translation = [ThemeNumber("padding"), 380]
    m.loading.visible = false

    m.noResults.color = ThemeColor("textSecondary")
    m.noResults.font = ThemeFont(GetTheme().fonts.body, 28)
    m.noResults.translation = [ThemeNumber("padding"), 380]
    m.noResults.visible = false

    m.results.visible = false
    m.results.translation = [0, 360]

    m.searchBtn.observeField("buttonSelected", "onSearch")
    m.results.observeField("rowItemSelected", "onItemSelected")

    m.queryBox.setFocus(true)
end sub

sub onSearch()
    query = Trim(m.queryBox.text)
    if query = "" then return

    m.loading.visible = true
    m.noResults.visible = false
    m.results.visible = false

    task = CreateObject("roSGNode", "ApiTask")
    task.uri = ApiEndpoint("/search?q=" + ApiEscape(query) + IIf(AppUserId() <> "", "&user_id=" + AppUserId(), ""))
    task.observeField("response", "onSearchResponse")
    task.control = "RUN"
    m.searchTask = task
end sub

sub onSearchResponse()
    if m.searchTask = invalid then return
    result = { status: m.searchTask.status, json: invalid }
    raw = m.searchTask.response
    m.searchTask = invalid
    m.loading.visible = false
    if raw <> invalid and raw <> "" then result.json = ParseJson(raw)

    if result.json = invalid then return

    items = result.json
    if items.count() = 0
        m.noResults.visible = true
        m.queryBox.setFocus(true)
        return
    end if

    BuildResults(items)
    m.results.setFocus(true)
end sub

sub BuildResults(items as Object)
    root = CreateObject("roSGNode", "ContentNode")
    rowNode = root.CreateChild("ContentNode")
    rowNode.title = "Results"

    for each item in items
        itemNode = rowNode.CreateChild("ContentNode")
        itemNode.title = m.QueryDisplayTitle(item)
        itemNode.description = m.QueryDescription(item)
        itemNode.hdposterurl = MediaPosterUrl(item)
        itemNode.addField("mediaData", "assocarray", true)
        itemNode.mediaData = item
    end for

    m.results.content = root
    m.results.numRows = 1
    m.results.itemComponentName = "MediaPoster"
    m.results.rowItemSize = [[ThemeNumber("posterWidth"), ThemeNumber("posterHeight")]]
    m.results.rowHeights = [ThemeNumber("posterHeight")]
    m.results.showRowLabel = [true]
    m.results.rowItemSpacing = [[ThemeNumber("itemSpacing"), 0]]
    m.results.rowLabelOffset = [[0, ThemeNumber("itemSpacing")]]
    m.results.itemSpacing = [0, 0]
    m.results.rowLabelColor = ThemeColor("textPrimary")
    m.results.rowFocusAnimationStyle = "fixedFocus"
    m.results.focusXOffset = [ThemeNumber("padding")]
    m.results.visible = true
end sub

sub onItemSelected()
    sel = m.results.rowItemSelected
    if sel = invalid then return
    row = m.results.content.getChild(sel[0])
    if row = invalid then return
    itemNode = row.getChild(sel[1])
    if itemNode = invalid or itemNode.mediaData = invalid then return
    m.top.result = { action: "details", item: itemNode.mediaData }
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if key = "back"
        m.top.result = { action: "back" }
        return true
    end if
    if key = "down"
        if m.queryBox.hasFocus() or m.searchBtn.hasFocus()
            if m.results.visible
                m.results.setFocus(true)
                return true
            end if
        end if
    end if
    if key = "up"
        if m.results.hasFocus()
            m.searchBtn.setFocus(true)
            return true
        end if
    end if
    if key = "left" or key = "right"
        if m.searchBtn.hasFocus()
            m.queryBox.setFocus(true)
            return true
        end if
    end if
    return false
end function

function QueryDisplayTitle(item as Object) as String
    if item = invalid then return ""
    if item.media_type = "episode" and item.show_title <> invalid
        return item.show_title
    end if
    if item.title <> invalid then return item.title
    return ""
end function

function QueryDescription(item as Object) as String
    if item = invalid then return ""
    if item.description <> invalid then return item.description
    return ""
end function

function IIf(cond as Boolean, a as Object, b as Object) as Object
    if cond then return a else return b
end function
