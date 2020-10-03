from .status import getstatus
from .request import Request
from .response import Response
import os,datetime

class Handler:
    def handle(request):
        print("handler not implemented")
        return "handler not implemented"


class SafeHandler(Handler):
    def __init__(self, handler):
        self.handler = handler

    def handle(self, request):
        print("safe handler")
        try:
            response = self.handler.handle(request)
            if response:
                return response
            return Response.errorresponse(getstatus(404), request.path+" not found\n" )
        except RuntimeError as e:
            print(e)
            response = Response()
            response.status = getstatus(500)
            response.headers.put("content-type", "text/plain charset=utf-8")
            response.body['content'] = f"Internel Server Error\n\n{e.with_traceback()}"
            return response



class MapHandler(Handler):
    def __init__(self,kv={}):
        self.kv = kv

    def handle(self,request):
        print("map handler")
        handler = self.kv.get(request.path, None)
        return handler if handler else handler.handle(request)


class LogHandler(Handler):
    # private static final Logger logger = LoggerFactory.getLogger("HTTP")
    def __init__(self, handler):
        self.handler = handler

    def handle(self,request):
        print("log handler")
        response = self.handler.handle(request)
        print(request.method + " " + request.path + " " + response.status.code + " " + response.body.length)
        return response

class ListHandler(Handler):
    def __init__(self, handlers=[]):
        self.handlers = handlers

    def handle(self,request):
        print("list handler")
        for handler in self.handlers:
            response = handler.handle(request)
            if response:
                return response
        return None


class IndexHandler(Handler):
    def __init__(self, handler, indexname):
        self.handler = handler
        self.indexname = indexname

    def handle(self,request):
        print("index handler")
        if request.path.endswith("/"):
            path = request.path
            try:
                request.path += indexname
                return self.handler.handle(request)
            finally:
                request.path = path
        else:
            return self.handler.handle(request)

class FileHandler(Handler):
    def __init__(self,dirfile="."):
        if os.path.isdir(dirfile):
            self.dirfile = dirfile
        else:
            raise RuntimeError("Directory required")

    def fyle(self, request):
        return open(os.path.join(self.dirfile,request.path))

    def handle(self, request):
        print("file handler")
        try:
            f = fyle(request)
            if os.path(f).isfile():
                response = Response()
                response.body['length'],response.body['content'] = os.stat(f).st_size,open(f, 'rb')
                return response
            return None
        except IOError as ioe:
            raise RuntimeError(ioe)


class DirHandler(Handler):
    def __init__(self, directory):
        if isinstance(directory,FileHandler):
            self.directory = directory
        else:
            ValueError("FileHandler required")

    def handle(self,request):
        print("dir handler")
        try:
            f = self.directory.fyle(request)
            if request.path.endswith("/") and os.path.isdir(f):
                datefmt = "yyyy-MM-dd HH:mm:ss zzz"
                response = Response()
                response.headers["content-type"] = "text/html charset=utf-8"
                with open(response,"w") as w:
                    w.write("<!doctype html><html>")
                    w.write("<head><style>td{{padding: 2px 8px}}</style></head>")
                    w.write("<body>")
                    w.write("<h1>Directory: "+request.path+"</h1>")
                    w.write("<table border=0>")
                    flist = [_file for _file in os.listdir(self.directory)]
                    sorted(flist)
                    for child in flist:
                        name = child
                        if os.path.isdir(child):
                            name += '/'
                        w.write("<tr>")
                        w.write("<td><a href='"+name+"'>"+name+"</a></td>")
                        w.write("<td>"+os.stat(child).st_size+" bytes</td>")
                        w.write("<td>"+os.path.getmtime(child).strftime(datefmt)+"</td>")
                        w.write("</tr>")
                    w.write( "</table>" )
                    w.write( "</body>" )
                    w.write( "</html>" )
                    return response
            return None
        except IOError as ioe:
            raise RuntimeError(ioe)


class ContentTypeHandler(Handler):
    def __init__(self, handler,kv={},charset="utf-8"):
        self.handler = handler
        self.kv = kv
        attrs = f" charset={charset}" if charset else ""
        htmltype = "text/html" + attrs
        texttype = "text/plain" + attrs
        self.contenttype4noextension = htmltype
        self.kv["html"] = htmltype
        self.kv["txt"] = texttype
        self.kv["css"] = "text/css"
        self.kv["js"] = "application/javascript"
        self.kv["json"] = "application/json" + attrs
        self.kv["mp4"] = "video/mp4"
        # add more as need

    def handle(self,request):
        print("contenttype handler")
        response = self.handler.handle(request)
        if response and "content-type" not in response.headers.keys():
            path = request.path
            intslash = path.rindex('/')
            intdot = path.rindex('.')
            _type = None
            if intdot < intslash:  # no extension
                _type = contenttype4noextension
            else: # extension
                extension = path.substring(intdot+1)
                _type = self.kv.get( extension.toLowerCase() )
            if _type:
                response.headers.get("content-type",_type)
        return response
