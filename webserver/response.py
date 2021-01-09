from . import util
from . import status

class Response:
    def __init__(self, protocol="HTTP/1.1",statuscode=200,headers={"server": "spellsoftruth"},body={"length":0,"content":None}):
        self.protocol = protocol
        self.statuscode = statuscode
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
        sb = f"{self.protocol} {self.statuscode} {status.getstatus(self.statuscode)}\r\n"
        for key,value in self.headers.items():
            # print(f"toheaderstring key:{key} value:{value}")
            if isinstance(value, list):
                for v in value:
                    sb += f"{key}: {v}\r\n"
            else:
                sb += f"{key}: {value}\r\n"
        sb += "\r\n"
        print(f"response toheaderstring:{sb}")
        return sb

    @staticmethod
    def errorresponse(statuscode, text):
        response = Response()
        response.statuscode = int(statuscode)
        response.headers["content-type"] = "text/plain; charset=utf-8"
        response.body['content'] = text
        response.body['length'] = len(text)
        return response
