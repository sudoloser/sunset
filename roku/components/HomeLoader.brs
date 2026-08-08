' Loads all the rows for the home screen in the background.
' Each row: { title, items: [media assocarrays] }
sub init()
    m.top.functionName = "runTask"
end sub

sub runTask()
    rows = []
    userId = m.top.userId

    cw = ApiContinueWatching(userId)
    if cw.json <> invalid and cw.json.count() > 0
        rows.push({ title: "Continue Watching", items: cw.json })
    end if

    recent = ApiRecentlyAdded(userId)
    if recent.json <> invalid and recent.json.count() > 0
        rows.push({ title: "Recently Added", items: recent.json })
    end if

    genres = ApiGetGenres()
    if genres.json <> invalid and genres.json.count() > 0
        count = 0
        for each genre in genres.json
            if count >= 6 then exit for
            items = ApiGetGenreItems(genre, userId)
            if items.json <> invalid and items.json.count() > 0
                rows.push({ title: genre, items: items.json })
                count = count + 1
            end if
        end for
    end if

    libs = ApiGetLibraries()
    if libs.json <> invalid
        for each lib in libs.json
            items = ApiGetLibraryItems(lib.id, userId)
            if items.json <> invalid and items.json.count() > 0
                rows.push({ title: lib.name, items: items.json })
            end if
        end for
    end if

    if rows.count() = 0
        m.top.error = "No content found on this server"
    end if
    m.top.result = rows
end sub
