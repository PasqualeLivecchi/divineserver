package goodjava.lucene.queryparser;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.index.Term;
import goodjava.parser.ParseException;


public abstract class NumberFieldParser implements FieldParser {

	@Override public final Query getQuery(GoodQueryParser qp,String field,String query) throws ParseException {
		if( query.equals("*") )
			return new PrefixQuery(new Term(field,""));
		return getRangeQuery(qp,field,query,query,true,true);
	}

	@Override public final Query getRangeQuery(GoodQueryParser qp,String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) throws ParseException {
		try {
			return getRangeQuery(field,minQuery,maxQuery,includeMin,includeMax);
		} catch(NumberFormatException e) {
			throw qp.exception(e);
		}
	}

	abstract protected Query getRangeQuery(String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax);

	@Override public SortField getSortField(GoodQueryParser qp,String field,boolean reverse) {
		return new SortField( field, sortType(), reverse );
	}

	abstract protected SortField.Type sortType();


	public static final FieldParser INT = new NumberFieldParser() {

		@Override protected Query getRangeQuery(String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) {
			Integer min = minQuery.equals("*") ? null : Integer.valueOf(minQuery);
			Integer max = maxQuery.equals("*") ? null : Integer.valueOf(maxQuery);
			return NumericRangeQuery.newIntRange(field,min,max,includeMin,includeMax);
		}

		@Override protected SortField.Type sortType() {
			return SortField.Type.INT;
		}
	};

	public static final FieldParser LONG = new NumberFieldParser() {

		@Override protected Query getRangeQuery(String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) {
			Long min = minQuery.equals("*") ? null : Long.valueOf(minQuery);
			Long max = maxQuery.equals("*") ? null : Long.valueOf(maxQuery);
			return NumericRangeQuery.newLongRange(field,min,max,includeMin,includeMax);
		}

		@Override protected SortField.Type sortType() {
			return SortField.Type.LONG;
		}
	};

	public static final FieldParser FLOAT = new NumberFieldParser() {

		@Override protected Query getRangeQuery(String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) {
			Float min = minQuery.equals("*") ? null : Float.valueOf(minQuery);
			Float max = maxQuery.equals("*") ? null : Float.valueOf(maxQuery);
			return NumericRangeQuery.newFloatRange(field,min,max,includeMin,includeMax);
		}

		@Override protected SortField.Type sortType() {
			return SortField.Type.FLOAT;
		}
	};

	public static final FieldParser DOUBLE = new NumberFieldParser() {

		@Override protected Query getRangeQuery(String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) {
			Double min = minQuery.equals("*") ? null : Double.valueOf(minQuery);
			Double max = maxQuery.equals("*") ? null : Double.valueOf(maxQuery);
			return NumericRangeQuery.newDoubleRange(field,min,max,includeMin,includeMax);
		}

		@Override protected SortField.Type sortType() {
			return SortField.Type.DOUBLE;
		}
	};

}
