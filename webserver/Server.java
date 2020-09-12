package goodjava.webserver;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public class Server {
	private static final Logger logger = LoggerFactory.getLogger(Server.class);

	public final int port;
	public final Handler handler;
	public static final ExecutorService threadPool = Executors.newCachedThreadPool();

	public Server(int port,Handler handler) {
		this.port = port;
		this.handler = handler;
	}

	protected ServerSocket newServerSocket() throws IOException {
		return new ServerSocket(port);
	}

	public synchronized void start() throws IOException {
		final ServerSocket ss = newServerSocket();
		threadPool.execute(new Runnable(){public void run() {
			try {
				while(!threadPool.isShutdown()) {
					final Socket socket = ss.accept();
					threadPool.execute(new Runnable(){public void run() {
						Connection.handle(Server.this,socket);
					}});
				}
			} catch(IOException e) {
				logger.error("",e);
			}
		}});
		logger.info("started server on port "+port);
	}

	public synchronized boolean stop(long timeoutSeconds) {
		try {
			threadPool.shutdownNow();
			boolean stopped = threadPool.awaitTermination(timeoutSeconds,TimeUnit.SECONDS);
			if(stopped)
				logger.info("stopped server on port "+port);
			else
				logger.warn("couldn't stop server on port "+port);
			return stopped;
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static class ForAddress extends Server {
		private final InetAddress addr;

		public ForAddress(InetAddress addr,int port,Handler handler) {
			super(port,handler);
			this.addr = addr;
		}

		public ForAddress(String addrName,int port,Handler handler) throws UnknownHostException {
			this(InetAddress.getByName(addrName),port,handler);
		}

		protected ServerSocket newServerSocket() throws IOException {
			return new ServerSocket(port,0,addr);
		}
	}
}
