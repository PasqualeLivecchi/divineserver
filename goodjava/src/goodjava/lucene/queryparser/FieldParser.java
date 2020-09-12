package goodjava.lucene.queryparser;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import goodjava.parser.ParseException;


public interface FieldParser {
	public Query getQuery(GoodQueryParser qp,String field,String query) throws ParseException;
	public Query getRangeQuery(GoodQueryParser qp,String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) throws ParseException;
	public SortField getSortField(GoodQueryParser qp,String field,boolean reverse) throws ParseException;
}
