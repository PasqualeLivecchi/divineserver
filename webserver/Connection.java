package goodjava.webserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.parser.ParseException;
import goodjava.io.IoUtils;


final class Connection {
	private static final Logger logger = LoggerFactory.getLogger(Connection.class);

	static void handle(Server server,Socket socket) {
		new Connection(server,socket).handle();
	}

	private final Server server;
	private final Socket socket;

	private Connection(Server server,Socket socket) {
		this.server = server;
		this.socket = socket;
	}

	private void handle() {
		try {
			Request request = new Request();
			Response response;
			String contentType = null;
			try {
				{
					InputStream in = socket.getInputStream();
					byte[] a = new byte[8192];
					int endOfHeader;
					int size = 0;
					int left = a.length;
					outer: while(true) {
						int n = in.read(a,size,left);
						if( n == -1 ) {
							if( size == 0 ) {
								socket.close();
								return;
							}
							throw new IOException("unexpected end of input at "+size);
						}
						size += n;
						for( int i=0; i<=size-4; i++ ) {
							if( a[i]=='\r' && a[i+1]=='\n' && a[i+2]=='\r' && a[i+3]=='\n' ) {
								endOfHeader = i + 4;
								break outer;
							}
						}
						left -= n;
						if( left == 0 ) {
							byte[] a2 = new byte[2*a.length];
							System.arraycopy(a,0,a2,0,size);
							a = a2;
							left = a.length - size;
						}
					}
					String rawHead = new String(a,0,endOfHeader);
					//System.out.println(rawHead);
					request.rawHead = rawHead;
					RequestParser parser = new RequestParser(request);
					parser.parseHead();
		
					String lenStr = (String)request.headers.get("content-length");
					if( lenStr != null ) {
						int len = Integer.parseInt(lenStr);
						byte[] body = new byte[len];
						size -= endOfHeader;
						System.arraycopy(a,endOfHeader,body,0,size);
						while( size < len ) {
							int n = in.read(body,size,len-size);
							if( n == -1 ) {
								throw new IOException("unexpected end of input at "+size);
							}
							size += n;
						}
						request.body = body;
						//System.out.println(new String(request.body));
					}
	
					contentType = (String)request.headers.get("content-type");
					if( contentType != null ) {
						contentType = contentType.toLowerCase();
						if( contentType.equals("application/x-www-form-urlencoded") ) {
							parser.parseUrlencoded(null);
						} else if( contentType.equals("application/x-www-form-urlencoded; charset=utf-8") ) {
							parser.parseUrlencoded("utf-8");
						} else if( contentType.startsWith("multipart/form-data;") ) {
							parser.parseMultipart();
						} else if( contentType.equals("application/json") ) {
							parser.parseJson(null);
						} else if( contentType.equals("application/json; charset=utf-8") ) {
							parser.parseJson("utf-8");
						} else {
							logger.info("unknown request content-type: "+contentType);
						}
					}

					String scheme = (String)request.headers.get("x-forwarded-proto");
					if( scheme != null )
						request.scheme = scheme;
				}
				response = server.handler.handle(request);
			} catch(ParseException e) {
				logger.warn("parse error\n"+request.rawHead.trim()+"\n",e);
				String msg = e.toString();
				if( contentType != null )
					msg = "invalid content for content-type " + contentType + "\n" + msg;
				response = Response.errorResponse(Status.BAD_REQUEST,msg);
			}
			response.headers.put("connection","close");
			response.headers.put("content-length",Long.toString(response.body.length));
			byte[] header = response.toHeaderString().getBytes();
	
			OutputStream out = socket.getOutputStream();
			out.write(header);
			IoUtils.copyAll(response.body.content,out);
			out.close();
			socket.close();
		} catch(IOException e) {
			logger.info("",e);
		}
	}

}
