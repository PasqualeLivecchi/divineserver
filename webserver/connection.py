from .requestparser import RequestParser
from .request import Request
from .response import Response
from copy import deepcopy
import socket,asyncio

class Connection:
    def __init__(self, server, sock):
        self.server = server
        self.sock = sock
        self.loop = self.server.loop

    async def handle(self):
        print("connection handle")
        onegb = 1073741824
        try:
            request,response,contenttype,ordr,ordn = Request(),None,None,ord('\r'),ord('\n')
            try:
                buf = await self.loop.sock_recv(self.sock,onegb)
                # print(f"handle buf:{buf}")
                size = len(buf)
                print(f"size:{size}")
                endofheader = size
                for i in range(0,size-3):
                    if buf[i]==ordr and buf[i+1]==ordn and buf[i+2]==ordr and buf[i+3]==ordn:
                        endofheader = i+4
                        print(f"break endofheader:{endofheader}")
                        break
                print(f"connection handle endofheadertest:{buf[-4]}{buf[-3]}{buf[-2]}{buf[-1]}endofheader: {endofheader} started buf: {size}")
                rawhead = buf[0:endofheader]
                # print(f"handle rawhead type:{type(rawhead)} rawhead:{rawhead}")
                request.rawhead = rawhead.decode()
                reqparser = RequestParser(request)
                print(f"connection handle requestparser created requestparservars:{vars(reqparser)}")
                reqparser.parsehead()
                lenstr = request.headers.get("content-length")
                print(f"lenstr:{lenstr}")
                if lenstr:
                    length = int(lenstr)
                    body = bytes(buf[x] for x in range(endofheader,length))
                    while size < length:
                        n = await self.loop.sock_recv_into(self.sock,body)
                        print(f"connection handle after sock_recv_into n:{n}")
                        if n < 0:
                            raise IOError("unexpected end of input at "+size)
                        size += n
                    request.body = body
                    # System.out.println(new String(request.body))

                contenttype = request.headers.get("content-type")
                print(f"connection handle contenttype={contenttype}")
                if contenttype:
                    print("connection handle request contenttype")
                    contenttypelow = contenttype.lower()
                    if contenttypelow == "application/x-www-form-urlencoded":
                        print("application/x-www-form-urlencoded")
                        reqparser.parseurlencoded(None)
                    elif contenttypelow == "application/x-www-form-urlencoded charset=utf-8":
                        print("application/x-www-form-urlencoded charset=utf-8")
                        reqparser.parseurlencoded("utf-8")
                    elif contenttypelow.startswith("multipart/form-data"):
                        print("multipart/form-data")
                        reqparser.parsemultipart()
                    elif contenttypelow == "application/json":
                        print("application/json")
                        reqparser.parsejson(None)
                    elif contenttypelow == "application/json charset=utf-8":
                        print("application/json charset=utf-8")
                        reqparser.parsejson("utf-8")
                    else:
                        # logger.info("unknown request content-type: "+contenttype)
                        print("unknown request contenttype: "+contenttype)

                scheme = request.headers.get("x-forwarded-proto", None)
                if scheme:
                    request.scheme = scheme
                response = self.server.handler.handle(request)
            except Exception as e:
                # logger.warn("parse error\n"+request.rawhead.trim()+"\n",e)
                msg = f"parse error\n{request.rawhead.strip()}\n{e}"
                if contenttype:
                    msg = "invalid content for content-type " + contenttype + "\n" + msg
                response = Response.errorresponse(400, msg)
            response.headers["connection"] = "close"
            response.headers["content-length"] = str(response.body['length'])
            header = response.toheaderstring().encode("utf-8")
            body = response.body['content'].encode('utf-8')
            await self.loop.sock_sendall(self.sock, header)
            print(f"connection handle header sent:{header}")
            await self.loop.sock_sendall(self.sock, body)
            print(f"connection handle body sent:{body}")
            self.sock.close()
        except IOError as ioe:
            # logger.info("",e)
            print(ioe)
