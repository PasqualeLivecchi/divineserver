from . import util


class Response:
    def __init__(self, protocol="HTTP/1.1",status="OK",headers={"server": "spellsoftruth"},body={"length":None,"content":None}):
        self.protocol = protocol
        self.status = status
        self.headers = headers
        self.body = body

    def addheader(self,name, value):
        self.headers[name] = value

    def setcookie(self,name,value,attributes={}):
        buf = f"{util.urlencode(name)}={util.urlencode(value)}"
        for k,v in attributes.items():
            buf += f" {k}={v}"
        self.addheader("Set-Cookie", str(buf))

    def toheaderstring(self):
        sb = f"{self.protocol} {self.status.code} {self.status.reason} \r\n"
        for key,value in headers.items():
            if isinstance(value, list):
                for v in value:
                    sb += f"{key}: {v}\r\n"
            else:
                sb += f"{key}: {value}\r\n"
        sb += "\r\n"
        return str(sb)

    def errorresponse(self,status, text):
        self.response = Response()
        self.response.status = status
        self.response.headers["content-type"] = "text/plain charset=utf-8"
        with open(self.response, "w") as w: #PrintWriter(ResponseOutputStream(response))
            w.write(text)
        return self.response