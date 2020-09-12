package luan.modules.parsers;

import java.util.List;
import java.util.ArrayList;
import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.modules.Utils;
import luan.modules.HtmlLuan;
import goodjava.parser.Parser;


public final class BBCode {

	public static String toHtml(String bbcode,LuanFunction quoter) throws LuanException {
		return new BBCode(bbcode,quoter,true).parse();
	}

	public static String toText(String bbcode,LuanFunction quoter) throws LuanException {
		return new BBCode(bbcode,quoter,false).parse();
	}

	private final Parser parser;
	private final LuanFunction quoter;
	private final boolean toHtml;

	private BBCode(String text,LuanFunction quoter,boolean toHtml) throws LuanException {
		Utils.checkNotNull(text,1);
//		Utils.checkNotNull(quoter,2);
		this.parser = new Parser(text);
		this.quoter = quoter;
		this.toHtml = toHtml;
	}

	private String parse() throws LuanException {
		StringBuilder sb = new StringBuilder();
		StringBuilder text = new StringBuilder();
		while( !parser.endOfInput() ) {
			String block = parseBlock();
			if( block != null ) {
				sb.append( textToString(text) );
				sb.append(block);
			} else {
				text.append( parser.currentChar() );
				parser.anyChar();
			}
		}
		sb.append( textToString(text) );
		return sb.toString();
	}

	private String parseWellFormed() throws LuanException {
		StringBuilder sb = new StringBuilder();
		StringBuilder text = new StringBuilder();
		while( !parser.endOfInput() ) {
			String block = parseBlock();
			if( block != null ) {
				sb.append( textToString(text) );
				sb.append(block);
				continue;
			}
			if( couldBeTag() )
				break;
			text.append( parser.currentChar() );
			parser.anyChar();
		}
		sb.append( textToString(text) );
		return sb.toString();
	}

	private String textToString(StringBuilder text) throws LuanException {
		String s = text.toString();
		text.setLength(0);
		if( toHtml )
			s = HtmlLuan.encode(s);
		return s;
	}

	private boolean couldBeTag() {
		if( parser.currentChar() != '[' )
			return false;
		return parser.testIgnoreCase("[b]")
			|| parser.testIgnoreCase("[/b]")
			|| parser.testIgnoreCase("[i]")
			|| parser.testIgnoreCase("[/i]")
			|| parser.testIgnoreCase("[u]")
			|| parser.testIgnoreCase("[/u]")
			|| parser.testIgnoreCase("[url]")
			|| parser.testIgnoreCase("[url=")
			|| parser.testIgnoreCase("[/url]")
			|| parser.testIgnoreCase("[code]")
			|| parser.testIgnoreCase("[/code]")
			|| parser.testIgnoreCase("[img]")
			|| parser.testIgnoreCase("[/img]")
			|| parser.testIgnoreCase("[color=")
			|| parser.testIgnoreCase("[/color]")
			|| parser.testIgnoreCase("[size=")
			|| parser.testIgnoreCase("[/size]")
			|| parser.testIgnoreCase("[youtube]")
			|| parser.testIgnoreCase("[/youtube]")
			|| parser.testIgnoreCase("[quote]")
			|| parser.testIgnoreCase("[quote=")
			|| parser.testIgnoreCase("[/quote]")
		;
	}

	private String parseBlock() throws LuanException {
		if( parser.currentChar() != '[' )
			return null;
		String s;
		s = parseB();  if(s!=null) return s;
		s = parseI();  if(s!=null) return s;
		s = parseU();  if(s!=null) return s;
		s = parseUrl1();  if(s!=null) return s;
		s = parseUrl2();  if(s!=null) return s;
		s = parseCode();  if(s!=null) return s;
		s = parseImg();  if(s!=null) return s;
		s = parseColor();  if(s!=null) return s;
		s = parseSize();  if(s!=null) return s;
		s = parseYouTube();  if(s!=null) return s;
		s = parseQuote1();  if(s!=null) return s;
		s = parseQuote2();  if(s!=null) return s;
		return null;
	}

	private String parseB() throws LuanException {
		parser.begin();
		if( !parser.matchIgnoreCase("[b]") )
			return parser.failure(null);
		String content = parseWellFormed();
		if( !parser.matchIgnoreCase("[/b]") )
			return parser.failure(null);
		String rtn = toHtml ? "<b>"+content+"</b>" : content;
		return parser.success(rtn);
	}

	private String parseI() throws LuanException {
		parser.begin();
		if( !parser.matchIgnoreCase("[i]") )
			return parser.failure(null);
		String content = parseWellFormed();
		if( !parser.matchIgnoreCase("[/i]") )
			return parser.failure(null);
		String rtn = toHtml ? "<i>"+content+"</i>" : content;
		return parser.success(rtn);
	}

	private String parseU() throws LuanException {
		parser.begin();
		if( !parser.matchIgnoreCase("[u]") )
			return parser.failure(null);
		String content = parseWellFormed();
		if( !parser.matchIgnoreCase("[/u]") )
			return parser.failure(null);
		String rtn = toHtml ? "<u>"+content+"</u>" : content;
		return parser.success(rtn);
	}

	private String parseUrl1() {
		parser.begin();
		if( !parser.matchIgnoreCase("[url]") )
			return parser.failure(null);
		String url = parseRealUrl();
		if( !parser.matchIgnoreCase("[/url]") )
			return parser.failure(null);
		String rtn = toHtml ? "<a href='"+url+"'>"+url+"</u>" : url;
		return parser.success(rtn);
	}

	private String parseUrl2() throws LuanException {
		parser.begin();
		if( !parser.matchIgnoreCase("[url=") )
			return parser.failure(null);
		String url = parseRealUrl();
		if( !parser.match(']') )
			return parser.failure(null);
		String content = parseWellFormed();
		if( !parser.matchIgnoreCase("[/url]") )
			return parser.failure(null);
		String rtn = toHtml ? "<a href='"+url+"'>"+content+"</u>" : content;
		return parser.success(rtn);
	}

	private String parseRealUrl() {
		parser.begin();
		while( parser.match(' ') );
		int start = parser.currentIndex();
		if( !parser.matchIgnoreCase("http") )
			return parser.failure(null);
		parser.matchIgnoreCase("s");
		if( !parser.matchIgnoreCase("://") )
			return parser.failure(null);
		while( parser.noneOf(" []'") );
		String url = parser.textFrom(start);
		while( parser.match(' ') );
		return parser.success(url);
	}

	private String parseCode() {
		parser.begin();
		if( !parser.matchIgnoreCase("[code]") )
			return parser.failure(null);
		int start = parser.currentIndex();
		while( !parser.testIgnoreCase("[/code]") ) {
			if( !parser.anyChar() )
				return parser.failure(null);
		}
		String content = parser.textFrom(start);
		if( !parser.matchIgnoreCase("[/code]") ) throw new RuntimeException();
		String rtn = toHtml ? "<code>"+content+"</code>" : content;
		return parser.success(rtn);
	}

	private String parseImg() {
		parser.begin();
		if( !parser.matchIgnoreCase("[img]") )
			return parser.failure(null);
		String url = parseRealUrl();
		if( !parser.matchIgnoreCase("[/img]") )
			return parser.failure(null);
		String rtn = toHtml ? "<img src='"+url+"'>" : "";
		return parser.success(rtn);
	}

	private String parseColor() throws LuanException {
		parser.begin();
		if( !parser.matchIgnoreCase("[color=") )
			return parser.failure(null);
		int start = parser.currentIndex();
		parser.match('#');
		while( parser.inCharRange('0','9')
			|| parser.inCharRange('a','z')
			|| parser.inCharRange('A','Z')
		);
		String color = parser.textFrom(start);
		if( !parser.match(']') )
			return parser.failure(null);
		String content = parseWellFormed();
		if( !parser.matchIgnoreCase("[/color]") )
			return parser.failure(null);
		String rtn = toHtml ? "<span style='color: "+color+"'>"+content+"</span>" : content;
		return parser.success(rtn);
	}

	private String parseSize() throws LuanException {
		parser.begin();
		if( !parser.matchIgnoreCase("[size=") )
			return parser.failure(null);
		int start = parser.currentIndex();
		while( parser.match('.') || parser.inCharRange('0','9') );
		String size = parser.textFrom(start);
		if( !parser.match(']') )
			return parser.failure(null);
		String content = parseWellFormed();
		if( !parser.matchIgnoreCase("[/size]") )
			return parser.failure(null);
		String rtn = toHtml ? "<span style='font-size: "+size+"em'>"+content+"</span>" : content;
		return parser.success(rtn);
	}

	private String parseYouTube() {
		parser.begin();
		if( !parser.matchIgnoreCase("[youtube]") )
			return parser.failure(null);
		int start = parser.currentIndex();
		while( parser.inCharRange('0','9')
			|| parser.inCharRange('a','z')
			|| parser.inCharRange('A','Z')
			|| parser.match('-')
			|| parser.match('_')
		);
		String id = parser.textFrom(start);
		if( id.length()==0 || !parser.matchIgnoreCase("[/youtube]") )
			return parser.failure(null);
		String rtn = toHtml ? "<iframe width='420' height='315' src='https://www.youtube.com/embed/"+id+"' frameborder='0' allowfullscreen></iframe>" : "";
		return parser.success(rtn);
	}

	private String quote(Object... args) throws LuanException {
		if( quoter==null ) {
			if( toHtml )
				throw new LuanException("BBCode quoter function not defined");
			else
				return "";
		}
		Object obj = quoter.call(args);
		if( !(obj instanceof String) )
			throw new LuanException("BBCode quoter function returned "+Luan.type(obj)+" but string required");
		return (String)obj;
	}

	private String parseQuote1() throws LuanException {
		parser.begin();
		if( !parser.matchIgnoreCase("[quote]") )
			return parser.failure(null);
		String content = parseWellFormed();
		if( !parser.matchIgnoreCase("[/quote]") )
			return parser.failure(null);
		String rtn = quote(content);
		return parser.success(rtn);
	}

	private String parseQuote2() throws LuanException {
		parser.begin();
		if( !parser.matchIgnoreCase("[quote=") )
			return parser.failure(null);
		List args = new ArrayList();
		int start = parser.currentIndex();
		while( parser.noneOf("[];") );
		String name = parser.textFrom(start).trim();
		if( name.length() == 0 )
			return parser.failure(null);
		args.add(name);
		while( parser.match(';') ) {
			start = parser.currentIndex();
			while( parser.noneOf("[];'") );
			String src = parser.textFrom(start).trim();
			args.add(src);
		}
		if( !parser.match(']') )
			return parser.failure(null);
		String content = parseWellFormed();
		args.add(0,content);
		if( !parser.matchIgnoreCase("[/quote]") )
			return parser.failure(null);
		String rtn = quote(args.toArray());
		return parser.success(rtn);
	}

}
