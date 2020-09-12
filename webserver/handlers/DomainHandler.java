package goodjava.webserver.handlers;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import goodjava.util.SoftCacheMap;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;


public final class DomainHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(DomainHandler.class);

	public interface Factory {
		public Handler newHandler(String domain);
	}

	private static class MyTask extends TimerTask {
		private final Set<Handler> dontGc;

		MyTask(Set<Handler> dontGc) {
			this.dontGc = dontGc;
		}

		public void run() {
			dontGc.clear();
			logger.info("dontGc.clear()");
		}
	}

	private static final long HOUR = 1000L*60*60;
	private final Map<String,Handler> map = new SoftCacheMap<String,Handler>();
	private final Set<Handler> dontGc = ConcurrentHashMap.newKeySet();
	private final Timer timer = new Timer();

	private final Factory factory;

	public DomainHandler(Factory factory) {
		this.factory = factory;
		timer.schedule(new MyTask(dontGc),HOUR,HOUR);
	}

	protected void finalize() throws Throwable {
		timer.cancel();
	}

	public Response handle(Request request) {
		String host = (String)request.headers.get("host");
		if( host == null )
			return null;
		int i = host.indexOf(':');
		String domain = i == -1 ? host : host.substring(0,i);
		Handler handler = getHandler(domain);
		if( handler==null )
			return null;
		dontGc.add(handler);
		return handler.handle(request);
	}

	public Handler getHandler(String domain) {
		domain = domain.toLowerCase();
		synchronized(map) {
			Handler handler = map.get(domain);
			if( handler == null ) {
				//if(ref!=null) logger.info("gc "+domain);
				handler = factory.newHandler(domain);
				if( handler == null )
					return null;
				map.put(domain,handler);
			}
			return handler;
		}
	}

	public void removeHandler(String domain) {
		//logger.info("removeHandler "+domain);
		domain = domain.toLowerCase();
		synchronized(map) {
			Handler handler = map.remove(domain);
			if( handler != null ) {
				close(handler);
			}
		}
	}

	private static void close(Handler handler) {
		if( handler instanceof Closeable ) {
			try {
				((Closeable)handler).close();
			} catch(IOException e) {
				logger.error(handler.toString(),e);
			}
		}
	}

}
