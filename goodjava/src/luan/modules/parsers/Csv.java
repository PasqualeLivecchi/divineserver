package luan.modules.parsers;

import luan.Luan;
import luan.LuanTable;
import luan.LuanException;
import goodjava.parser.Parser;
import goodjava.parser.ParseException;


public final class Csv {

	public static LuanTable toList(Luan luan,String line) throws ParseException {
		try {
			return new Csv(line).parse(luan);
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
	}

	private final Parser parser;

	private Csv(String line) {
		this.parser = new Parser(line);
	}

	private ParseException exception(String msg) {
		return new ParseException(parser,msg);
	}

	private LuanTable parse(Luan luan) throws ParseException, LuanException {
		LuanTable list = new LuanTable(luan);
		while(true) {
			Spaces();
			String field = parseField();
			list.put(list.rawLength()+1,field);
			Spaces();
			if( parser.endOfInput() )
				return list;
			if( !parser.match(',') )
				throw exception("unexpected char");
		}
	}

	private String parseField() throws ParseException {
		parser.begin();
		String rtn;
		if( parser.match('"') ) {
			int start = parser.currentIndex();
			do {
				if( parser.endOfInput() ) {
					parser.failure();
					throw exception("unclosed quote");
				}
			} while( parser.noneOf("\"") );
			rtn = parser.textFrom(start);
			parser.match('"');
		} else {
			int start = parser.currentIndex();
			while( !parser.endOfInput() && parser.noneOf(",") );
			rtn = parser.textFrom(start).trim();
		}
		return parser.success(rtn);
	}

	private void Spaces() {
		while( parser.anyOf(" \t") );
	}

}
