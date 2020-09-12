from . import util

# class Body:
#     length = None
#     content = None

#     def __init__(self, length, content):
#         self.length = length
#         self.content = content

class Response:
    protocol = "HTTP/1.1"
    status = "OK"
    headers = {"server": "spellsoftruth"}
    empty = {"length":None,"content":None}
    body = empty
    # def __init__(self, protocol="HTTP/1.1",status="OK",headers={"server": "spellsoftruth"},body={"length":None,"content":None}):
    #     self.protocol = protocol
    #     self.status = status
    #     self.headers = headers
    #     self.body = body

    def addheader(self,name, value):
        self.headers[name] = value

    def setcookie(self,name,value,attributes={}):
        buf = f"{util.urlencode(name)}={util.urlencode(value)}"
        for k,v in attributes.items():
            buf += f" {k}={v}"
        addheader("Set-Cookie", str(buf))


    def toheaderstring(self):
        sb = f"{protocol} {status.code} {status.reason} \r\n"
        for k,v in headers.items():
            name = k
            value = v
            if isinstance(value, list):
                for v in value:
                    sb += f"{name}: {v}\r\n"
            else:
                sb += f"{name}: {value}\r\n"
        sb += "\r\n"
        return str(sb)

    def errorresponse(self,status, text):
        response = Response()
        response.status = status
        response.headers["content-type"] = "text/plain charset=utf-8"
        writer = open(response, "w") #PrintWriter(ResponseOutputStream(response))
        writer.write(text)
        writer.close()
        return response