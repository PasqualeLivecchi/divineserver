package goodjava.webserver.handlers;

import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.ResponseOutputStream;
import goodjava.webserver.Status;


public final class SafeHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(SafeHandler.class);

	private final Handler handler;

	public SafeHandler(Handler handler) {
		this.handler = handler;
	}

	public Response handle(Request request) {
		try {
			Response response = handler.handle(request);
			if( response != null )
				return response;
		} catch(RuntimeException e) {
			logger.error("",e);
			Response response = new Response();
			response.status = Status.INTERNAL_SERVER_ERROR;
			response.headers.put( "content-type", "text/plain; charset=utf-8" );
			PrintWriter writer = new PrintWriter( new ResponseOutputStream(response) );
			writer.write( "Internel Server Error\n\n" );
			e.printStackTrace(writer);
			writer.close();
			return response;
		}
		return Response.errorResponse( Status.NOT_FOUND, request.path+" not found\n" );
	}

}
