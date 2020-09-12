package goodjava.webserver;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;


public class Request {
	public volatile String rawHead;
	public volatile String method;
	public volatile String rawPath;
	public volatile String originalPath;
	public volatile String path;
	public volatile String protocol;  // only HTTP/1.1 is accepted
	public volatile String scheme;
	public final Map<String,Object> headers = Collections.synchronizedMap(new LinkedHashMap<String,Object>());
	public final Map<String,Object> parameters = Collections.synchronizedMap(new LinkedHashMap<String,Object>());
	public final Map<String,String> cookies = Collections.synchronizedMap(new LinkedHashMap<String,String>());
	public volatile byte[] body;

	public static final class MultipartFile {
		public final String filename;
		public final String contentType;
		public final Object content;  // byte[] or String

		public MultipartFile(String filename,String contentType,Object content) {
			this.filename = filename;
			this.contentType = contentType;
			this.content = content;
		}

		public String toString() {
			return "{filename="+filename+", content="+content+"}";
		}
	}
}
