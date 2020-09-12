package goodjava.webserver;

import java.util.Map;
import java.util.HashMap;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public class Status {
	private static final Logger logger = LoggerFactory.getLogger(Status.class);

	public final int code;
	public final String reason;

	public Status(int code,String reason) {
		this.code = code;
		this.reason = reason;
	}

	private static final Map<Integer,Status> map = new HashMap<Integer,Status>();

	protected static Status newStatus(int code,String reason) {
		Status status = new Status(code,reason);
		map.put(code,status);
		return status;
	}

	public static Status getStatus(int code) {
		Status status = map.get(code);
		if( status == null ) {
			logger.warn("missing status "+code);
			status = new Status(code,"");
		}
		return status;
	}

	public static final Status OK = newStatus(200,"OK");
	public static final Status MOVED_PERMANENTLY = newStatus(301,"Moved Permanently");
	public static final Status FOUND = newStatus(302,"Found");
	public static final Status BAD_REQUEST = newStatus(400,"Bad Request");
	public static final Status NOT_FOUND = newStatus(404,"Not Found");
	public static final Status INTERNAL_SERVER_ERROR = newStatus(500,"Internal Server Error");
}
