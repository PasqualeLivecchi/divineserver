from .handlers import Handler
from .connection import Connection
import socket, asyncio


class Server:
    def __init__(self, handler, port, loop=asyncio.get_event_loop()):
        self.handler = handler
        self.port = port
        self.loop = loop
        host = 'localhost'
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM, 0)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.setblocking(False)
        self.sock.bind((host, port))
        self.sock.listen(10)

    async def run(self):
        try:
            while True:
                if self.loop.is_closed():
                    break
                connsock, addr = await self.loop.sock_accept(self.sock)
                self.loop.create_task(Connection(self,connsock).handle())
        except IOError as ioe:
            print("server start loop error",ioe)

    def start(self):
        try:
            self.loop.create_task(self.run())
        except IOError as ioe:
            print(f"error: {ioe}")
        print(f"started server on port {self.port}")
        self.loop.run_forever()

    def shutdown(self):
        try:
            self.loop.stop()
            isrunning = self.loop.is_running()
            if not isrunning:
                print("stopped server on port " + self.port)
            else:
                print("couldn't stop server on port " + self.port)
            return isrunning
        finally:
            self.loop.close()


