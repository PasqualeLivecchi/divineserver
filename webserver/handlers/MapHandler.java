package goodjava.webserver.handlers;

import java.util.Map;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;


public final class MapHandler implements Handler {
	private final Map<String,Handler> map;

	public MapHandler(Map<String,Handler> map) {
		this.map = map;
	}

	public Response handle(Request request) {
		Handler handler = map.get(request.path);
		return handler==null ? null : handler.handle(request);
	}
}
