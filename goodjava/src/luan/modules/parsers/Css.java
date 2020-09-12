package luan.modules.parsers;

import luan.Luan;
import luan.LuanTable;
import luan.LuanException;
import goodjava.parser.Parser;


public final class Css {

	public static LuanTable style(Luan luan,String text) {
		try {
			return new Css(luan,text).parseStyle();
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	private final Luan luan;
	private final Parser parser;

	private Css(Luan luan,String text) {
		this.luan = luan;
		this.parser = new Parser(text);
	}

	private LuanTable parseStyle() throws LuanException {
		LuanTable tbl = new LuanTable(luan);
		while( matchSpace() );
		while( !parser.endOfInput() ) {
			int start = parser.currentIndex();
			if( !matchPropertyChar() )
				return null;
			while( matchPropertyChar() );
			String prop = parser.textFrom(start).toLowerCase();

			while( matchSpace() );
			if( !parser.match(':') )
				return null;

			start = parser.currentIndex();
			while( !parser.endOfInput() && parser.noneOf(";") );
			String val = parser.textFrom(start).trim();

			tbl.put(prop,val);
			parser.match(';');
			while( matchSpace() );
		}
		return tbl;
	}

	private boolean matchPropertyChar() {
		return parser.inCharRange('a','z')
			|| parser.inCharRange('A','Z')
			|| parser.inCharRange('0','9')
			|| parser.anyOf("_-")
		;
	}

	private boolean matchSpace() {
		return parser.anyOf(" \t\r\n");
	}

}
