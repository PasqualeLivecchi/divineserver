from .requestparser import RequestParser
from .request import Request
from .response import Response
from .parser import ParseException
import asyncio,socket


async def msghandler(server,connsock):
    print("connection handlemsg")
    loop,twoMB = server.loop,16_777_216
    try:
        while (msg := await loop.sock_recv(connsock,twoMB)):
            # msg = await loop.sock_recv(connsock,twoMB)
            print(msg)
            await loop.sock_sendall(connsock,msg)
    except Exception as e:
        print(f"Error handling message{e}")
    finally:
        connsock.close()


async def connectionhandler(server,connsock):
    print("connection handleconnection")
    try:
        loop,request,resp,contenttype,ordr,ordn,twoMB,endofheader,bufbreak = server.loop,Request(),None,None,ord('\r'),ord('\n'),16_777_216,0,False
        try:
            buf = await loop.sock_recv(connsock,twoMB)
            # msg = await loop.sock_recv(connsock,twoMB)
            # print(f"buf:{buf}")
            size = len(buf)
            if buf == -1:
                if size == 0:
                    print("buf == -1 and size == 0")
                    connsock.close()
                    return
                raise IOError("unexpected end of input at "+size)
            for i in range(0,size-3):
                if buf[i]==ordr and buf[i+1]==ordn and buf[i+2]==ordr and buf[i+3]==ordn:
                    endofheader = i+4
                    break
            request.rawhead = buf[0:endofheader].decode()
            reqparser = RequestParser(request)
            reqparser.parsehead()
            lenstr = reqparser.request.headers.get("content-length")
            print(f"lenstr:{lenstr}")
            if lenstr:
                length,cursor = int(lenstr),0
                body = bytes(buf[x] for x in range(endofheader,length))
                while cursor < length:
                    n = await loop.sock_recv_into(connsock,body)
                    print(f"connection handle after sock_recv_into n:{n}")
                    if n < 0:
                        raise IOError("unexpected end of input at "+cursor)
                    cursor += n
                request.body = body
            contenttype = reqparser.request.headers.get("content-type")
            print(f"connection handle contenttype={contenttype}")
            if contenttype:
                print("connection handle request contenttype")
                contenttypelow = contenttype.lower()
                if contenttypelow == "application/x-www-form-urlencoded":
                    print("application/x-www-form-urlencoded")
                    reqparser.parseurlencoded(None)
                elif contenttypelow == "application/x-www-form-urlencoded; charset=utf-8":
                    print("application/x-www-form-urlencoded; charset=utf-8")
                    reqparser.parseurlencoded("utf-8")
                elif contenttypelow.startswith("multipart/form-data"):
                    print("multipart/form-data")
                    reqparser.parsemultipart()
                elif contenttypelow == "application/json":
                    print("application/json")
                    reqparser.parsejson(None)
                elif contenttypelow == "application/json; charset=utf-8":
                    print("application/json; charset=utf-8")
                    reqparser.parsejson("utf-8")
                else:
                    print(f"unknown request contenttype: {contenttype}")
            scheme = reqparser.request.headers.get("x-forwarded-proto", None)
            if scheme:
                reqparser.request.scheme = scheme
            resp = server.handler(reqparser.request)
        except ParseException as pe:
            print("connectionhandler parseexception:" + str(pe))
            msg = ""
            if contenttype:
                msg += "invalid content for content-type " + contenttype + "\n"
            msg += "parse error\n" + request.rawhead.strip() + "\n" + str(pe)
            resp = Response.errorresponse(400, msg)
        # print(f"response before: {resp}")
        resp.headers["connection"] = "close"
        resp.headers["content-length"] = str(resp.body['length'])
        header = resp.toheaderstring().encode("utf-8")
        body = resp.body['content'].encode('utf-8')
        await loop.sock_sendall(connsock, header)
        # print(f"connection handle header sent:{header}")
        await loop.sock_sendall(connsock, body)
        # print(f"connection handle body sent:{body}")
        connsock.close()
    except IOError as ioe:
        print(f"connection error {ioe}")


"""
class Connection:
    def __init__(self, server, sock):
        self.server = server
        self.sock = sock
        self.loop = self.server.loop

    async def handle(self):
        print("connection handle")
        sixty4kb = 65536
        try:
            request,response,contenttype,ordr,ordn = Request(),None,None,ord('\r'),ord('\n')
            try:
                buf = await self.loop.sock_recv(self.sock,sixty4kb)
                # print(f"handle buf:{buf}")
                size = len(buf)
                print(f"size:{size}")
                if buf == -1:
                    print("buf == -1")
                    if size == 0:
                        print("buf == -1 and size <= 0")
                        self.sock.close()
                        return
                    raise IOError("unexpected end of input at "+size)
                endofheader = size
                for i in range(0,size-3):
                    if buf[i]==ordr and buf[i+1]==ordn and buf[i+2]==ordr and buf[i+3]==ordn:
                        endofheader = i+4
                        print(f"break endofheader:{endofheader} :{buf[i]}{buf[i+1]}{buf[i+2]}{buf[i+3]}endofheader: {endofheader} started buf: {size}")
                        break

                rawhead = buf[0:endofheader]
                request.rawhead = rawhead.decode()
                reqparser = RequestParser(request)
                reqparser.parsehead()
                lenstr = reqparser.request.headers.get("content-length")
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

                contenttype = reqparser.request.headers.get("content-type")
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

                scheme = reqparser.request.headers.get("x-forwarded-proto", None)
                if scheme:
                    reqparser.request.scheme = scheme
                response = self.server.handler(reqparser.request)
            except Exception as e:
                # logger.warn("parse error\n"+request.rawhead.trim()+"\n",e)
                msg = f"parse error\r\n{request.rawhead.strip()}\r\n{e}"
                if contenttype:
                    msg = "invalid content for content-type " + contenttype + "\r\n" + msg
                response = Response.errorresponse(400, msg)
            # print(f"response before: {response}")
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
"""