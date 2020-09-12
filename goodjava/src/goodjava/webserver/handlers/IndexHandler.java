package goodjava.webserver.handlers;

import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;


public final class IndexHandler implements Handler {
	private final Handler handler;
	private final String indexName;

	public IndexHandler(Handler handler) {
		this(handler,"index.html");
	}

	public IndexHandler(Handler handler,String indexName) {
		this.handler = handler;
		this.indexName = indexName;
	}

	public Response handle(Request request) {
		if( request.path.endsWith("/") ) {
			String path = request.path;
			try {
				request.path += indexName;
				return handler.handle(request);
			} finally {
				request.path = path;
			}
		} else
			return handler.handle(request);
	}
}
