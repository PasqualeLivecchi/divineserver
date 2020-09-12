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


public final class Cookies implements Handler {

	public Response handle(Request request) {
		Response response = new Response();
		String name = (String)request.parameters.get("name");
		if( name != null ) {
			Map<String,String> attributes = new HashMap<String,String>();
			String value = (String)request.parameters.get("value");
			if( value != null ) {
				response.setCookie(name,value,attributes);
			} else {
				attributes.put("Max-Age","0");
				response.setCookie(name,"delete",attributes);
			}
		}
		response.headers.put( "content-type", "text/plain; charset=utf-8" );
		try {
			Writer writer = new OutputStreamWriter( new ResponseOutputStream(response) );
			for( Map.Entry<String,String> entry : request.cookies.entrySet() ) {
				writer.write(entry.getKey()+" = "+entry.getValue()+"\n");
			}
			writer.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		return response;
	}

}
