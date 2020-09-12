from .connection import Connection
import socket,os


class Server:
    port = None
    handler = None
    threadpool = None

    def __init__(self, port, handler):
        self.port = port
        self.handler = handler

    def newserversocket(self):
        return socket.socket(socket.AF_INET,socket.SOCK_DGRAM)

    def start(self):
        ssock = self.newserversocket()
        try:
            ssock.bind()
            sockaccept = ssock.accept()
            Connection(self,sockaccept).handle()
        except IOError as ioe:
            # logger.error("",e)
            print(f"error: {ioe}")
        # logger.info("started server on port "+self.port)
        print(f"started server on port {self.port}")

    def stop(self,timeoutinseconds):
        try:
            self.threadpool.shutdownnow()
            isstopped = self.threadpool.awaittermination(timeoutinseconds) # TimeUnit.SECONDS)
            if isstopped:
                # logger.info("stopped server on port "+self.port)
                print("stopped server on port "+self.port)
            else:
                # logger.warn("couldn't stop server on port "+port)
                print("couldn't stop server on port "+self.port)
            return isstopped
        except InterruptedError as ie:
            raise RuntimeError(ie)

class ForAddress(Server):
    addr = None
    def __init__(self, port, handler, addr=socket.gethostbyname('localhost')):
        super(port,handler)
        self.port = port
        self.addr = addr

    def newserversocket(self):
        return socket(self.port,0,self.addr)
