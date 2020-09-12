package goodjava.lucene.queryparser;

import java.util.Map;
import java.util.HashMap;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortField;
import goodjava.parser.ParseException;


public class MultiFieldParser implements FieldParser {

	/**
	 * maps field name to FieldParser
	 */
	public final Map<String,FieldParser> fields = new HashMap<String,FieldParser>();
	public boolean allowUnspecifiedFields = false;
	private final FieldParser defaultFieldParser;
	private final String[] defaultFields;

	public MultiFieldParser() {
		this.defaultFieldParser = null;
		this.defaultFields = null;
	}

	public MultiFieldParser(FieldParser defaultFieldParser,String... defaultFields) {
		this.defaultFieldParser = defaultFieldParser;
		this.defaultFields = defaultFields;
		for( String field : defaultFields ) {
			fields.put(field,defaultFieldParser);
		}
	}

	@Override public Query getQuery(GoodQueryParser qp,String field,String query) throws ParseException {
		if( field == null ) {
			if( defaultFieldParser == null )
				throw qp.exception("no defaults were specified, so a field is required");
			if( defaultFields.length == 1 )
				return defaultFieldParser.getQuery(qp,defaultFields[0],query);
			BooleanQuery bq = new BooleanQuery();
			for( String f : defaultFields ) {
				bq.add( defaultFieldParser.getQuery(qp,f,query), BooleanClause.Occur.SHOULD );
			}
			return bq;
		} else {
			FieldParser fp = fields.get(field);
			if( fp != null )
				return fp.getQuery(qp,field,query);
			if( allowUnspecifiedFields )
				return defaultFieldParser.getQuery(qp,field,query);
			throw qp.exception("unrecognized field '"+field+"'");
		}
	}

	@Override public Query getRangeQuery(GoodQueryParser qp,String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) throws ParseException {
		if( field == null ) {
			if( defaultFieldParser == null )
				throw qp.exception("no defaults were specified, so a field is required");
			if( defaultFields.length == 1 )
				return defaultFieldParser.getRangeQuery(qp,defaultFields[0],minQuery,maxQuery,includeMin,includeMax);
			BooleanQuery bq = new BooleanQuery();
			for( String f : defaultFields ) {
				bq.add( defaultFieldParser.getRangeQuery(qp,f,minQuery,maxQuery,includeMin,includeMax), BooleanClause.Occur.SHOULD );
			}
			return bq;
		} else {
			FieldParser fp = fields.get(field);
			if( fp != null )
				return fp.getRangeQuery(qp,field,minQuery,maxQuery,includeMin,includeMax);
			if( allowUnspecifiedFields )
				return defaultFieldParser.getRangeQuery(qp,field,minQuery,maxQuery,includeMin,includeMax);
			throw qp.exception("field '"+field+"' not specified");
		}
	}

	@Override public SortField getSortField(GoodQueryParser qp,String field,boolean reverse) throws ParseException {
		FieldParser fp = fields.get(field);
		if( fp != null )
			return fp.getSortField(qp,field,reverse);
		if( allowUnspecifiedFields )
			return defaultFieldParser.getSortField(qp,field,reverse);
		throw qp.exception("field '"+field+"' not specified");
	}

}
