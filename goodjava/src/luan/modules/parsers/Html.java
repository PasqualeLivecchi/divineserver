package luan.modules.parsers;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import luan.Luan;
import luan.LuanTable;
import luan.LuanException;
import goodjava.parser.Parser;


public final class Html {

	public static LuanTable toList(Luan luan,String text,LuanTable containerTagsTbl) {
		try {
			return new Html(luan,text,containerTagsTbl).parse();
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	private final Luan luan;
	private final Parser parser;
	private final Set<String> containerTags = new HashSet<String>();

	private Html(Luan luan,String text,LuanTable containerTagsTbl) {
		this.luan = luan;
		this.parser = new Parser(text);
		for( Object v : containerTagsTbl.asList() ) {
			containerTags.add((String)v);
		}
	}

	private LuanTable parse() throws LuanException {
		List list = new ArrayList();
		StringBuilder sb = new StringBuilder();
		while( !parser.endOfInput() ) {
			if( parser.test('<') ) {
				LuanTable tbl = parseTag();
				if( tbl != null ) {
					String tagName = (String)tbl.rawGet("name");
					if( containerTags.contains(tagName) ) {
						LuanTable container = parseContainer(tbl);
						if( container != null )
							tbl = container;
					}
					if( tbl != null 
						|| (tbl = parseComment()) != null
						|| (tbl = parseCdata()) != null
					) {
						if( sb.length() > 0 ) {
							list.add(sb.toString());
							sb.setLength(0);
						}
						list.add(tbl);
						continue;
					}
				}
			}
			sb.append( parser.currentChar() );
			parser.anyChar();
		}
		if( sb.length() > 0 )
			list.add(sb.toString());
		return new LuanTable(luan,list);
	}

	private LuanTable parseComment() throws LuanException {
		parser.begin();
		if( !parser.match("<!--") )
			return parser.failure(null);
		int start = parser.currentIndex();
		while( !parser.test("-->") ) {
			if( !parser.anyChar() )
				return parser.failure(null);
		}
		String text = parser.textFrom(start);
		LuanTable tbl = new LuanTable(luan);
		tbl.put("type","comment");
		tbl.put("text",text);
		return parser.success(tbl);
	}

	private LuanTable parseCdata() throws LuanException {
		parser.begin();
		if( !parser.match("<![CDATA[") )
			return parser.failure(null);
		int start = parser.currentIndex();
		while( !parser.test("]]>") ) {
			if( !parser.anyChar() )
				return parser.failure(null);
		}
		String text = parser.textFrom(start);
		LuanTable tbl = new LuanTable(luan);
		tbl.put("type","cdata");
		tbl.put("text",text);
		return parser.success(tbl);
	}

	private LuanTable parseContainer(LuanTable tag) throws LuanException {
		String endTagName = '/' + (String)tag.rawGet("name");
		int start = parser.begin();
		int end;
		while(true) {
			if( parser.test('<') ) {
				end = parser.currentIndex();
				LuanTable tag2 = parseTag();
				String s = (String)tag2.rawGet("name");
				if( s.equals(endTagName) )
					break;
			}
			if( !parser.anyChar() )
				return parser.failure(null);
		}
		String text = parser.text.substring(start,end);
		LuanTable tbl = new LuanTable(luan);
		tbl.put("type","container");
		tbl.put("tag",tag);
		tbl.put("text",text);
		return parser.success(tbl);
	}

	private LuanTable parseTag() throws LuanException {
		LuanTable tbl = new LuanTable(luan);
		tbl.put("type","tag");
		int tagStart = parser.begin();
		if( !parser.match('<') )
			return parser.failure(null);
		int start = parser.currentIndex();
		parser.match('/');
		if( !matchNameChar() )
			return parser.failure(null);
		while( matchNameChar() );
		String name = parser.textFrom(start).toLowerCase();
		tbl.put("name",name);
		LuanTable attributes = new LuanTable(luan);
		tbl.put("attributes",attributes);
		String attrName;
		while( (attrName = parseAttrName()) != null ) {
			String attrValue = parseAttrValue();
			attributes.put( attrName, attrValue!=null ? attrValue : true );
			if( attrName.equals("style") && attrValue!=null ) {
				LuanTable style = Css.style(luan,attrValue);
				if( style!=null )
					tbl.put("style",style);
			}
		}
		while( matchSpace() );
		boolean isEmpty = parser.match('/');
		tbl.put("is_empty",isEmpty);
		if( !parser.match('>') )
			return parser.failure(null);
		String raw = parser.textFrom(tagStart);
		tbl.put("raw",raw);
		return parser.success(tbl);
	}

	private String parseAttrName() {
		parser.begin();
		if( !matchSpace() )
			return parser.failure(null);
		while( matchSpace() );
		int start = parser.currentIndex();
		if( !matchNameChar() )
			return parser.failure(null);
		while( matchNameChar() );
		String name = parser.textFrom(start).toLowerCase();
		return parser.success(name);
	}

	private String parseAttrValue() {
		parser.begin();
		while( matchSpace() );
		if( !parser.match('=') )
			return parser.failure(null);
		while( matchSpace() );
		if( parser.anyOf("\"'") ) {
			char quote = parser.lastChar();
			int start = parser.currentIndex();
			while( !parser.test(quote) ) {
				if( !parser.anyChar() )
					return parser.failure(null);
			}
			String value = parser.textFrom(start);
			parser.match(quote);
			return parser.success(value);
		}
		int start = parser.currentIndex();
		if( !matchValueChar() )
			return parser.failure(null);
		while( matchValueChar() );
		String value = parser.textFrom(start);
		return parser.success(value);
	}

	private boolean matchNameChar() {
		return parser.inCharRange('a','z')
			|| parser.inCharRange('A','Z')
			|| parser.inCharRange('0','9')
			|| parser.anyOf("_.-:")
		;
	}

	private boolean matchValueChar() {
		return parser.noneOf(" \t\r\n\"'>/=");
	}

	private boolean matchSpace() {
		return parser.anyOf(" \t\r\n");
	}

}
