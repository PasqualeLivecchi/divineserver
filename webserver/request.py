import socket

class Request:
    rawhead = ""
    method = ""
    rawpath = ""
    originalpath = ""
    path = ""
    protocol = "" # only HTTP/1.1 is accepted
    scheme = ""
    headers = {}
    parameters = {}
    cookies = {}
    body = b""
    multipartfile = {"filename":None,"contenttype":None,"content":None}
