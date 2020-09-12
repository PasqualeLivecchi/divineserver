package luan.modules.url;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Base64;
import goodjava.io.IoUtils;
import goodjava.parser.ParseException;
import luan.Luan;
import luan.LuanTable;
import luan.LuanJavaFunction;
import luan.LuanException;
import luan.modules.IoLuan;
import luan.modules.Utils;


public final class LuanUrl extends IoLuan.LuanIn {

	private static enum Method { GET, POST, DELETE }

	public final URL url;
	private Method method = Method.GET;
	private final Map<String,Object> headers = new HashMap<String,Object>();
	private static final byte[] NO_CONTENT = new byte[0];
	private byte[] content = NO_CONTENT;
	private MultipartClient multipart = null;
	private int timeout = 60000;
	private String authUsername = null;
	private String authPassword = null;
	public HttpURLConnection httpCon;

	public LuanUrl(URL url,LuanTable options) throws LuanException {
		if( options != null ) {
			options = new LuanTable(options);
			String methodStr = Utils.removeString(options,"method");
			if( methodStr != null ) {
				methodStr = methodStr.toUpperCase();
				try {
					this.method = Method.valueOf(methodStr);
				} catch(IllegalArgumentException e) {
					throw new LuanException( "invalid method: "+methodStr );
				}
			}
			Map headerMap = removeMap(options,"headers");
			if( headerMap != null ) {
				for( Object hack : headerMap.entrySet() ) {
					Map.Entry entry = (Map.Entry)hack;
					String name = (String)entry.getKey();
					Object val = entry.getValue();
					if( val instanceof String ) {
						headers.put(name,val);
					} else {
						if( !(val instanceof LuanTable) )
							throw new LuanException( "header '"+name+"' must be string or table" );
						LuanTable t = (LuanTable)val;
						if( !t.isList() )
							throw new LuanException( "header '"+name+"' table must be list" );
						headers.put(name,t.asList());
					}
				}
			}
			LuanTable auth = Utils.removeTable(options,"authorization");
			if( auth != null ) {
				auth = new LuanTable(auth);
				if( headers!=null && headers.containsKey("authorization") )
					throw new LuanException( "can't define authorization with header 'authorization' defined" );
				String username = Utils.removeString(auth,"username");
				if( username==null )  username = "";
				String password = Utils.removeString(auth,"password");
				if( password==null )  password = "";
				String type = Utils.removeString(auth,"type");
				if( !auth.isEmpty() )
					throw new LuanException( "unrecognized authorization options: "+auth );
				if( type != null ) {
					if( !type.toLowerCase().equals("basic") )
						throw new LuanException( "authorization type can only be 'basic' or nil" );
					String val = basicAuth(username,password);
					headers.put("authorization",val);
				} else {
					authUsername = username;
					authPassword = password;
				}
			}
			Map params = removeMap(options,"parameters");
			String enctype = Utils.removeString(options,"enctype");
			Object content = options.remove("content");
			if( content != null ) {
				if( this.method != Method.POST )
					throw new LuanException( "content can only be used with POST" );
				if( params != null )
					throw new LuanException( "content cannot be used with parameters" );
				if( content instanceof String ) {
					this.content = ((String)content).getBytes();
				} else if( content instanceof byte[] ) {
					this.content = (byte[])content;
				} else
					throw new LuanException( "content must be String or byte[]" );
			}
			if( enctype != null ) {
				if( !enctype.equals("multipart/form-data") )
					throw new LuanException( "unrecognized enctype: "+enctype );
				if( this.method != Method.POST )
					throw new LuanException( "multipart/form-data can only be used with POST" );
				if( params==null )
					throw new LuanException( "multipart/form-data requires parameters" );
				if( params.isEmpty() )
					throw new LuanException( "multipart/form-data parameters can't be empty" );
				multipart = new MultipartClient(params);
			}
			else if( params != null ) {
				StringBuilder sb = new StringBuilder();
				for( Object hack : params.entrySet() ) {
					Map.Entry entry = (Map.Entry)hack;
					String key = (String)entry.getKey();
					Object val = entry.getValue();
					String keyEnc = encode(key);
					if( val instanceof String ) {
						and(sb);
						sb.append( keyEnc ).append( '=' ).append( encode((String)val) );
					} else {
						if( !(val instanceof LuanTable) )
							throw new LuanException( "parameter '"+key+"' must be string or table" );
						LuanTable t = (LuanTable)val;
						if( !t.isList() )
							throw new LuanException( "parameter '"+key+"' table must be list" );
						for( Object obj : t.asList() ) {
							if( !(obj instanceof String) )
								throw new LuanException( "parameter '"+key+"' values must be strings" );
							and(sb);
							sb.append( keyEnc ).append( '=' ).append( encode((String)obj) );
						}
					}
				}
				if( this.method==Method.POST ) {
					this.content = sb.toString().getBytes();
				} else {
					String urlS = url.toString();
					if( urlS.indexOf('?') == -1 ) {
						urlS += '?';
					} else {
						urlS += '&';
					}
					urlS += sb;
					try {
						url = new URL(urlS);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
			Integer timeout = Utils.removeInteger(options,"time_out");
			if( timeout != null )
				this.timeout = timeout;
			Utils.checkEmpty(options);
		}
		this.url = url;
	}

	private static void and(StringBuilder sb) {
		if( sb.length() > 0 )
			sb.append('&');
	}

	private static String encode(String s) {
		try {
			return URLEncoder.encode(s,"UTF-8");
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static Map removeMap(LuanTable options,String key) throws LuanException {
		LuanTable t = Utils.removeTable(options,key);
		return t==null ? null : t.asMap();
	}

	@Override public InputStream inputStream() throws IOException, LuanException {
		try {
			return inputStream(null);
		} catch(SocketTimeoutException e) {
			String msg = e.getMessage();
			if( msg != null ) {
				throw new LuanException(e.getMessage());
			} else {
				throw e;
			}
		} catch(AuthException e) {
			try {
				return inputStream(e.authorization);
			} catch(AuthException e2) {
				throw new RuntimeException(e2);  // never
			}
		}
	}

	private InputStream inputStream(String authorization)
		throws IOException, LuanException, AuthException
	{
		URLConnection con = url.openConnection();
		if( timeout != 0 ) {
			con.setConnectTimeout(timeout);
			con.setReadTimeout(timeout);
		}
		for( Map.Entry<String,Object> entry : headers.entrySet() ) {
			String key = entry.getKey();
			Object val = entry.getValue();
			if( val instanceof String ) {
				con.addRequestProperty(key,(String)val);
			} else {
				List list = (List)val;
				for( Object obj : list ) {
					con.addRequestProperty(key,(String)obj);
				}
			}
		}
		if( authorization != null )
			con.addRequestProperty("Authorization",authorization);
		if( !(con instanceof HttpURLConnection) ) {
			if( method!=Method.GET )
				throw new LuanException("method must be GET but is "+method);
			return con.getInputStream();
		}

		httpCon = (HttpURLConnection)con;

		if( method==Method.GET ) {
			return getInputStream(httpCon,authorization);
		}

		if( method==Method.DELETE ) {
			httpCon.setRequestMethod("DELETE");
			return getInputStream(httpCon,authorization);
		}

		// POST

//		httpCon.setRequestProperty("content-type","application/x-www-form-urlencoded");
		httpCon.setDoOutput(true);
		httpCon.setRequestMethod("POST");

		OutputStream out;
		if( multipart != null ) {
			out = multipart.write(httpCon);
		} else {
//			httpCon.setRequestProperty("Content-Length",Integer.toString(content.length));
			out = httpCon.getOutputStream();
			out.write(content);
		}
		out.flush();
		try {
			return getInputStream(httpCon,authorization);
		} finally {
			out.close();
		}
	}

	private InputStream getInputStream(HttpURLConnection httpCon,String authorization)
		throws IOException, LuanException, AuthException
	{
		try {
			return httpCon.getInputStream();
//		} catch(FileNotFoundException e) {
//			throw e;
		} catch(IOException e) {
			int responseCode = httpCon.getResponseCode();
			if( responseCode == 401 && authUsername != null && authorization==null ) {
				String authStr = httpCon.getHeaderField("www-authenticate");
				//System.out.println("auth = "+authStr);
				try {
					WwwAuthenticate auth = new WwwAuthenticate(authStr);
					if( auth.type.equals("Basic") ) {
						String val = basicAuth(authUsername,authPassword);
						throw new AuthException(val);
					} else if( auth.type.equals("Digest") ) {
						String realm = auth.options.get("realm");
						if(realm==null) throw new RuntimeException("missing realm");
						String algorithm = auth.options.get("algorithm");
						if( algorithm!=null && !algorithm.equals("MD5") )
							throw new LuanException("unsupported digest algorithm: "+algorithm);
						String qop = auth.options.get("qop");
						if( qop!=null && !qop.equals("auth") )
							throw new LuanException("unsupported digest qop: "+qop);
						String nonce = auth.options.get("nonce");
						if(nonce==null) throw new RuntimeException("missing nonce");
						String uri = fullPath(url);
						String a1 = authUsername + ':' + realm + ':' + authPassword;
						String a2 = "" + method + ':' + uri;
						String nc = "00000001";
						String cnonce = "7761faf2daa45b3b";  // who cares?
						String response = md5(a1) + ':' + nonce;
						if( qop != null ) {
							response += ':' + nc + ':' + cnonce + ':' + qop;
						}
						response += ':' + md5(a2);
						response = md5(response);
						String val = "Digest";
						val += " username=\"" + authUsername + "\"";
						val += ", realm=\"" + realm + "\"";
						val += ", uri=\"" + uri + "\"";
						val += ", nonce=\"" + nonce + "\"";
						val += ", response=\"" + response + "\"";
						if( qop != null ) {
							val += ", qop=" + qop;
							val += ", nc=" + nc;
							val += ", cnonce=\"" + cnonce + "\"";
						}
						//System.out.println("val = "+val);
						throw new AuthException(val);
					} else
						throw new RuntimeException(auth.type);
				} catch(ParseException pe) {
					throw new LuanException(pe);
				}
			}
			String msg = "" + responseCode;
			String responseMessage = httpCon.getResponseMessage();
			if( responseMessage != null )
				msg += " - " + responseMessage;
			String responseContent = null;
			InputStream is = httpCon.getErrorStream();
			if( is != null ) {
				Reader in = new InputStreamReader(is);
				responseContent = IoUtils.readAll(in);
				in.close();
				msg += "\n" + responseContent;
			}
			LuanException le = new LuanException(msg,e);
			le.put("response_code",responseCode);
			le.put("response_message",responseMessage);
			le.put("response_content",responseContent);
			throw le;
		}
	}

	@Override public String to_string() {
		return url.toString();
	}

	@Override public String to_uri_string() {
		return url.toString();
	}

	private static String basicAuth(String username,String password) {
		String s = username + ':' + password;
		return "Basic " + Base64.getEncoder().encodeToString(s.getBytes());
	}

	private final class AuthException extends Exception {
		final String authorization;

		AuthException(String authorization) {
			this.authorization = authorization;
		}
	}

	// retarded java api lacks this
	public static String fullPath(URL url) {
		String path = url.getPath();
		String query = url.getQuery();
		if( query != null )
			path += "?" + query;
		return path;
	}

	// retarded java api lacks this
	public static String md5(String s) {
		try {
			byte[] md5 = MessageDigest.getInstance("MD5").digest(s.getBytes());
			StringBuffer sb = new StringBuffer();
			for( byte b : md5 ) {
				sb.append( String.format("%02x",b) );
			}
			return sb.toString();
		} catch(NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
