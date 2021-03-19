from .connection import msghandler, connectionhandler
# from .connection import Connection
import socket, asyncio, concurrent.futures
from functools import partial
from contextlib import asynccontextmanager


class Server:
    def __init__(self, handler, port=8080, host="localhost", loop=asyncio.new_event_loop()):
        self.handler = handler
        self.port = port
        self.loop = loop
        self.host = host
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM, 0)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.sock.setblocking(False)
        self.sock.bind((self.host, self.port))
        self.sock.listen(10)
        self.threxecutor = concurrent.futures.ThreadPoolExecutor()

    async def run(self):
        while True:
            connsock, addr = await self.loop.sock_accept(self.sock)
            self.loop.create_task(connectionhandler(self,connsock))
        else:
            print("server closed on port" + str(self.port))

    def start(self):
        try:
            self.loop.create_task(self.run())
            print("started server on port " + str(self.port))
            self.loop.run_forever()
            self.loop.close()
        except Exception as e:
            print("error starting server: " + str(e))

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

