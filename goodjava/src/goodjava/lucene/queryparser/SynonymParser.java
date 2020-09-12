package goodjava.lucene.queryparser;

import java.util.Map;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortField;
import goodjava.parser.ParseException;


public class SynonymParser implements FieldParser {
	private final FieldParser fp;
	private final Map<String,String[]> synonymMap;

	public SynonymParser(FieldParser fp,Map<String,String[]> synonymMap) {
		this.fp = fp;
		this.synonymMap = synonymMap;
	}

	protected String[] getSynonyms(String query) {
		return synonymMap.get(query);
	}

	public Query getQuery(GoodQueryParser qp,String field,String query) throws ParseException {
		String[] synonyms = getSynonyms(query);
		if( synonyms == null )
			return fp.getQuery(qp,field,query);
		BooleanQuery bq = new BooleanQuery();
		bq.add( fp.getQuery(qp,field,query), BooleanClause.Occur.SHOULD );
		for( String s : synonyms ) {
			bq.add( fp.getQuery(qp,field,s), BooleanClause.Occur.SHOULD );
		}
		return bq;
	}

	public Query getRangeQuery(GoodQueryParser qp,String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) throws ParseException {
		return fp.getRangeQuery(qp,field,minQuery,maxQuery,includeMin,includeMax);
	}

	public SortField getSortField(GoodQueryParser qp,String field,boolean reverse) throws ParseException {
		return fp.getSortField(qp,field,reverse);
	}
}
