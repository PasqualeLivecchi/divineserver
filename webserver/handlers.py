from .status import getstatus
from .request import Request
from .response import Response
import os,datetime

# class Handler:
#     def handle(request):
#         print("handler not implemented")
#         return "handler not implemented"

def handlesafely(request,handler):
    print("handlesafely")
    try:
        response = handler.handle(request)
        return response if response else Response.errorresponse(404, request.path + " not found\n")
    except RuntimeError as e:
        print("handlesafely error")
        response = Response()
        response.status = getstatus(500)
        response.headers['content-type'] = "text/plain; charset=utf-8"
        response.body['content'] = f"Internal Server Error\n\n{e.with_traceback()}"
        return response


# class SafeHandler(Handler):
#     def __init__(self, handler):
#         self.handler = handler
#
#     def handle(self, request):
#         print("safe handler")
#         try:
#             response = self.handler.handle(request)
#             if response:
#                 print(f"safe response: {vars(response)}")
#                 return response
#             else:
#                 response404 = Response.errorresponse(404, request.path + " not found\n")
#                 print(f"safe response 404: {response404}")
#                 return response404
#         except RuntimeError as e:
#             print(e)
#             response = Response()
#             response.status = getstatus(500)
#             response.headers['content-type'] = "text/plain; charset=utf-8"
#             response.body['content'] = f"Internal Server Error\n\n{e.with_traceback()}"
#             return response

def handledict(request, dictofhandlers={}):
    handler = dictofhandlers.get(request.path, None)
    return handler if handler else handler(request)

# class MapHandler(Handler):
#     def __init__(self,kv={}):
#         self.kv = kv
#
#     def handle(self,request):
#         print("map handler")
#         handler = self.kv.get(request.path, None)
#         response = handler if handler else handler.handle(request)
#         print(f"map response: {vars(response)}")
#         return response

def handlelog(request,handler):
    response = handler(request)
    log.info(f"{request.method} {request.path} {response.status} {response.body['length']}")
    return response



# class LogHandler(Handler):
#     # private static final Logger logger = LoggerFactory.getLogger("HTTP")
#     def __init__(self, handler):
#         self.handler = handler
#
#     def handle(self,request):
#         print("log handler")
#         response = self.handler.handle(request)
#         print(f"{request.method} {request.path} {response.status.code} {response.body.length}")
#         print(f"log response:{vars(response)}")
#         return response

def handlelist(request,listofhandlers):
    for handler in listofhandlers:
        response = handler(request)
        if response:
            return response
    return None


# class ListHandler(Handler):
#     def __init__(self, handlers=[]):
#         self.handlers = handlers
#
#     def handle(self,request):
#         print("list handler")
#         for i,handler in enumerate(self.handlers):
#             response = handler.handle(request)
#             if response:
#                 print(f"list response {i}: {vars(response)}")
#                 yield response
#         return None


class IndexHandler(Handler):
    def __init__(self, handler, indexname):
        self.handler = handler
        self.indexname = indexname

    def handle(self,request):
        print("index handler")
        try:
            if request.path.endswith("/"):
                path = request.path
                request.path += self.indexname
            response = self.handler.handle(request)
            print(f"index response: {vars(response)}")
            return response
        finally:
            request.path = path

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
            f = self.fyle(request)
            response = None
            if os.path(f).isfile():
                response = Response()
                response.body['length'],response.body['content'] = os.stat(f).st_size,open(f, 'rb')
            print(f"file response: {vars(response)}")
            return response
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
            f,response = self.directory.fyle(request),None
            if os.path.isdir(f): #request.path.endswith("/") 
                datefmt = "yyyy-MM-dd HH:mm:ss zzz"
                response = Response()
                response.headers["content-type"] = "text/html charset=utf-8"
                response.body['content'] = "<!doctype html><html>"
                response.body['content'] += "<!doctype html><html>"
                response.body['content'] += "<head><style>td{{padding: 2px 8px}}</style></head>"
                response.body['content'] += "<body>"
                response.body['content'] += "<h1>Directory: "+request.path+"</h1>"
                response.body['content'] += "<table border=0>"
                flist = [_file for _file in os.listdir(self.directory)]
                sorted(flist)
                for child in flist:
                    name = child
                    if os.path.isdir(child):
                        name += '/'
                    response.body['content'] += "<tr>"
                    response.body['content'] += "<td><a href='"+name+"'>"+name+"</a></td>"
                    response.body['content'] += "<td>"+os.stat(child).st_size+" bytes</td>"
                    response.body['content'] += "<td>"+os.path.getmtime(child).strftime(datefmt)+"</td>"
                    response.body['content'] += "</tr>"
                response.body['content'] += "</table>"
                response.body['content'] += "</body>"
                response.body['content'] += "</html>"
                response.length = len(response.body['content'])
                print(f"dir response: {vars(response)}")
            return response
        except IOError as ioe:
            raise RuntimeError(ioe)


class ContentTypeHandler(Handler):
    def __init__(self, handler,kv={},charset="utf-8"):
        self.handler = handler
        self.kv = kv
        attrs = f"; charset={charset}" if charset else ""
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
            _type = self.contenttype4noextension
            if intdot < intslash:  # no extension
                _type = None #contenttype4noextension
            else: # extension
                extension = path.substring(intdot+1)
                _type = self.kv.get(extension.toLowerCase())
            if _type:
                response.headers.get("content-type",_type)
        print(f"contenttype response: {response}")
        return response
