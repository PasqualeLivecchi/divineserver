package goodjava.webserver.handlers;

import java.util.Map;
import java.util.HashMap;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;


public class ContentTypeHandler implements Handler {
	private final Handler handler;

	// maps extension to content-type
	// key must be lower case
	public final Map<String,String> map = new HashMap<String,String>();

	// set to null for none
	public String contentTypeForNoExtension;

	public ContentTypeHandler(Handler handler) {
		this(handler,"utf-8");
	}

	public ContentTypeHandler(Handler handler,String charset) {
		this.handler = handler;
		String attrs = charset== null ? "" : "; charset="+charset;
		String htmlType = "text/html" + attrs;
		String textType = "text/plain" + attrs;
		contentTypeForNoExtension = htmlType;
		map.put( "html", htmlType );
		map.put( "txt", textType );
		map.put( "luan", textType );
		map.put( "css", "text/css" );
		map.put( "js", "application/javascript" );
		map.put( "json", "application/json" + attrs );
		map.put( "mp4", "video/mp4" );
		// add more as need
	}

	public Response handle(Request request) {
		Response response = handler.handle(request);
		if( response!=null && !response.headers.containsKey("content-type") ) {
			String path = request.path;
			int iSlash = path.lastIndexOf('/');
			int iDot = path.lastIndexOf('.');
			String type;
			if( iDot < iSlash ) {  // no extension
				type = contentTypeForNoExtension;
			} else {  // extension
				String extension = path.substring(iDot+1);
				type = map.get( extension.toLowerCase() );
			}
			if( type != null )
				response.headers.put("content-type",type);
		}
		return response;
	}
}
