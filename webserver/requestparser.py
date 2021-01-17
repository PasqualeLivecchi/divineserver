from .parser import Parser, ParseException
from .request import Request
import pickle

class RequestParser:
    # private static final Logger logger = LoggerFactory.getLogger(RequestParser.class)
    def __init__(self, request):
        if isinstance(request,Request):
            self.request = request
            self.parser = None
        else:
            raise ValueError("Request required")
    
    def parseurlencoded(self,charset):
        print("parseurlencoded")
        if self.request.body is None:
            print("body is null\r\n" + self.request.rawhead)
            return
        self.parser = Parser(str(self.request.body,encoding=charset))
        self.parsequery()
        self.require(self.parser.endofinput())
    
    def parsehead(self):
        # print(f"requestparser parsehead")
        self.parser = Parser(rf"{self.request.rawhead}")
        self.parserequestline()
        while not self.parser.match(string="\r\n"):
            print("new property")
            self.parserheaderfield()
        self.parsecookies()

    def parserequestline(self):
        # print("requestparser parserequestline")
        self.parsemethod()
        self.require(self.parser.match(char=' '))
        self.parserawpath()
        self.require(self.parser.match(char=' '))
        self.parseprotocol()
        self.require(self.parser.match(string="\r\n"))

    def parsemethod(self):
        start = int(self.parser.currentindex())
        if not self.methodchar():
            raise ParseException(self.parser, "no method")
        while self.methodchar():
            # print("parsemethod methodchar")
            continue
        self.request.method = self.parser.textfrom(start)

    def methodchar(self):
        return self.parser.incharrange('A','z')

    def parserawpath(self):
        start = self.parser.currentindex()
        self.parsepath()
        if self.parser.match(char='?'):
            self.parsequery()
        self.request.rawpath = self.parser.textfrom(start)

    def parsepath(self):
        # print("requestparser parsepath")
        start = self.parser.currentindex()
        if not self.parser.match(char='/'):
            raise ParseException(self.parser, "bad path")
        while self.parser.noneof(" ?#"):
            continue
        self.request.path = self.urldecode(self.parser.textfrom(start))
        self.request.originalpath = self.request.path

    def parsequery(self):
        while True:
            start = self.parser.currentindex()
            while self.querychar():
                continue
            name = self.urldecode(self.parser.textfrom(start))
            value = None
            if self.parser.match(char='='):
                start = self.parser.currentindex()
                while self.querychar() or self.parser.match(char='='):
                    continue
                value = self.urldecode(self.parser.textfrom(start))
            if len(name) > 0 or value != None:
                if value is None:
                    value = ""
                self.request.parameters[name] = value
            if not self.parser.match(char='&'):
                break

    def querychar(self):
        return self.parser.noneof("=&# \t\n\f\r\u000b")

    def parseprotocol(self):
        # print("requestparser parseprotocol")
        start = self.parser.currentindex()
        if not (self.parser.match(string="HTTP/") and self.parser.incharrange('0','9') and self.parser.match(char='.') and self.parser.incharrange('0','9')):
            raise ParseException(self.parser, "bad protocol")
        self.request.protocol = self.parser.textfrom(start)
        self.request.scheme = "http"

    def parserheaderfield(self):
        # print(f"requestparser parserheaderfield")
        name = self.parsename()
        self.require(self.parser.match(char=':'))
        while self.parser.anyof(" \t"):
            # print(f"parserheaderfield is any of ' \t' 1")
            continue
        value = self.parsevalue()
        # print(f"parserheaderfield value:{value}")
        while self.parser.anyof(" \t"):
            # print(f"parserheaderfield is any of ' \t' 2")
            continue
        self.require(self.parser.match(string="\r\n"))
        self.request.headers[name] = value

    def parsename(self):
        # print("requestparser parsename")
        start = self.parser.currentindex()
        self.require(self.tokenchar())
        while self.tokenchar():
            # print("parsename not tokenchar")
            continue
        return self.parser.textfrom(start).lower()

    def parsevalue(self):
        # print("requestparser parsevalue")
        start = self.parser.currentindex()
        while not self.testendofvalue():
            self.require(self.parser.anychar())
        return self.parser.textfrom(start)

    def testendofvalue(self):
        # print("requestparser testendofvalue")
        self.parser.begin()
        while self.parser.anyof(" \t"):
            continue
        isendof = self.parser.endofinput() or self.parser.anyof("\r\n")
        self.parser.failure() # rollback
        return isendof

    def require(self, boolean):
        # print("requestparser require")
        # print(f"require boolean:{boolean}")
        if not boolean:
            raise ParseException(self.parser, "failed")

    def tokenchar(self):
        # print("requestparser tokenchar")
        if self.parser.endofinput():
            print("tokenchar endofinput False")
            return False
        c = self.parser.currentchar()
        # print(f"tokenchar currentchar c:{c}")
        if 32 <= ord(c) <= 126 and "()<>@,:\\\"/[]?={} \t\r\n".find(c) == -1:
            self.parser.anychar()
            return True
        else:
            return False

    def parsecookies(self):
        text = self.request.headers.get("cookie")
        if text is None:
            return
        self.parser = Parser(rf"{text}")
        while True:
            start = self.parser.currentindex()
            while self.parser.noneof("="):
                continue
            name = self.urldecode(self.parser.textfrom(start))
            if self.parser.match(char='='):
                start = self.parser.currentindex()
                while self.parser.noneof(""):
                    continue
                value = self.parser.textfrom(start)
                length = len(value)
                if value.charat(0)=='"' and value.charat(length-1) == '"':
                    value = value.substring(1,length-1)
                value = self.urldecode(value)
                self.request.cookies[name] = value
            if self.parser.endofinput():
                return
            self.require(self.parser.match(char=''))
            self.parser.match(char=' ')  # optional for bad browsers

    def parsemultipart(self):
        contenttypestart = "multipart/form-data; boundary="
        if self.request.body is None:
            print("body is null\n"+self.request.rawhead)
            return
        contenttype = self.request.headers.get("content-type")
        if not contenttype.startswith(contenttypestart):
            raise RuntimeError(contenttype)
        boundary = "--" + contenttype[:len(contenttypestart)]
        self.parser = Parser(rf"{self.request.body}")
        self.require(self.parser.match(string=boundary))
        boundary = "\r\n" + boundary
        while not self.parser.match(string="--\r\n"):
            self.require(self.parser.match(string="\r\n"))
            self.require(self.parser.match(string="Content-Disposition: form-data; name="))
            name = self.quotedstring()
            filename = None
            isbinary = False
            if self.parser.match(string=" filename="):
                filename = self.quotedstring()
                self.require(self.parser.match(string="\r\n"))
                self.require(self.parser.match(string="Content-Type: "))
                start = self.parser.currentindex()
                if self.parser.match(string="application/"):
                    isbinary = True
                elif self.parser.match(string="image/"):
                    isbinary = True
                elif self.parser.match(string="text/"):
                    isbinary = False
                else:
                    raise ParseException(self.parser, "bad file content-type")
                while self.parser.incharrange('A','z') or self.parser.anyof("-."):
                    continue
                contenttype = self.parser.textfrom(start)
            self.require(self.parser.match("\r\n"))
            self.require(self.parser.match("\r\n"))
            start = self.parser.currentindex()
            while not self.parser.test(boundary):
                self.require(self.parser.anychar())
            value = self.parser.textfrom(start)
            if filename is None:
                self.request.parameters[name] = value
            else:
                content = bytes(value) if all(c in '01' for c in content) else value
                raise NotImplementedError("MultipartFiles Not Implemented Yet")
                mf = MultipartFile(filename,contenttype,content)
                self.request.parameters[name] = mf
            self.require(self.parser.match(boundary))

    def quotedstring(self):
        sb = ""
        self.require(self.parser.match(char='"'))
        while not self.parser.match(char='"'):
            if self.parser.match(string="\\\""):
                sb += '"'
            else:
                self.require(self.parser.anychar())
                sb += self.parser.lastchar()
        return str(sb)

    def urldecode(self, string):
        try:
            return bytes(string,encoding="utf-8").decode()
        except UnicodeEncodeError as uee:
            self.parser.rollback()
            raise ParseException(self.parser, "unsupported encoding" + str(uee))
        except ValueError as ve:
            self.parser.rollback()
            raise ParseException(self.parser, "illegal argument" + str(ve))

    def parsejson(self, charset):
        if self.request.body is None:
            print("body is empty(None aka null)\n"+self.request.rawhead)
            return
        value = str(self.request.body,encoding=charset)
        self.request.parameters["json"] = value