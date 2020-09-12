from .requestparser import RequestParser
from .request import Request
from .response import Response
from copy import deepcopy
import socket

class Connection:
    server = None
    sock = None

    def __init__(self, server, sock):
        self.server = server
        self.sock = sock

    def handle(self):
        try:
            request = Request()
            response = None
            contenttype = None
            try:
                incoming = self.sock
                a = bytearray(8192)
                endofheader = None
                size = 0
                left = len(a)
                while True:
                    n = incoming.read(a,size,left)
                    if n == -1:
                        if size == 0:
                            self.sock.close()
                            return
                        raise IOError("unexpected end of input at "+size)
                    size += n
                    breakwhile = False
                    for i in range(size-3):
                        if a[i]=='\r' and a[i+1]=='\n' and a[i+2]=='\r' and a[i+3]=='\n':
                            endofheader = i + 4
                            breakwhile = True
                            break
                    if breakwhile:
                        break
                    left -= n
                    if left == 0:
                        a2 = bytearray(2*len(a))
                        a = [deepcopy(a[x]) for x in range(len(a2))]
                        # System.arraycopy(a,0,a2,0,size)
                        a = a2
                        left = a.length - size
                rawhead = str(a[0:endofheader])
                # System.out.println(rawhead)
                request.rawhead = rawhead
                parser = RequestParser(request)
                parser.parsehead()
                lenstr = request.headers.get("content-length")
                if lenstr != None:
                    length = int(lenstr)
                    body = bytearray(length)
                    size -= endofheader
                    a = [deepcopy(a[x]) for x in range(endofheader,len(body))]
                    # System.arraycopy(a,endofheader,body,0,size)
                    while size < length:
                        n = incoming.read(body,size,length-size)
                        if n == -1:
                            raise IOError("unexpected end of input at "+size)
                        size += n
                    request.body = body
                    # System.out.println(new String(request.body))

                contenttype = request.headers.get("content-type")
                if contenttype != None:
                    contenttype = contenttype.lower()
                    if contenttype.equals("application/x-www-form-urlencoded"):
                        parser.parseurlencoded(None)
                    elif contenttype.equals("application/x-www-form-urlencoded charset=utf-8"):
                        parser.parseurlencoded("utf-8")
                    elif contenttype.startswith("multipart/form-data"):
                        parser.parsemultipart()
                    elif contenttype.equals("application/json"):
                        parser.parsejson(None)
                    elif contenttype.equals("application/json charset=utf-8"):
                        parser.parsejson("utf-8")
                    else:
                        # logger.info("unknown request content-type: "+contenttype)
                        print("unknown request content-type: "+contenttype)

                scheme = request.headers.get("x-forwarded-proto", None)
                if scheme:
                    request.scheme = scheme
                response = self.server.handler.handle(request)
            except SyntaxError as se:
                # logger.warn("parse error\n"+request.rawhead.trim()+"\n",e)
                print("parse error\n"+request.rawhead.trim()+"\n")
                msg = str(se)
                if contenttype != None:
                    msg = "invalid content for content-type " + contenttype + "\n" + msg
                response = Response.errorResponse("BAD_REQUEST",msg)
            response.headers["connection"] = "close"
            response.headers["content-length"] = str(len(response.body))
            header = response.toheaderstring().getBytes()
    
            out = self.sock.getoutputStream()
            out.write(header)
            IoUtils.copyAll(response.body["content"],out)
            out.close()
            self.sock.close()
        except IOError as ioe:
            # logger.info("",e)
            print(ioe)
