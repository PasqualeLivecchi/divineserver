from .connection import msghandler, connectionhandler
import socket, asyncio, sys, traceback #, concurrent.futures
from functools import partial
from contextlib import asynccontextmanager
# from .connection import Connection
# from os import cpu_count


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
        # self.threxecutor = concurrent.futures.ThreadPoolExecutor(max_workers=cpu_count()*3)
        # self.processexecutor = concurrent.futures.ProcessPoolExecutor()

    async def run(self):
        # try:
        while True:
            connsock, addr = await self.loop.sock_accept(self.sock)
            self.loop.create_task(connectionhandler(self,connsock))
            print(f"running: {self.loop.is_running()}")
        else:
            print(f"server closed on port {self.port}")
        # except Exception:
        #     error = sys.exc_info()[0](traceback.format_exc())
        #     print(f"error starting server: {error}")


    def start(self):
        try:
            self.loop.create_task(self.run())
            print(f"started server on port {self.port}")
            self.loop.run_forever()
            self.loop.close()
        except Exception as e:
            print(f"error starting server: {e}")

    def shutdown(self):
        try:
            self.loop.stop()
            # self.loop.close()
            if self.loop.is_closed() or not self.loop.is_running():
                print(f"stopped server on port {self.port}")
            else:
                print(f"couldn't stop server on port {self.port}")
        except Exception:
            error = sys.exc_info()[0](traceback.format_exc())
            print(f"error shutting down server: {error}")
        finally:
            self.loop.close()

