package goodjava.webserver;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;


// plenty of room for improvement
public class ResponseOutputStream extends ByteArrayOutputStream {
	private final Response response;

	public ResponseOutputStream(Response response) {
		if(response==null) throw new NullPointerException();
		this.response = response;
	}

	@Override public void close() throws IOException {
		super.close();
		int size = size();
		response.body = new Response.Body( size, new ByteArrayInputStream(buf,0,size) );
	}
}
