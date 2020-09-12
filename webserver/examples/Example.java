package goodjava.webserver.examples;

import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.ResponseOutputStream;
import goodjava.webserver.Server;
import goodjava.webserver.handlers.MapHandler;
import goodjava.webserver.handlers.SafeHandler;
import goodjava.webserver.handlers.LogHandler;
import goodjava.webserver.handlers.FileHandler;
import goodjava.webserver.handlers.DirHandler;
import goodjava.webserver.handlers.ListHandler;
import goodjava.webserver.handlers.ContentTypeHandler;


public class Example implements Handler {

	public Response handle(Request request) {
		Response response = new Response();
		response.headers.put( "content-type", "text/plain; charset=utf-8" );
		try {
			Writer writer = new OutputStreamWriter( new ResponseOutputStream(response) );
			writer.write("Hello World\n");
			writer.close();
		} catch(IOException e) {
			throw new RuntimeException("shouldn't happen",e);
		}
		return response;
	}

	public static void simple() throws IOException {
		Handler handler = new Example();
		new Server(8080,handler).start();
	}

	public static void fancy() throws IOException {
		Map<String,Handler> map = new HashMap<String,Handler>();
		map.put( "/hello", new Example() );
		map.put( "/headers", new Headers() );
		map.put( "/params", new Params() );
		map.put( "/cookies", new Cookies() );
		Handler mapHandler = new MapHandler(map);
		FileHandler fileHandler = new FileHandler();
		Handler dirHandler = new DirHandler(fileHandler);
		Handler handler = new ListHandler( mapHandler, fileHandler, dirHandler );
		handler = new ContentTypeHandler(handler);
		handler = new SafeHandler(handler);
		handler = new LogHandler(handler);
		new Server(8080,handler).start();
	}

	public static void main(String[] args) throws Exception {
		fancy();
	}
}
