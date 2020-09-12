package luan.modules.parsers;

import goodjava.parser.Parser;
import goodjava.parser.ParseException;
import luan.Luan;
import luan.LuanTable;
import luan.LuanException;


public final class LuanParser {

	public static Object parse(Luan luan,String text) throws ParseException {
		return new LuanParser(luan,text).parse();
	}

	private static final Object NULL = new Object();
	private final Luan luan;
	private final Parser parser;

	private LuanParser(Luan luan,String text) {
		this.luan = luan;
		this.parser = new Parser(text);
	}

	private ParseException exception(String msg) {
		return new ParseException(parser,msg);
	}

	private Object parse() throws ParseException {
		do { spaces(); } while( endOfLine() );
		Object value = requiredValue();
		do { spaces(); } while( endOfLine() );
		if( !parser.endOfInput() )
			throw exception("unexpected text");
		return value;
	}

	private Object requiredValue() throws ParseException {
		Object value = value();
		if( value == null )
			throw exception("invalid value");
		if( value == NULL )
			return null;
		return value;
	}

	private Object value() throws ParseException {
		if( parser.match("nil") )
			return NULL;
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
		LuanTable tbl = table();
		if( tbl != null )
			return tbl;
		return null;
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
					case '\'':
					case '\\':
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
		parser.begin();
		if( parser.match("double") ) {
			Number n = inParens();
			if( n==null )
				return parser.failure(null);
			n = Luan.asDouble(n);
			if( n==null )
				return parser.failure(null);
			return n;
		} else if( parser.match("float") ) {
			Number n = inParens();
			if( n==null )
				return parser.failure(null);
			n = Luan.asFloat(n);
			if( n==null )
				return parser.failure(null);
			return n;
		} else if( parser.match("integer") ) {
			Number n = inParens();
			if( n==null )
				return parser.failure(null);
			n = Luan.asInteger(n);
			if( n==null )
				return parser.failure(null);
			return n;
		} else if( parser.match("long") ) {
			Number n = inParens();
			if( n==null )
				return parser.failure(null);
			n = Luan.asLong(n);
			if( n==null )
				return parser.failure(null);
			return n;
		} else {
			Number n = untypedNumber();
			if( n != null )
				return parser.success(n);
			else
				return parser.failure(null);
		}
	}

	private Number inParens() {
		spaces();
		if( !parser.match('(') )
			return null;
		spaces();
		Number n = untypedNumber();
		if( n==null )
			return null;
		spaces();
		if( !parser.match(')') )
			return null;
		return n;
	}

	private Number untypedNumber() {
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

	private LuanTable table() throws ParseException {
		parser.begin();
		if( !parser.match('{') )
			return parser.failure(null);
		LuanTable tbl = new LuanTable(luan);
		do {
			spaces();
			Object obj = value();
			if( obj != null ) {
				if( obj != NULL )
					tbl.rawAdd(obj);
				spaces();
				continue;
			}
			Object key = key();
			if( key != null ) {
				spaces();
				requiredMatch('=');
				spaces();
				Object value = requiredValue();
				spaces();
				try {
					tbl.rawPut(key,value);
				} catch(LuanException e) {
					throw new RuntimeException(e);
				}
			}
		} while( fieldSep() );
		requiredMatch('}');
		return parser.success(tbl);
	}

	private Object key() throws ParseException {
		if( parser.match('[') ) {
			spaces();
			Object key = requiredValue();
			spaces();
			requiredMatch(']');
			return key;
		}
		int start = parser.currentIndex();
		if( nameFirstChar() ) {
			while( nameChar() );
			return parser.textFrom(start);
		}
		return null;
	}

	private boolean nameChar() {
		return nameFirstChar() || parser.inCharRange('0','9');
	}

	private boolean nameFirstChar() {
		return parser.inCharRange('a','z') || parser.inCharRange('A','Z') || parser.match('_');
	}

	private boolean fieldSep() throws ParseException {
		return parser.anyOf(",;") || endOfLine();
	}

	private boolean endOfLine() {
		return parser.match( "\r\n" ) || parser.match( '\r' ) || parser.match( '\n' );
	}

	private void requiredMatch(char c) throws ParseException {
		if( !parser.match(c) )
			throw exception("'"+c+"' expected");
	}

	private void spaces() {
		while( parser.anyOf(" \t") );
	}

}
