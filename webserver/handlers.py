from .status import getstatus
from .request import Request
from .response import Response
import os,datetime

class Handler:
    def handle(request):
        print("handler not implemented")
        return "handler not implemented"


class SafeHandler(Handler):
    # # private static final Logger logger = LoggerFactory.getLogger(SafeHandler.class)

    def __init__(self, handler):
        self.handler = handler

    def handle(self, request):
        try:
            response = self.handler.handle(request)
            if response:
                return response
            return Response.errorresponse( getstatus(404), request.path+" not found\n" )
        except RuntimeError as e:
            # logger.error("",e)
            print(e)
            response = Response()
            response.status = getstatus(500)
            response.headers.put("content-type", "text/plain charset=utf-8")
            writer = open(response, "w")
            writer.write( "Internel Server Error\n\n" )
            writer.write(e.with_traceback())
            writer.close()
            return response
        finally:
            writer.close()



class MapHandler(Handler):
    kv = {}

    def __init__(self,kv):
        self.kv = kv

    def handle(self,request):
        handler = self.kv.get(request.path, None)
        return handler if handler else self.handler.handle(request)


class LogHandler(Handler):
    # private static final Logger logger = LoggerFactory.getLogger("HTTP")
    handler = None
    def __init__(self, handler):
        self.handler = handler

    def handle(self,request):
        response = self.handler.handle(request)
        # logger.info( request.method + " " + request.path + " " + response.status.code + " " + response.body.length )
        print(request.method + " " + request.path + " " + response.status.code + " " + response.body.length)
        return response

class ListHandler(Handler):
    handlers = []

    def __init__(self, handlers):
        self.handlers = handlers

    def handle(self,request):
        for handler in self.handlers:
            response = handler.handle(request)
            if response:
                return response
        return None


class IndexHandler(Handler):
    handler = None
    indexname = None

    def __init__(self, handler, indexname):
        self.handler = handler
        self.indexname = indexname

    def handle(self,request):
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
    dirfile = None

    def __init__(self,dirfile="."): # FileHandler() {
        if os.path.isdir(dirfile):
            self.dirfile = dirfile
        else:
            raise RuntimeError("must be a directory")


    def f(self, request):
        return self.dirfile,request.path


    def handle(self, request):
        try:
            f = open(request)
            if os.path(f).isfile():
                response = Response()
                response.body['length'],response.body['content'] = os.stat(f).st_size,open(f, 'r')
                return response
            return None
        except IOError as ioe:
            raise RuntimeError(ioe)

# class FileHandler(Handler):
#     f = None

#     def __init__(self, pathname="."):
#         if os.path.isfile(pathname):
#             self.f = pathname
#         else:
#             raise RuntimeError("must be a file")

#     def f(self, request):
#         return self.f,request.path

#     def handle(self,request):
#         try:
#             f = self.f(request)
#             if os.path.isfile(f):
#                 response = Response()
#                 response.body[length],response.body[content] = len(f),open(f, 'r')
#                 return response
#             return None
#         except IOError as ioe:
#             raise RuntimeError(ioe)


# class DomainHandler(Handler):
#     # private static final Logger logger = LoggerFactory.getLogger(DomainHandler.class)

#     public interface Factory {
#         public Handler newHandler(domain)

#     private static class MyTask extends TimerTask {
#         private final Set<Handler> dontGc

#         MyTask(Set<Handler> dontGc):
#             self.dontGc = dontGc

#         public void run():
#             dontGc.clear()
#             logger.info("dontGc.clear()")

#     private static final long HOUR = 1000L*60*60
#     private final Map<String,Handler> map = new SoftCacheMap<String,Handler>()
#     private final Set<Handler> dontGc = ConcurrentHashMap.newKeySet()
#     private final Timer timer = new Timer()

#     private final Factory factory

#     public DomainHandler(Factory factory):
#         self.factory = factory
#         timer.schedule(new MyTask(dontGc),HOUR,HOUR)

#     protected void finalize() throws Throwable {
#         timer.cancel()

#     def handle(self,request):
#         host = (String)request.headers.get("host")
#         if host == None )
#             return None
#         int i = host.indexOf(':')
#         domain = i == -1 ? host : host.substring(0,i)
#         Handler handler = getHandler(domain)
#         if handler==None )
#             return None
#         dontGc.add(handler)
#         return self.handler.handle(request)

#     public Handler getHandler(domain):
#         domain = domain.toLowerCase()
#         synchronized(map):
#             Handler handler = map.get(domain)
#             if handler == None ):
#                 //if(ref!=None) logger.info("gc "+domain)
#                 handler = factory.newHandler(domain)
#                 if handler == None )
#                     return None
#                 map.put(domain,handler)
#             return handler

#     public void removeHandler(domain):
#         //logger.info("removeHandler "+domain)
#         domain = domain.toLowerCase()
#         synchronized(map):
#             Handler handler = map.remove(domain)
#             if handler != None ):
#                 close(handler)

#     private static void close(Handler handler):
#         if handler instanceof Closeable ):
#             try:
#                 ((Closeable)handler).close()
#             } catch(IOError e):
#                 logger.error(handler.toString(),e)


class DirHandler(Handler):
    directory = None

    def __init__(self, directory):
        self.directory = directory

    def handle(self,request):
        try:
            f = self.directory.f(request)
            if request.path.endswith("/") and os.path.isdir(f):
                datefmt = "yyyy-MM-dd HH:mm:ss zzz"
                response = Response()
                response.headers["content-type"] = "text/html charset=utf-8"
                writer = open(response,"w") # OutputStreamWriter(ResponseOutputStream(response))
                writer.write("<!doctype html><html>")
                writer.write("<head><style>td{{padding: 2px 8px}}</style></head>")
                writer.write("<body>")
                writer.write("<h1>Directory: "+request.path+"</h1>")
                writer.write("<table border=0>")
                flist = [_file for _file in os.listdir(self.directory)]
                sorted(flist)
                for child in flist:
                    name = child
                    if os.path.isdir(child):
                        name += '/'
                    writer.write("<tr>")
                    writer.write("<td><a href='"+name+"'>"+name+"</a></td>")
                    writer.write("<td>"+os.stat(child).st_size+" bytes</td>")
                    writer.write("<td>"+os.path.getmtime(child).strftime(datefmt)+"</td>")
                    writer.write("</tr>")
                writer.write( "</table>" )
                writer.write( "</body>" )
                writer.write( "</html>" )
                writer.close()
                return response
            return None
        except IOError as ioe:
            raise RuntimeError(ioe)
        finally:
            writer.close()



class ContentTypeHandler(Handler):
    handler = None
    kv = {}
    contenttype4noextension = None

    def __init__(self, handler,charset="utf-8"):
        self.handler = handler
        attrs = f" charset={charset}" if charset else ""
        htmltype = "text/html" + attrs
        texttype = "text/plain" + attrs
        self.contenttype4noextension = htmltype
        self.kv["html"] = htmltype
        self.kv["txt"] = texttype
        self.kv["luan"] = texttype
        self.kv["css"] = "text/css"
        self.kv["js"] = "application/javascript"
        self.kv["json"] = "application/json" + attrs
        self.kv["mp4"] = "video/mp4"
        # add more as need

    def handle(self,request):
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
                _type = map.get( extension.toLowerCase() )
            if _type:
                response.headers.get("content-type",_type)
        return response
