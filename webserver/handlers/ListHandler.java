package goodjava.webserver.handlers;

import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;


public final class ListHandler implements Handler {
	private final Handler[] handlers;

	public ListHandler(Handler... handlers) {
		this.handlers = handlers;
	}

	public Response handle(Request request) {
		for( Handler handler : handlers ) {
			Response response = handler.handle(request);
			if( response != null )
				return response;
		}
		return null;
	}
}
