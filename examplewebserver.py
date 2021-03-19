from webserver.response import Response
from webserver.request import Request
from webserver.server import Server
# from webserver.server import serve
from webserver.handlers import handledict, handlefile, handlelist, handledir, handlelog, handlesafely, handlecontenttype, handlehttpheaders, handlehttpparams, handlehttpcookies
from functools import partial


def handleexample(request):
    print("example handler")
    response = Response()
    response.headers["content-type"] = "text/plain; charset=utf-8"
    try:
        response.body['content'] = "WTF is this shit.\r\nHey today baby I got your money don't ya worry say hey, baby I got your money."
        response.body['length'] = len(response.body['content'])
        return response
    except Exception as e:
        raise IOError(f"Shouldn't happen {e}")


def simple():
    Server(handleexample).start()


def fancy():
    pass
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
    # Server(SafeHandler(Params()), 8080).start()


if __name__ == '__main__':
    print("simple")
    simple()


