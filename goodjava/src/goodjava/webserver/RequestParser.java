package goodjava.webserver;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.ArrayList;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.parser.Parser;
import goodjava.parser.ParseException;


final class RequestParser {
	private static final Logger logger = LoggerFactory.getLogger(RequestParser.class);
	private final Request request;
	private Parser parser;

	RequestParser(Request request) {
		this.request = request;
	}

	void parseUrlencoded(String charset) throws ParseException, UnsupportedEncodingException {
		if( request.body == null ) {
			logger.warn("body is null\n"+request.rawHead);
			return;
		}
		this.parser = new Parser(Util.toString(request.body,charset));
		parseQuery();
		require( parser.endOfInput() );
	}

	void parseHead() throws ParseException {
		this.parser = new Parser(request.rawHead);
		parseRequestLine();
		while( !parser.match("\r\n") ) {
			parserHeaderField();
		}
		parseCookies();
	}

	private void parseRequestLine() throws ParseException {
		parseMethod();
		require( parser.match(' ') );
		parseRawPath();
		require( parser.match(' ') );
		parseProtocol();
		require( parser.match("\r\n") );
	}

	private void parseMethod() throws ParseException {
		int start = parser.currentIndex();
		if( !methodChar() )
			throw new ParseException(parser,"no method");
		while( methodChar() );
		request.method = parser.textFrom(start);
	}

	private boolean methodChar() {
		return parser.inCharRange('A','Z');
	}

	private void parseRawPath() throws ParseException {
		int start = parser.currentIndex();
		parsePath();
		if( parser.match('?') )
			parseQuery();
		request.rawPath = parser.textFrom(start);
	}

	private void parsePath() throws ParseException {
		int start = parser.currentIndex();
		if( !parser.match('/') )
			throw new ParseException(parser,"bad path");
		while( parser.noneOf(" ?#") );
		request.path = urlDecode( parser.textFrom(start) );
		request.originalPath = request.path;
	}

	private void parseQuery() throws ParseException {
		do {
			int start = parser.currentIndex();
			while( queryChar() );
			String name = urlDecode( parser.textFrom(start) );
			String value = null;
			if( parser.match('=') ) {
				start = parser.currentIndex();
				while( queryChar() || parser.match('=') );
				value = urlDecode( parser.textFrom(start) );
			}
			if( name.length() > 0 || value != null ) {
				if( value==null )
					value = "";
				Util.add(request.parameters,name,value);
			}
		} while( parser.match('&') );
	}

	private boolean queryChar() {
		return parser.noneOf("=&# \t\n\f\r\u000b");
	}

	private void parseProtocol() throws ParseException {
		int start = parser.currentIndex();
		if( !(
			parser.match("HTTP/")
			&& parser.inCharRange('0','9')
			&& parser.match('.')
			&& parser.inCharRange('0','9')
		) )
			throw new ParseException(parser,"bad protocol");
		request.protocol = parser.textFrom(start);
		request.scheme = "http";
	}


	private void parserHeaderField() throws ParseException {
		String name = parseName();
		require( parser.match(':') );
		while( parser.anyOf(" \t") );
		String value = parseValue();
		while( parser.anyOf(" \t") );
		require( parser.match("\r\n") );
		Util.add(request.headers,name,value);
	}

	private String parseName() throws ParseException {
		int start = parser.currentIndex();
		require( tokenChar() );
		while( tokenChar() );
		return parser.textFrom(start).toLowerCase();
	}

	private String parseValue() throws ParseException {
		int start = parser.currentIndex();
		while( !testEndOfValue() )
			require( parser.anyChar() );
		return parser.textFrom(start);
	}

	private boolean testEndOfValue() {
		parser.begin();
		while( parser.anyOf(" \t") );
		boolean b = parser.endOfInput() || parser.anyOf("\r\n");
		parser.failure();  // rollback
		return b;
	}

	private void require(boolean b) throws ParseException {
		if( !b )
			throw new ParseException(parser,"failed");
	}

	boolean tokenChar() {
		if( parser.endOfInput() )
			return false;
		char c = parser.currentChar();
		if( 32 <= c && c <= 126 && "()<>@,;:\\\"/[]?={} \t\r\n".indexOf(c) == -1 ) {
			parser.anyChar();
			return true;
		} else {
			return false;
		}
	}


	private void parseCookies() throws ParseException {
		String text = (String)request.headers.get("cookie");
		if( text == null )
			return;
		this.parser = new Parser(text);
		while(true) {
			int start = parser.currentIndex();
			while( parser.noneOf("=;") );
			String name = urlDecode( parser.textFrom(start) );
			if( parser.match('=') ) {
				start = parser.currentIndex();
				while( parser.noneOf(";") );
				String value = parser.textFrom(start);
				int len = value.length();
				if( value.charAt(0)=='"' && value.charAt(len-1)=='"' )
					value = value.substring(1,len-1);
				value = urlDecode(value);
				request.cookies.put(name,value);
			}
			if( parser.endOfInput() )
				return;
			require( parser.match(';') );
			parser.match(' ');  // optional for bad browsers
		}
	}


	private static final String contentTypeStart = "multipart/form-data; boundary=";

	void parseMultipart() throws ParseException, UnsupportedEncodingException {
		if( request.body == null ) {
			logger.warn("body is null\n"+request.rawHead);
			return;
		}
		String contentType = (String)request.headers.get("content-type");
		if( !contentType.startsWith(contentTypeStart) )
			throw new RuntimeException(contentType);
		String boundary = "--"+contentType.substring(contentTypeStart.length());
		this.parser = new Parser(Util.toString(request.body,null));
//System.out.println(this.parser.text);
		require( parser.match(boundary) );
		boundary = "\r\n" + boundary;
		while( !parser.match("--\r\n") ) {
			require( parser.match("\r\n") );
			require( parser.match("Content-Disposition: form-data; name=") );
			String name = quotedString();
			String filename = null;
			boolean isBinary = false;
			if( parser.match("; filename=") ) {
				filename = quotedString();
				require( parser.match("\r\n") );
				require( parser.match("Content-Type: ") );
				int start = parser.currentIndex();
				if( parser.match("application/") ) {
					isBinary = true;
				} else if( parser.match("image/") ) {
					isBinary = true;
				} else if( parser.match("text/") ) {
					isBinary = false;
				} else
					throw new ParseException(parser,"bad file content-type");
				while( parser.inCharRange('a','z') || parser.anyOf("-.") );
				contentType = parser.textFrom(start);
			}
			require( parser.match("\r\n") );
			require( parser.match("\r\n") );
			int start = parser.currentIndex();
			while( !parser.test(boundary) ) {
				require( parser.anyChar() );
			}
			String value = parser.textFrom(start);
			if( filename == null ) {
				Util.add(request.parameters,name,value);
			} else {
				Object content = isBinary ? Util.toBytes(value) : value;
				Request.MultipartFile mf = new Request.MultipartFile(filename,contentType,content);
				Util.add(request.parameters,name,mf);
			}
			require( parser.match(boundary) );
		}
	}

	private String quotedString() throws ParseException {
		StringBuilder sb = new StringBuilder();
		require( parser.match('"') );
		while( !parser.match('"') ) {
			if( parser.match("\\\"") ) {
				sb.append('"');
			} else {
				require( parser.anyChar() );
				sb.append( parser.lastChar() );
			}
		}
		return sb.toString();
	}

	private String urlDecode(String s) throws ParseException {
		try {
			return URLDecoder.decode(s,"UTF-8");
		} catch(UnsupportedEncodingException e) {
			parser.rollback();
			throw new ParseException(parser,e);
		} catch(IllegalArgumentException e) {
			parser.rollback();
			throw new ParseException(parser,e);
		}
	}

	// improve later
	void parseJson(String charset) throws UnsupportedEncodingException {
		if( request.body == null ) {
			logger.warn("body is null\n"+request.rawHead);
			return;
		}
		String value = Util.toString(request.body,charset);
		Util.add(request.parameters,"json",value);
	}

}
