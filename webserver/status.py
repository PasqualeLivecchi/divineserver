
statuscode = {
        200: "OK",
        301: "Moved Permanently",
        302: "Found",
        400: "Bad Request",
        404: "Not Found",
        500: "Internal Server Error"
    }
def getstatus(code):
    return statuscode[code] if code in statuscode.keys() else ""

# def getcode(status):
#     return code for code,s in statuscode.items() if status 
# print(getstatus(302))
# assert getstatus(200), "OK"
# assert getstatus(300), ""