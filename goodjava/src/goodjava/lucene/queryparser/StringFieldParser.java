package goodjava.lucene.queryparser;

import java.io.StringReader;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.SortField;
import org.apache.lucene.index.Term;
import goodjava.parser.ParseException;


public class StringFieldParser implements FieldParser {
	public int slop = 0;
	public final Analyzer analyzer;

	public StringFieldParser(Analyzer analyzer) {
		this.analyzer = analyzer;
	}

	@Override public Query getQuery(GoodQueryParser qp,String field,String query) throws ParseException {
		String wildcard = wildcard(qp,query);
		if( wildcard != null )
			return new WildcardQuery(new Term(field,wildcard));
		if( query.endsWith("*") && !query.endsWith("\\*") )
			return new PrefixQuery(new Term(field,query.substring(0,query.length()-1)));
		query = escape(qp,query);
		PhraseQuery pq = new PhraseQuery();
		try {
			TokenStream ts = analyzer.tokenStream(field,new StringReader(query));
			CharTermAttribute termAttr = ts.addAttribute(CharTermAttribute.class);
			PositionIncrementAttribute posAttr = ts.addAttribute(PositionIncrementAttribute.class);
			ts.reset();
			int pos = -1;
			while( ts.incrementToken() ) {
				pos += posAttr.getPositionIncrement();
				pq.add( new Term(field,termAttr.toString()), pos );
			}
			ts.end();
			ts.close();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		Term[] terms = pq.getTerms();
		if( terms.length==1 && pq.getPositions()[0]==0 )
			return new TermQuery(terms[0]);
		return pq;
	}

	@Override public Query getRangeQuery(GoodQueryParser qp,String field,String minQuery,String maxQuery,boolean includeMin,boolean includeMax) throws ParseException {
		minQuery = minQuery.equals("*") ? null : escape(qp,minQuery);
		maxQuery = maxQuery.equals("*") ? null : escape(qp,maxQuery);
		return TermRangeQuery.newStringRange(field,minQuery,maxQuery,includeMin,includeMax);
	}

	static String escape(GoodQueryParser qp,String s) throws ParseException {
		final char[] a = s.toCharArray();
		int i, n;
		if( a[0] == '"' ) {
			if( a[a.length-1] != '"' )  throw new RuntimeException();
			i = 1;
			n = a.length - 1;
		} else {
			i = 0;
			n = a.length;
		}
		StringBuilder sb = new StringBuilder();
		for( ; i<n; i++ ) {
			char c = a[i];
			if( c == '\\' ) {
				if( ++i == a.length )
					throw qp.exception("ends with '\\'");
				c = a[i];
			}
			sb.append(c);
		}
		return sb.toString();
	}

	private static String wildcard(GoodQueryParser qp,String s) throws ParseException {
		final char[] a = s.toCharArray();
		if( a[0] == '"' )
			return null;
		boolean hasWildcard = false;
		StringBuilder sb = new StringBuilder();
		for( int i=0; i<a.length; i++ ) {
			char c = a[i];
			if( c=='?' || c=='*' && i<a.length-1 )
				hasWildcard = true;
			if( c == '\\' ) {
				if( ++i == a.length )
					throw qp.exception("ends with '\\'");
				c = a[i];
				if( c=='?' || c=='*' )
					sb.append('\\');
			}
			sb.append(c);
		}
		return hasWildcard ? sb.toString() : null;
	}

	@Override public SortField getSortField(GoodQueryParser qp,String field,boolean reverse) {
		return new SortField( field, SortField.Type.STRING, reverse );
	}

}
