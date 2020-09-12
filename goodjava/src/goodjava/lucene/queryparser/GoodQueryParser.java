package goodjava.lucene.queryparser;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import goodjava.parser.Parser;
import goodjava.parser.ParseException;


public class GoodQueryParser {

	public static Query parseQuery(FieldParser fieldParser,String query) throws ParseException {
		return new GoodQueryParser(fieldParser,query).parseQuery();
	}

	public static String quote(String s) {
		s = s.replace("\\","\\\\");
		s = s.replace("\b","\\b");
		s = s.replace("\f","\\f");
		s = s.replace("\n","\\n");
		s = s.replace("\r","\\r");
		s = s.replace("\t","\\t");
		s = s.replace("\"","\\\"");
		return "\""+s+"\"";
	}

	public static Sort parseSort(FieldParser fieldParser,String sort) throws ParseException {
		return new GoodQueryParser(fieldParser,sort).parseSort();
	}


	private static final String NOT_IN_RANGE = " \t\r\n\":[]{}^+()";
	private static final String NOT_IN_TERM = NOT_IN_RANGE + "-";
	private static final String NOT_IN_FIELD = NOT_IN_TERM + ",";
	private final FieldParser fieldParser;
	private final Parser parser;

	private GoodQueryParser(FieldParser fieldParser,String query) {
		this.fieldParser = fieldParser;
		this.parser = new Parser(query);
		parser.begin();
	}

	ParseException exception(String msg) {
		parser.failure();
		return new ParseException(parser,msg);
	}

	ParseException exception(Exception cause) {
		parser.failure();
		return new ParseException(parser,cause);
	}

	private Query parseQuery() throws ParseException {
		Spaces();
		BooleanQuery bq = new BooleanQuery();
		while( !parser.endOfInput() ) {
			bq.add( Term(null) );
		}
		BooleanClause[] clauses = bq.getClauses();
		switch( clauses.length ) {
		case 0:
			return new MatchAllDocsQuery();
		case 1:
			{
				BooleanClause bc = clauses[0];
				if( bc.getOccur() != BooleanClause.Occur.MUST_NOT )
					return bc.getQuery();
			}
		default:
			return bq;
		}
	}

	private BooleanClause Term(String defaultField) throws ParseException {
		BooleanClause.Occur occur;
		if( parser.match('+') ) {
			occur = BooleanClause.Occur.MUST;
			Spaces();
		} else if( parser.match('-') ) {
			occur = BooleanClause.Occur.MUST_NOT;
			Spaces();
		} else {
			occur = BooleanClause.Occur.SHOULD;
		}
		String field = QueryField();
		if( field == null )
			field = defaultField;
		Query query = NestedTerm(field);
		if( query == null )
			query = RangeTerm(field);
		if( query == null ) {
			parser.begin();
			String match = SimpleTerm(NOT_IN_TERM);
			query = fieldParser.getQuery(this,field,match);
			parser.success();
		}
		if( parser.match('^') ) {
			Spaces();
			int start = parser.begin();
			try {
				while( parser.anyOf("0123456789.") );
				String match = parser.textFrom(start);
				float boost = Float.parseFloat(match);
				query.setBoost(boost);
			} catch(NumberFormatException e) {
				throw exception(e);
			}
			parser.success();
			Spaces();
		}
		BooleanClause bc = new BooleanClause(query,occur);
		return bc;
	}

	private Query NestedTerm(String field) throws ParseException {
		parser.begin();
		if( !parser.match('(') )
			return parser.failure(null);
		BooleanQuery bq = new BooleanQuery();
		while( !parser.match(')') ) {
			if( parser.endOfInput() )
				throw exception("unclosed parentheses");
			bq.add( Term(field) );
		}
		Spaces();
		BooleanClause[] clauses = bq.getClauses();
		switch( clauses.length ) {
		case 0:
			throw exception("empty parentheses");
		case 1:
			{
				BooleanClause bc = clauses[0];
				if( bc.getOccur() != BooleanClause.Occur.MUST_NOT )
					return parser.success(bc.getQuery());
			}
		default:
			return parser.success(bq);
		}
	}

	private Query RangeTerm(String field) throws ParseException {
		parser.begin();
		if( !parser.anyOf("[{") )
			return parser.failure(null);
		boolean includeMin = parser.lastChar() == '[';
		Spaces();
		String minQuery = SimpleTerm(NOT_IN_RANGE);
		TO();
		String maxQuery = SimpleTerm(NOT_IN_RANGE);
		if( !parser.anyOf("]}") )
			throw exception("unclosed range");
		boolean includeMax = parser.lastChar() == ']';
		Spaces();
		Query query = fieldParser.getRangeQuery(this,field,minQuery,maxQuery,includeMin,includeMax);
		return parser.success(query);
	}

	private void TO() throws ParseException {
		parser.begin();
		if( !(parser.match("TO") && Space()) )
			throw exception("'TO' expected");
		Spaces();
		parser.success();
	}

	private String SimpleTerm(String exclude) throws ParseException {
		parser.begin();
		String match = Quoted();
		if( match==null )
			match = Unquoted(exclude);
		if( match.length() == 0 )
			throw exception("invalid input");
		return parser.success(match);
	}

	private String QueryField() throws ParseException {
		parser.begin();
		String match = Field();
		if( match==null || !parser.match(':') )
			return parser.failure((String)null);
		Spaces();
		return parser.success(match);
	}

	private String Field() throws ParseException {
		parser.begin();
		String match = Unquoted(NOT_IN_FIELD);
		if( match.length()==0 )
			return parser.failure((String)null);
		match = StringFieldParser.escape(this,match);
		return parser.success(match);
	}

	private String Quoted() throws ParseException {
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

	private String Unquoted(String exclude) throws ParseException {
		int start = parser.begin();
		while( parser.noneOf(exclude) ) {
			checkEscape();
		}
		String match = parser.textFrom(start);
		Spaces();
		return parser.success(match);
	}

	private void checkEscape() {
		if( parser.lastChar() == '\\' )
			parser.anyChar();
	}

	private void Spaces() {
		while( Space() );
	}

	private boolean Space() {
		return parser.anyOf(" \t\r\n");
	}


	// sort

	private Sort parseSort() throws ParseException {
		Spaces();
		if( parser.endOfInput() )
			return null;
		List<SortField> list = new ArrayList<SortField>();
		list.add( SortField() );
		while( !parser.endOfInput() ) {
			parser.begin();
			if( !parser.match(',') )
				throw exception("',' expected");
			Spaces();
			parser.success();
			list.add( SortField() );
		}
		return new Sort(list.toArray(new SortField[0]));
	}

	private SortField SortField() throws ParseException {
		parser.begin();
		String field = Field();
		if( field==null )
			throw exception("invalid input");
		boolean reverse = !parser.matchIgnoreCase("asc") && parser.matchIgnoreCase("desc");
		Spaces();
		SortField sf = fieldParser.getSortField(this,field,reverse);
		return parser.success(sf);
	}

}
