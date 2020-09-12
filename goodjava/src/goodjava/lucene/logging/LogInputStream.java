package goodjava.lucene.logging;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.util.BytesRef;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public class LogInputStream extends DataInputStream {
	private static final Logger logger = LoggerFactory.getLogger(LogInputStream.class);

	public LogInputStream(InputStream in) {
		super(in);
	}

	public Object readObject() throws IOException {
		int type = readByte();
		return readObject(type);
	}

	protected Object readObject(int type) throws IOException {
		switch(type) {
		case LogFile.TYPE_NULL:
			return null;
		case LogFile.TYPE_STRING:
			return readUTF();
		case LogFile.TYPE_INT:
			return readInt();
		case LogFile.TYPE_LONG:
			return readLong();
		case LogFile.TYPE_FLOAT:
			return readFloat();
		case LogFile.TYPE_DOUBLE:
			return readDouble();
		case LogFile.TYPE_BYTES:
			return readByteArray();
		case LogFile.TYPE_LIST:
			return readList();
		case LogFile.TYPE_QUERY_MATCH_ALL_DOCS:
			return new MatchAllDocsQuery();
		case LogFile.TYPE_QUERY_TERM:
			return new TermQuery( readTerm() );
		case LogFile.TYPE_QUERY_PREFIX:
			return new PrefixQuery( readTerm() );
		case LogFile.TYPE_QUERY_WILDCARD:
			return new WildcardQuery( readTerm() );
		case LogFile.TYPE_QUERY_TERM_RANGE:
			{
				String field = readUTF();
				BytesRef lowerTerm = readBytesRef();
				BytesRef upperTerm = readBytesRef();
				boolean includeLower = readBoolean();
				boolean includeUpper = readBoolean();
				return new TermRangeQuery(field,lowerTerm,upperTerm,includeLower,includeUpper);
			}
		case LogFile.TYPE_QUERY_PHRASE:
			{
				PhraseQuery query = new PhraseQuery();
				int n = readInt();
				for( int i=0; i<n; i++ ) {
					Term term = readTerm();
					int position = readInt();
					query.add(term,position);
				}
				return query;
			}
		case LogFile.TYPE_QUERY_NUMERIC_RANGE:
			{
				String field = readUTF();
				Number min = (Number)readObject();
				Number max = (Number)readObject();
				boolean minInclusive = readBoolean();
				boolean maxInclusive = readBoolean();
				Number n = min!=null ? min : max;
				if( n instanceof Integer )
					return NumericRangeQuery.newIntRange(field,(Integer)min,(Integer)max,minInclusive,maxInclusive);
				if( n instanceof Long )
					return NumericRangeQuery.newLongRange(field,(Long)min,(Long)max,minInclusive,maxInclusive);
				if( n instanceof Float )
					return NumericRangeQuery.newFloatRange(field,(Float)min,(Float)max,minInclusive,maxInclusive);
				if( n instanceof Double )
					return NumericRangeQuery.newDoubleRange(field,(Double)min,(Double)max,minInclusive,maxInclusive);
				throw new RuntimeException("bad numeric type for "+n);
			}
		case LogFile.TYPE_QUERY_BOOLEAN:
			{
				BooleanQuery query = new BooleanQuery();
				int n = readInt();
				for( int i=0; i<n; i++ ) {
					Query subquery = readQuery();
					BooleanClause.Occur occur = BooleanClause.Occur.valueOf( readUTF() );
					query.add(subquery,occur);
				}
				return query;
			}
		default:
			throw new RuntimeException("invalid type "+type);
		}
	}

	public byte[] readByteArray() throws IOException {
		int len = readInt();
		byte[] bytes = new byte[len];
		readFully(bytes);
		return bytes;
	}

	public List readList() throws IOException {
		final int size = readInt();
		List list = new ArrayList(size);
		for( int i=0; i<size; i++ ) {
			list.add( readObject() );
		}
		return list;
	}

	public Map readMap() throws IOException {
		final int size = readInt();
		Map map = new LinkedHashMap();
		for( int i=0; i<size; i++ ) {
			Object key = readObject();
			Object value = readObject();
			map.put(key,value);
		}
		return map;
	}

	public Query readQuery() throws IOException {
		return (Query)readObject();
	}

	public BytesRef readBytesRef() throws IOException {
		return new BytesRef( readByteArray() );
	}

	public Term readTerm() throws IOException {
		String key = readUTF();
		BytesRef value = readBytesRef();
		return new Term(key,value);
	}

}
