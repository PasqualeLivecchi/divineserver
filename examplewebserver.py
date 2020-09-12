from webserver.response import Response
from webserver.server import Server
from webserver.handlers import MapHandler,FileHandler,ListHandler,DirHandler,LogHandler,SafeHandler,ContentTypeHandler, Handler
from webserver import util

class Cookies(Handler):
    def handle(self,request):
        response = Response()
        name = request.parameters.get("name", None)
        if name:
            attributes = {}
            value = request.parameters.get("value", None)
            if value:
                response.setcookie(name,value,attributes)
            else:
                attributes["Max-Age"] = "0"
                response.setcookie(name,"delete",attributes)

        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            writer = open(response, "w")
            for k,v in request.cookies.items():
                writer.write(k+" = "+v+"\n")
            writer.close()
            return response
        except IOError as ioe:
            raise Exception(ioe)
        finally:
            writer.close()


class Example(Handler):
    def handle(self,request):
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            writer = open(response, "w")
            writer.write("Hello World\n")
            writer.close()
            return response
        except IOError as ioe:
            raise Exception("shouldn't happen",e)
        finally:
            writer.close()

def simple(): # throws IOError {
    handler = Example()
    Server(8080,handler).start()
    

def fancy(): # throws IOError {
    kv = {}
    kv.get("/hello", Example())
    kv.get("/headers", Headers())
    kv.get("/params", Params())
    kv.get("/cookies", Cookies())
    kvhandler = MapHandler(kv)
    filehandler = FileHandler()
    dirhandler = DirHandler(filehandler)
    handler = ListHandler([kvhandler, filehandler, dirhandler])
    handler = ContentTypeHandler(handler)
    handler = SafeHandler(handler)
    handler = LogHandler(handler)
    Server(8080,handler).start()


class Headers(Handler):

    def handle(self,request):
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            writer = open(response, "w")
            for k,v in request.headers.items():
                writer.write(k+": "+v+"\n")
            writer.close()
            return response
        except IOError as ioe:
            raise Exception(ioe)
        finally:
            writer.close()


class Params(Handler):

    def handle(self,request):
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            writer = open(response, "w")
            for k,v in request.parameters.items():
                writer.write(k+" = "+v+"\n")
            writer.close()
            return response
        except IOError as ioe:
            raise Exception(ioe)
        finally:
            writer.close()

if __name__ == '__main__':
    simple()
    fancy()
