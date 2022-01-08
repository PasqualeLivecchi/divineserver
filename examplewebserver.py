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
        response.body['content'] = "WTF is this muhahah.\nHey today baby I got your money don't ya worry say hey, baby I got your money."
        response.body['length'] = len(response.body['content'])
        return response
    except Exception as e:
        raise IOError(f"Shouldn't happen {e}")


def simple(request):
    Server(handleexample).start()

# still working out the kinks
def fancy(request):
    kvhandler = handledict(request,{"/headers": handlehttpheaders(), "/params": handlehttpparams(), "/cookies": handlehttpcookies()})
    print("maphandler ok")
    filehandler = handlefile(request)
    print("filehandler ok")
    dirhandler = handledir(filehandler)
    print("dirhandler ok")
    listhandler = handlelist([kvhandler, filehandler, dirhandler])
    print("listhandler ok")
    cthandler = handlecontenttype(listhandler)
    print("contenttypehandler ok")
    safehandler = handlesafely(cthandler)
    print("safehandler ok")
    # loghandler = LogHandler(safehandler)
    Server(safehandler, 8080).start()


if __name__ == '__main__':
    print("simple")
    simple(None)
    # print("fancy")
    # fancy()


