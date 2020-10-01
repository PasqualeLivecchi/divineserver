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
            with open(response, "w") as w:
                for k,v in request.cookies.items():
                    writer.write(k+" = "+v+"\n")
                return response
        except IOError as ioe:
            raise Exception(ioe)


class Example(Handler):
    def handle(self,request):
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            with open(response, "w") as w:
                w.write("Hello World\n")
                return response
        except Exception as ioe:
            raise IOError(f"Shouldn't happen {ioe}")

def simple(): # throws IOError {
    handler = Example()
    Server(handler,8080).run()

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
    Server(handler,8080).run()


class Headers(Handler):
    def handle(self,request):
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            with open(response, "w") as w:
                for k,v in request.headers.items():
                    w.write(k+": "+v+"\n")
                return response
        except IOError as ioe:
            raise Exception(ioe)


class Params(Handler):
    def handle(self,request):
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            with open(response, "w") as w:
                for k,v in request.parameters.items():
                    w.write(k+" = "+v+"\n")
                return response
        except IOError as ioe:
            raise Exception(ioe)

if __name__ == '__main__':
    simple()
    # fancy()
