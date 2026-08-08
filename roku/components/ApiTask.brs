sub init()
    m.top.functionName = "runTask"
end sub

sub runTask()
    transfer = CreateObject("roUrlTransfer")
    transfer.SetUrl(m.top.uri)
    transfer.SetCertificatesFile("common:/certs/ca-certificates.crt")
    transfer.InitClientCertificates()
    transfer.SetRequestHeader("Accept", "application/json")
    transfer.SetTimeout(15000)

    resp = ""
    method = m.top.method
    if method = "POST"
        transfer.SetRequestHeader("Content-Type", "application/json")
        resp = transfer.PostFromString(m.top.body)
    else if method = "PUT"
        transfer.SetRequestHeader("Content-Type", "application/json")
        resp = transfer.PutFromString(m.top.body)
    else if method = "DELETE"
        transfer.SetRequestHeader("Content-Type", "application/json")
        resp = transfer.DeleteFromString(m.top.body)
    else
        resp = transfer.GetToString()
    end if

    m.top.status = transfer.GetResponseCode()
    m.top.response = resp
    if resp = invalid
        m.top.error = "Request failed"
    end if
end sub
