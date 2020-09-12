package goodjava.json;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import goodjava.parser.Parser;
import goodjava.parser.ParseException;


public final class JsonParser {

	public static Object parse(String text) throws ParseException {
		return new JsonParser(text).parse();
	}

	private final Parser parser;

	private JsonParser(String text) {
		this.parser = new Parser(text);
	}

	private ParseException exception(String msg) {
		return new ParseException(parser,msg);
	}

	private Object parse() throws ParseException {
		spaces();
		Object value = value();
		spaces();
		if( !parser.endOfInput() )
			throw exception("unexpected text");
		return value;
	}

	private Object value() throws ParseException {
		if( parser.match("null") )
			return null;
		if( parser.match("true") )
			return Boolean.TRUE;
		if( parser.match("false") )
			return Boolean.FALSE;
		String s = string();
		if( s != null )
			return s;
		Number n = number();
		if( n != null )
			return n;
		List a = array();
		if( a != null )
			return a;
		Map o = object();
		if( o != null )
			return o;
		throw exception("invalid value");
	}

	private String string() throws ParseException {
		parser.begin();
		if( !parser.match('"') )
			return parser.failure(null);
		StringBuilder sb = new StringBuilder();
		while( parser.anyChar() ) {
			char c = parser.lastChar();
			switch(c) {
			case '"':
				return parser.success(sb.toString());
			case '\\':
				if( parser.anyChar() ) {
					c = parser.lastChar();
					switch(c) {
					case '"':
					case '\'':  // not in spec
					case '\\':
					case '/':
						sb.append(c);
						continue;
					case 'b':
						sb.append('\b');
						continue;
					case 'f':
						sb.append('\f');
						continue;
					case 'n':
						sb.append('\n');
						continue;
					case 'r':
						sb.append('\r');
						continue;
					case 't':
						sb.append('\t');
						continue;
					case 'u':
						int n = 0;
						for( int i=0; i<4; i++ ) {
							int d;
							if( parser.inCharRange('0','9') ) {
								d = parser.lastChar() - '0';
							} else if( parser.inCharRange('a','f') ) {
								d = parser.lastChar() - 'a' + 10;
							} else if( parser.inCharRange('A','F') ) {
								d = parser.lastChar() - 'A' + 10;
							} else {
								throw exception("invalid hex digit");
							}
							n = 16*n + d;
						}
						sb.append((char)n);
						continue;
					}
				}
				throw exception("invalid escape char");
			default:
				sb.append(c);
			}
		}
		parser.failure();
		throw exception("unclosed string");
	}

	private Number number() {
		int start = parser.begin();
		boolean isFloat = false;
		parser.match('-');
		if( !parser.match('0') ) {
			if( !parser.inCharRange('1','9') )
				return parser.failure(null);
			while( parser.inCharRange('0','9') );
		}
		if( parser.match('.') ) {
			if( !parser.inCharRange('0','9') )
				return parser.failure(null);
			while( parser.inCharRange('0','9') );
			isFloat = true;
		}
		if( parser.anyOf("eE") ) {
			parser.anyOf("+-");
			if( !parser.inCharRange('0','9') )
				return parser.failure(null);
			while( parser.inCharRange('0','9') );
			isFloat = true;
		}
		String s = parser.textFrom(start);
		Number n;
		if(isFloat)
			n = Double.valueOf(s);
		else
			n = Long.valueOf(s);
		return parser.success(n);
	}

	private List array() throws ParseException {
		parser.begin();
		if( !parser.match('[') )
			return parser.failure(null);
		spaces();
		if( parser.match(']') )
			return parser.success(Collections.emptyList());
		List list = new ArrayList();
		list.add( value() );
		spaces();
		while( parser.match(',') ) {
			spaces();
			list.add( value() );
			spaces();
		}
		if( parser.match(']') )
			return parser.success(list);
		if( parser.endOfInput() ) {
			parser.failure();
			throw exception("unclosed array");
		}
		throw exception("unexpected text in array");
	}

	private Map object() throws ParseException {
		parser.begin();
		if( !parser.match('{') )
			return parser.failure(null);
		spaces();
		if( parser.match('}') )
			return parser.success(Collections.emptyMap());
		Map map = new LinkedHashMap();
		addEntry(map);
		while( parser.match(',') ) {
			spaces();
			addEntry(map);
		}
		if( parser.match('}') )
			return parser.success(map);
		if( parser.endOfInput() ) {
			parser.failure();
			throw exception("unclosed object");
		}
		throw exception("unexpected text in object");
	}

	private void addEntry(Map map) throws ParseException {
		String key = string();
		if( key==null )
			throw exception("invalid object key");
		spaces();
		if( !parser.match(':') )
			throw exception("':' expected");
		spaces();
		Object value = value();
		spaces();
		map.put(key,value);
	}

	private void spaces() {
		while( parser.anyOf(" \t\r\n") );
	}

}
