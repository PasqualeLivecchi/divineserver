package luan.modules.url;

import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import luan.LuanTable;
import luan.LuanException;
import goodjava.webserver.Request;


public final class MultipartClient {
	private static final byte[] __CRLF = {'\r','\n'};
	private static final byte[] __DASHDASH = {'-','-'};
	private static final String __ISO_8859_1 = "ISO-8859-1";

	private final Map params = new HashMap();

	private static Object get(Map<Object,Object> params,String key) throws LuanException {
		Object val = params.remove(key);
		if( val==null)
			throw new LuanException( "parameter '"+key+"' is required in multipart file" );
		return val;
	}

	private static String getString(Map<Object,Object> params,String key) throws LuanException {
		Object val = get(params,key);
		if( !(val instanceof String) )
			throw new LuanException( "parameter '"+key+"' must be a string" );
		return (String)val;
	}

	MultipartClient(Map params) throws LuanException {
		for( Object hack : params.entrySet() ) {
			Map.Entry entry = (Map.Entry)hack;
			String key = (String)entry.getKey();
			Object val = entry.getValue();
			List list = new ArrayList();
			if( val instanceof String ) {
				list.add(val);
			} else if(val instanceof LuanTable) {
				LuanTable t = (LuanTable)val;
				if( t.isList() ) {
					for( Object obj : t.asList() ) {
						if( obj instanceof String ) {
							list.add(obj);
						} else
							throw new LuanException( "parameter '"+key+"' values must be strings or tables" );
					}
				} else {
					Map<Object,Object> map = t.asMap();
					String filename = getString(map,"filename");
					String contentType = getString(map,"content_type");
					Object content = get(map,"content");
					if( !(content instanceof String || content instanceof byte[]) )
						throw new LuanException( "content must be a string or binary" );
					list.add( new Request.MultipartFile(filename,contentType,content) );
				}
			} else {
				throw new LuanException( "parameter '"+key+"' must be string or table" );
			}
			this.params.put(key,list);
		}
	}

	public OutputStream write(HttpURLConnection httpCon) throws IOException {
		String boundary = "luan" + System.identityHashCode(this) + Long.toString(System.currentTimeMillis(),36);
		byte[] boundaryBytes = boundary.getBytes(__ISO_8859_1);

		httpCon.setRequestProperty("content-type","multipart/form-data; boundary="+boundary);
		OutputStream out = httpCon.getOutputStream();
		for( Object hack : params.entrySet() ) {
			Map.Entry entry = (Map.Entry)hack;
			String name = (String)entry.getKey();
			List list = (List)entry.getValue();
			for( Object obj : list ) {
		        out.write(__DASHDASH);
		        out.write(boundaryBytes);
		        out.write(__CRLF);
	            out.write(("Content-Disposition: form-data; name=\""+name+"\"").getBytes(__ISO_8859_1));
				if( obj instanceof String ) {
					String val = (String)obj;
		            out.write(__CRLF);
			        out.write(__CRLF);
					out.write(val.getBytes());
				} else {
					Request.MultipartFile mpf = (Request.MultipartFile)obj;
		            out.write(("; filename=\""+mpf.filename+"\"").getBytes(__ISO_8859_1));
		            out.write(__CRLF);
		            out.write(("Content-Type: "+mpf.contentType).getBytes(__ISO_8859_1));
		            out.write(__CRLF);
			        out.write(__CRLF);
					byte[] content = mpf.content instanceof String ? ((String)mpf.content).getBytes() : (byte[])mpf.content;
					out.write(content);
				}
		        out.write(__CRLF);
			}
		}
		out.write(__DASHDASH);
		out.write(boundaryBytes);
		out.write(__DASHDASH);
		out.write(__CRLF);
		return out;
	}
}
