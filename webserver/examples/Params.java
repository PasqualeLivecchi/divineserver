package goodjava.webserver.examples;

import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.Map;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.ResponseOutputStream;


public final class Params implements Handler {

	public Response handle(Request request) {
		Response response = new Response();
		response.headers.put( "content-type", "text/plain; charset=utf-8" );
		try {
			Writer writer = new OutputStreamWriter( new ResponseOutputStream(response) );
			for( Map.Entry<String,Object> entry : request.parameters.entrySet() ) {
				writer.write(entry.getKey()+" = "+entry.getValue()+"\n");
			}
			writer.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		return response;
	}

}
