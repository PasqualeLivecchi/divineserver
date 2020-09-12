package goodjava.xml;

import java.util.Map;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import goodjava.parser.Parser;
import goodjava.parser.ParseException;


public final class XmlParser {

	public static XmlElement parse(String text) throws ParseException {
		return new XmlParser(text).parse();
	}

	private final Parser parser;

	private XmlParser(String text) {
		this.parser = new Parser(text);
	}

	private ParseException exception(String msg) {
		return new ParseException(parser,msg);
	}

	private XmlElement parse() throws ParseException {
		spaces();
		prolog();
		spaces();
		XmlElement element = element();
		spaces();
		if( !parser.endOfInput() )
			throw exception("unexpected text");
		return element;
	}

	private void prolog() throws ParseException {
		if( !parser.match("<?xml") )
			return;
		while( attribute() != null );
		spaces();
		required("?>");
	}

	private XmlElement element() throws ParseException {
		parser.begin();
		if( !parser.match('<') || parser.test('/') )
			return parser.failure(null);
		//spaces();
		String name = name();
		if( name==null )
			throw exception("element name not found");
		Map<String,String> attributes = new LinkedHashMap<String,String>();
		Map.Entry<String,String> attribute;
		while( (attribute=attribute()) != null ) {
			attributes.put(attribute.getKey(),attribute.getValue());
		}
		spaces();
		if( parser.match("/>") ) {
			XmlElement element = new XmlElement(name,attributes);
			return parser.success(element);
		}
		required(">");
		String s = string(name);
		if( s != null ) {
			XmlElement element = new XmlElement(name,attributes,s);
			return parser.success(element);
		}
		List<XmlElement> elements = elements(name);
		if( elements != null ) {
			XmlElement element = new XmlElement(name,attributes,elements.toArray(new XmlElement[0]));
			return parser.success(element);
		}
		throw exception("bad element");
	}

	private String string(String name) throws ParseException {
		int start = parser.begin();
		while( parser.noneOf("<") );
		String s = parser.textFrom(start);
		s = decode(s);
		if( !endTag(name) )
			return parser.failure(null);
		return parser.success(s);
	}

	private List<XmlElement> elements(String name) throws ParseException {
		parser.begin();
		List<XmlElement> elements = new ArrayList<XmlElement>();
		spaces();
		XmlElement element;
		while( (element=element()) != null ) {
			elements.add(element);
			spaces();
		}
		if( !endTag(name) )
			return parser.failure(null);
		return parser.success(elements);
	}

	private boolean endTag(String name) throws ParseException {
		parser.begin();
		if( !parser.match("</") || !parser.match(name) )
			return parser.failure();
		spaces();
		if( !parser.match('>') )
			return parser.failure();
		return parser.success();
	}

	private Map.Entry<String,String> attribute() throws ParseException {
		parser.begin();
		if( !matchSpace() )
			return parser.failure(null);
		spaces();
		String name = name();
		if( name==null )
			return parser.failure(null);
		spaces();
		required("=");
		spaces();
		if( !parser.anyOf("\"'") )
			throw exception("quote expected");
		char quote = parser.lastChar();
		int start = parser.currentIndex();
		while( !parser.test(quote) ) {
			if( !parser.anyChar() )
				throw exception("unclosed attribute value");
		}
		String value = parser.textFrom(start);
		value = decode(value);
		parser.match(quote);
		Map.Entry<String,String> attribute = new AbstractMap.SimpleImmutableEntry<String,String>(name,value);
		return parser.success(attribute);
	}

	private String name() {
		int start = parser.currentIndex();
		if( !matchNameChar() )
			return null;
		while( matchNameChar() );
		return parser.textFrom(start);
	}

	private boolean matchNameChar() {
		return parser.inCharRange('a','z')
			|| parser.inCharRange('A','Z')
			|| parser.inCharRange('0','9')
			|| parser.anyOf("_.-:")
		;
	}

	private void required(String s) throws ParseException {
		if( !parser.match(s) )
			exception("'"+s+"' expected");
	}

	private void spaces() throws ParseException {
		while( matchSpace() || matchComment() );
	}

	private boolean matchComment() throws ParseException {
		if( !parser.match("<!--") )
			return false;
		while( !parser.match("-->") ) {
			if( !parser.anyChar() )
				throw exception("unclosed comment");
		}
		return true;
	}

	private boolean matchSpace() {
		return parser.anyOf(" \t\r\n");
	}

	private static String decode(String s) {
		s = s.replace("&lt;","<");
		s = s.replace("&gt;",">");
		s = s.replace("&quot;","\"");
		s = s.replace("&apos;","'");
		s = s.replace("&amp;","&");
		return s;
	}

}
