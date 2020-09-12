package luan.modules.url;

import java.util.Map;
import java.util.HashMap;
import goodjava.parser.Parser;
import goodjava.parser.ParseException;


public final class WwwAuthenticate {
	public final String type;
	public final Map<String,String> options = new HashMap<String,String>();
	private final Parser parser;

	public WwwAuthenticate(String header) throws ParseException {
		parser = new Parser(header);
		type = parseType();
		if( !matchSpace() )
			throw new ParseException(parser,"space expected");
		do {
			while( matchSpace() );
			int start = parser.currentIndex();
			while( parser.inCharRange('a','z') );
			String name = parser.textFrom(start);
			if( name.length() == 0 )
				throw new ParseException(parser,"option name not found");
			if( !parser.match('=') )
				throw new ParseException(parser,"'=' expected");
			if( !parser.match('"') )
				throw new ParseException(parser,"'\"' expected");
			start = parser.currentIndex();
			while( !parser.test('"') ) {
				if( !parser.anyChar() )
					throw new ParseException(parser,"unexpected end of text");
			}
			String value = parser.textFrom(start);
			if( !parser.match('"') )
				throw new ParseException(parser,"'\"' expected");
			options.put(name,value);
			while( matchSpace() );
		} while( parser.match(',') );
		if( !parser.endOfInput() )
			throw new ParseException(parser,"unexpected input");
	}

	private String parseType() throws ParseException {
		if( parser.match("Basic") )
			return "Basic";
		if( parser.match("Digest") )
			return "Digest";
		throw new ParseException(parser,"invalid type");
	}

	private boolean matchSpace() {
		return parser.anyOf(" \t\r\n");
	}
}