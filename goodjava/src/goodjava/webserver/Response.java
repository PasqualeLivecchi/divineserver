package goodjava.webserver;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;


public class Response {
	public final String protocol = "HTTP/1.1";
	public volatile Status status = Status.OK;
	public final Map<String,Object> headers = Collections.synchronizedMap(new LinkedHashMap<String,Object>());
	{
		headers.put("server","Goodjava");
	}
	private static final Body empty = new Body(0,new InputStream(){
		public int read() { return -1; }
	});
	public volatile Body body = empty;

	public static class Body {
		public final long length;
		public final InputStream content;
	
		public Body(long length,InputStream content) {
			this.length = length;
			this.content = content;
		}
	}


	public void addHeader(String name,String value) {
		Util.add(headers,name,value);
	}

	public void setCookie(String name,String value,Map<String,String> attributes) {
		StringBuilder buf = new StringBuilder();
		buf.append( Util.urlEncode(name) );
		buf.append( '=' );
		buf.append( Util.urlEncode(value) );
		for( Map.Entry<String,String> entry : attributes.entrySet() ) {
			buf.append( "; " );
			buf.append( entry.getKey() );
			buf.append( '=' );
			buf.append( entry.getValue() );
		}
		addHeader( "Set-Cookie", buf.toString() );
	}


	public String toHeaderString() {
		StringBuilder sb = new StringBuilder();
		sb.append( protocol )
			.append( ' ' ).append( status.code )
			.append( ' ' ).append( status.reason )
			.append( "\r\n" )
		;
		for( Map.Entry<String,Object> entry : headers.entrySet() ) {
			String name = entry.getKey();
			Object value = entry.getValue();
			if( value instanceof List ) {
				for( Object v : (List)value ) {
					sb.append( name ).append( ": " ).append( v ).append( "\r\n" );
				}
			} else {
				sb.append( name ).append( ": " ).append( value ).append( "\r\n" );
			}
		}
		sb.append( "\r\n" );
		return sb.toString();
	}


	public static Response errorResponse(Status status,String text) {
		Response response = new Response();
		response.status = status;
		response.headers.put( "content-type", "text/plain; charset=utf-8" );
		PrintWriter writer = new PrintWriter( new ResponseOutputStream(response) );
		writer.write( text );
		writer.close();
		return response;
	}
}
