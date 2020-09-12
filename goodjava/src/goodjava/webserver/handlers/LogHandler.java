package goodjava.webserver.handlers;

import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;


public final class LogHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger("HTTP");

	private final Handler handler;

	public LogHandler(Handler handler) {
		this.handler = handler;
	}

	public Response handle(Request request) {
		Response response = handler.handle(request);
		logger.info( request.method + " " + request.path + " " + response.status.code + " " + response.body.length );
		return response;
	}
}
