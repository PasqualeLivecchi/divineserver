from webserver.response import Response
from webserver.request import Request
from webserver.server import Server
from webserver.handlers import MapHandler, FileHandler, ListHandler, DirHandler, LogHandler, SafeHandler, ContentTypeHandler, Handler
from webserver import util
from io import BufferedRandom


class Cookies(Handler):
    def handle(self, request):
        print("cookies handler")
        response = Response()
        name = request.parameters.get("name", None)
        if name:
            attributes = {}
            value = request.parameters.get("value", None)
            if value:
                response.setcookie(name, value, attributes)
            else:
                attributes["Max-Age"] = "0"
                response.setcookie(name, "delete", attributes)

        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            s = ""
            for k, v in request.cookies.items():
                s += k + " = " + v + "\n"
            response.body['content'] = s
            response.body['length'] = len(s)
            return response
        except IOError as ioe:
            raise Exception(ioe)


class Example(Handler):
    def handle(self, request):
        print("example handler")
        response = Response()
        response.headers["content-type"] = "text/plain charset=utf-8"
        try:
            response.body['content'] = "WTF is this shit\n"
            response.body['length'] = len(response.body['content'])
            print(f"example handle responsevars:{vars(response)}")
            return response
        except IOError as ioe:
            raise IOError(f"Shouldn't happen {ioe}")


class Headers(Handler):
    def handle(self, request):
        print("headers handler")
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            s = ""
            for k, v in request.headers.items():
                s += f"{k}: {v}\n"
            response.body['content'] = s
            response.body['length'] = len(s)
            return response
        except IOError as ioe:
            raise IOError(ioe)


class Params(Handler):
    def handle(self, request):
        print("params handler")
        response = Response()
        response.headers.get("content-type", "text/plain charset=utf-8")
        try:
            s = ""
            for k, v in request.parameters.items():
                s += f"{k} = {v}\n"
            response.body['content'] = s
            response.body['length'] = len(s)
            return response
            # with open(response, "wb+") as w:
            #     for k,v in request.parameters.items():
            #         w.write(k+" = "+v+"\n")
            #     return response
        except IOError as ioe:
            raise IOError(ioe)

def simple():  # throws IOError {
    Server(Example(), 8080).start()


def fancy():  # throws IOError {
    # kv = {"/hello": Example(), "/headers": Headers(), "/params": Params(), "/cookies": Cookies()}
    # print("maphandler ok")
    # filehandler = FileHandler()
    # print("filehandler ok")
    # dirhandler = DirHandler(filehandler)
    # print("dirhandler ok")
    # listhandler = ListHandler([kvhandler, filehandler, dirhandler])
    # print("listhandler ok")
    # cthandler = ContentTypeHandler(listhandler)
    # print("contenttypehandler ok")
    # safehandler = SafeHandler(cthandler)
    # print("safehandler ok")
    # # loghandler = LogHandler(safehandler)
    Server(SafeHandler(Example()), 8080).start()


if __name__ == '__main__':
    # simple()
    print("fancy")
    fancy()
