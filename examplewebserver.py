from webserver.response import Response
from webserver.request import Request
from webserver.server import Server
from webserver.handlers import MapHandler,FileHandler,ListHandler,DirHandler,LogHandler,SafeHandler,ContentTypeHandler, Handler
from webserver import util
from io import BufferedRandom
import pickle

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
        print(response.headers)
        try:
            response.body['content'] = "Hello World\n"#(str("Hello World\n").encode(response.headers.get('content-type',"text/plain charset=utf-8").split('charset=')[1]))
            response.body['length'] = len(response.body['content'])
            print("dumps",pickle.dumps(response))
            return pickle.dumps(response)
        except Exception as ioe:
            raise IOError(f"Shouldn't happen {ioe}")

def simple(): # throws IOError {
    handler = Example()
    print(handler.handle(Request()))
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
            s =""
            for k,v in request.headers.items():
                response.body[k] = v
            return pickle.dumps(response)

            # with open(response, "wb+") as w:
            #     for k,v in request.headers.items():
            #         w.write(k+": "+v+"\n")
            #     return response
        except IOError as ioe:
            raise Exception(ioe)


class Params(Handler):
    def handle(self,request):
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            s =""
            for k,v in request.parameters.items():
                s += k+" = "+v+"\n"
            response.body = s
            return pickle.dumps(response)
            # with open(response, "wb+") as w:
            #     for k,v in request.parameters.items():
            #         w.write(k+" = "+v+"\n")
            #     return response
        except IOError as ioe:
            raise Exception(ioe)

if __name__ == '__main__':
    # simple()
    fancy()
