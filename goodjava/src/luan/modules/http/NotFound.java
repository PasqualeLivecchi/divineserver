package luan.modules.http;

import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.Handler;


public class NotFound implements Handler {
	private final Handler handler;

	public NotFound(Handler handler) {
		this.handler = handler;
	}

	@Override public Response handle(Request request) {
		request.headers.put(LuanHandler.NOT_FOUND,"whatever");
		try {
			return handler.handle(request);
		} finally {
			request.headers.remove(LuanHandler.NOT_FOUND);
		}
	}

}
