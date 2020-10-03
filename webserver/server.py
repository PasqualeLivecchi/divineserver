from .handlers import Handler
import socket,os,asyncio,mmap,pickle



class Server:
    def __init__(self, handler, port):
        self.handler = handler
        self.port = port
        self.loop = asyncio.get_event_loop()
        host = 'localhost'
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM,0)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR,1)
        self.sock.setblocking(False)
        self.sock.bind((host, port))
        self.sock.listen(10)

    async def serve(self):
        while True:
            conn, addr = await self.loop.sock_accept(self.sock)
            self.loop.create_task(self.hunt(conn))


    async def hunt(self, conn, handler=None):
        if not handler:
            handler = self.handler
        # if isinstance(handler,Handler):
            # print(type(handler))
        while True:
            msg = await self.loop.sock_recv(conn,8096)
            if not msg:
                break
            await self.loop.sock_sendall(conn, msg)
        conn.close()
        # else:
        #     raise ValueError("Handler required")

    def run(self):
        try:
            self.loop.create_task(self.serve())
        except IOError as ioe:
            print(f"error: {ioe}")
        print(f"started server on port {self.port}")
        self.loop.run_forever()

    def shutdown(self):
        try:
            self.loop.stop()
            isstopped = self.loop.is_closed()
            if isstopped:
                print("stopped server on port "+self.port)
            else:
                print("couldn't stop server on port "+self.port)
            return isstopped
        finally:
            self.loop.close()



#     def newserversocket(self):
#         return socket.socket(socket.AF_INET,socket.SOCK_DGRAM)

#     def start(self):
#         ssock = self.newserversocket()
#         try:
#             ssock.bind()
#             sockaccept = ssock.accept()
#             Connection(self,sockaccept).handle()
#         except IOError as ioe:
#             # logger.error("",e)
#             print(f"error: {ioe}")
#         # logger.info("started server on port "+self.port)
#         print(f"started server on port {self.port}")

#     def stop(self,timeoutinseconds):
#         try:
#             self.threadpool.shutdownnow()
#             isstopped = self.threadpool.awaittermination(timeoutinseconds) # TimeUnit.SECONDS)
#             if isstopped:
#                 # logger.info("stopped server on port "+self.port)
#                 print("stopped server on port "+self.port)
#             else:
#                 # logger.warn("couldn't stop server on port "+port)
#                 print("couldn't stop server on port "+self.port)
#             return isstopped
#         except InterruptedError as ie:
#             raise RuntimeError(ie)

# class ForAddress(Server):
#     addr = None
#     def __init__(self, port, handler, addr=socket.gethostbyname('localhost')):
#         super(port,handler)
#         self.port = port
#         self.addr = addr

#     def newserversocket(self):
#         return socket(self.port,0,self.addr)
