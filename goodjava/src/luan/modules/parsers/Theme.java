package luan.modules.parsers;

import luan.LuanException;
import goodjava.parser.Parser;
import goodjava.parser.ParseException;


public final class Theme {

	public static String toLuan(String source) throws LuanException {
		try {
			return new Theme(source).parse();
		} catch(ParseException e) {
			throw new LuanException(e.getMessage());
		}
	}

	private final Parser parser;

	private Theme(String source) {
		this.parser = new Parser(source);
	}

	private ParseException exception(String msg) {
//		parser.failure();
		return new ParseException(parser,msg);
	}

	private String parse() throws ParseException {
		StringBuilder stmts = new StringBuilder();
		stmts.append( "local M = {};  " );
		while( !parser.endOfInput() ) {
			String def = parseDef();
			if( def != null ) {
				stmts.append(def);
			} else {
//				parser.anyChar();
				stmts.append(parsePadding());
			}
		}
		stmts.append( "\n\nreturn M\n" );
		return stmts.toString();
	}

	private String parsePadding() throws ParseException {
		int start = parser.currentIndex();
		if( parser.match("--") ) {
			while( parser.noneOf("\r\n") );
		} else if( !parser.anyOf(" \t\r\n") ) {
			throw exception("unexpected text");
		}
		return parser.textFrom(start);
	}

	private String parseDef() throws ParseException {
		int start = parser.begin();
		if( !parser.match('{') )
			return parser.failure(null);
		spaces();
		if( !parser.match("define:") )
			return parser.failure(null);
		String name = parseName();
		if( name==null )
			throw exception("invalid block name");
		spaces();
		if( !parser.match('}') )
			throw exception("unclosed define tag");
		String block = parseBody("define:"+name);
		String rtn = "function M." + name + "(env) " + block + " end;  ";
		return parser.success(rtn);
	}

	private String parseBody(String tagName) throws ParseException {
		StringBuilder stmts = new StringBuilder();
		int start = parser.currentIndex();
		int end = start;
		while( !matchEndTag(tagName) ) {
			if( parser.endOfInput() ) {
				parser.failure();
				throw exception("unclosed block");
			}
			String block = parseBlock();
			if( block != null ) {
				addText(start,end,stmts);
				start = parser.currentIndex();
				stmts.append(block);
				continue;
			}
			String simpleTag = parseSimpleTag();
			if( simpleTag != null ) {
				addText(start,end,stmts);
				start = parser.currentIndex();
				stmts.append(simpleTag);
				continue;
			}
			if( parser.match("<%") ) {
				addText(start,end,stmts);
				start = parser.currentIndex();
				stmts.append("%><%='<%'%><%");
				continue;
			}
			parser.anyChar();
			end = parser.currentIndex();
		}
		addText(start,end,stmts);
		return stmts.toString();
	}

	private boolean matchEndTag(String tagName) {
		parser.begin();
		if( !parser.match('{') )
			return parser.failure();
		spaces();
		if( !(parser.match('/') && parser.match(tagName)) )
			return parser.failure();
		spaces();
		if( !parser.match('}') )
			return parser.failure();
		return parser.success();
	}

	private void addText(int start,int end,StringBuilder stmts) {
		if( start < end ) {
			stmts.append( "%>" ).append( parser.text.substring(start,end) ).append( "<%" );
		}
	}

	private String parseBlock() throws ParseException {
		int start = parser.begin();
		if( !parser.match('{') )
			return parser.failure(null);
		spaces();
		if( !parser.match("block:") )
			return parser.failure(null);
		String name = parseName();
		if( name==null ) {
			parser.failure();
			throw exception("invalid block name");
		}
		spaces();
		if( !parser.match('}') )
			return parser.failure(null);
		String block = parseBody("block:"+name);
		String rtn = " env."+ name + "( env, function(env) " + block + "end); ";
//		String rtn = "<% env." + tag.name + "(" + (tag.attrs.isEmpty() ? "nil" : table(tag.attrs)) + ",env,function(env) %>" + block + "<% end) %>";
		return parser.success(rtn);
	}

	private String parseSimpleTag() throws ParseException {
		int start = parser.begin();
		if( !parser.match('{') )
			return parser.failure(null);
		spaces();
		String name = parseName();
		if( name==null )
			return parser.failure(null);
		spaces();
		if( !parser.match('}') )
			return parser.failure(null);
//		rtn = "<% env." + name + (attrs.isEmpty() ? "()" : table(attrs)) + " %>";
		String rtn = " env." + name + "(env); ";
		return parser.success(rtn);
	}

	private boolean BlankLine() {
		parser.begin();
		while( parser.anyOf(" \t") );
		return EndOfLine() ? parser.success() : parser.failure();
	}

	private boolean EndOfLine() {
		return parser.match( "\r\n" ) || parser.match( '\r' ) || parser.match( '\n' );
	}

	private String parseName() throws ParseException {
		int start = parser.begin();
		if( parser.match('/') ) {
			parser.failure();
			throw exception("bad closing tag");
		}
		if( parser.match("define:") ) {
			parser.failure();
			throw exception("unexpected definition");
		}
		if( !FirstNameChar() )
			return parser.failure(null);
		while( NameChar() );
		String match = parser.textFrom(start);
		return parser.success(match);
	}

	private boolean FirstNameChar() {
		return parser.inCharRange('a', 'z') || parser.inCharRange('A', 'Z') || parser.match('_');
	}

	private boolean NameChar() {
		return FirstNameChar() || parser.inCharRange('0', '9');
	}

	private void spaces() {
		while( parser.anyOf(" \t") );
	}

}
