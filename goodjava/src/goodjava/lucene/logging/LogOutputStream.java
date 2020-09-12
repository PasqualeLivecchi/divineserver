package goodjava.lucene.logging;

import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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


public class LogOutputStream extends DataOutputStream {
	private static final Logger logger = LoggerFactory.getLogger(LogOutputStream.class);
	public final LogFile logFile;
	private final RandomAccessFile raf;

	protected LogOutputStream(LogFile logFile,RandomAccessFile raf,OutputStream out) throws IOException {
		super(out);
		this.logFile = logFile;
		this.raf = raf;
		raf.seek(logFile.end);
	}

	public void commit() throws IOException {
		flush();
		long end = raf.getFilePointer();
		raf.seek(0L);
		raf.writeLong(end);
		logFile.end = end;
		raf.seek(end);
	}

	public void rollback() throws IOException {
		flush();
		raf.seek(logFile.end);
	}

	public void writeObject(Object obj) throws IOException {
		if( obj==null ) {
			writeByte(LogFile.TYPE_NULL);
			return;
		}
		if( obj instanceof String ) {
			writeByte(LogFile.TYPE_STRING);
			writeUTF((String)obj);
			return;
		}
		if( obj instanceof Integer ) {
			writeByte(LogFile.TYPE_INT);
			writeInt((Integer)obj);
			return;
		}
		if( obj instanceof Long ) {
			writeByte(LogFile.TYPE_LONG);
			writeLong((Long)obj);
			return;
		}
		if( obj instanceof Float ) {
			writeByte(LogFile.TYPE_FLOAT);
			writeFloat((Float)obj);
			return;
		}
		if( obj instanceof Double ) {
			writeByte(LogFile.TYPE_DOUBLE);
			writeDouble((Double)obj);
			return;
		}
		if( obj instanceof byte[] ) {
			writeByte(LogFile.TYPE_BYTES);
			writeByteArray((byte[])obj);
			return;
		}
		if( obj instanceof List ) {
			writeByte(LogFile.TYPE_LIST);
			writeList((List)obj);
			return;
		}
		if( obj instanceof MatchAllDocsQuery ) {
			writeByte(LogFile.TYPE_QUERY_MATCH_ALL_DOCS);
			return;
		}
		if( obj instanceof TermQuery ) {
			writeByte(LogFile.TYPE_QUERY_TERM);
			TermQuery query = (TermQuery)obj;
			writeTerm( query.getTerm() );
			return;
		}
		if( obj instanceof PrefixQuery ) {
			writeByte(LogFile.TYPE_QUERY_PREFIX);
			PrefixQuery query = (PrefixQuery)obj;
			writeTerm( query.getPrefix() );
			return;
		}
		if( obj instanceof WildcardQuery ) {
			writeByte(LogFile.TYPE_QUERY_TERM_RANGE);
			WildcardQuery query = (WildcardQuery)obj;
			writeTerm( query.getTerm() );
			return;
		}
		if( obj instanceof TermRangeQuery ) {
			writeByte(LogFile.TYPE_QUERY_TERM_RANGE);
			TermRangeQuery query = (TermRangeQuery)obj;
			writeUTF( query.getField() );
			writeBytesRef( query.getLowerTerm() );
			writeBytesRef( query.getUpperTerm() );
			writeBoolean( query.includesLower() );
			writeBoolean( query.includesUpper() );
			return;
		}
		if( obj instanceof PhraseQuery ) {
			writeByte(LogFile.TYPE_QUERY_PHRASE);
			PhraseQuery query = (PhraseQuery)obj;
			Term[] terms = query.getTerms();
			int[] positions = query.getPositions();
			if( terms.length != positions.length )
				throw new RuntimeException();
			writeInt( terms.length );
			for( int i=0; i<terms.length; i++ ) {
				writeTerm( terms[i] );
				writeInt( positions[i] );
			}
			return;
		}
		if( obj instanceof NumericRangeQuery ) {
			writeByte(LogFile.TYPE_QUERY_NUMERIC_RANGE);
			NumericRangeQuery query = (NumericRangeQuery)obj;
			writeUTF( query.getField() );
			writeObject( query.getMin() );
			writeObject( query.getMax() );
			writeBoolean( query.includesMin() );
			writeBoolean( query.includesMax() );
			return;
		}
		if( obj instanceof BooleanQuery ) {
			writeByte(LogFile.TYPE_QUERY_BOOLEAN);
			BooleanQuery query = (BooleanQuery)obj;
			BooleanClause[] a = query.getClauses();
			writeInt(a.length);
			for( BooleanClause bc : a ) {
				writeQuery( bc.getQuery() );
				writeUTF( bc.getOccur().name() );
			}
			return;
		}
		throw new IllegalArgumentException("invalid type for "+obj);
	}

	public void writeByteArray(byte[] bytes) throws IOException {
		writeInt(bytes.length);
		write(bytes);
	}

	public void writeList(List list) throws IOException {
		writeInt(list.size());
		for( Object obj : list ) {
			writeObject(obj);
		}
	}

	public void writeMap(Map map) throws IOException {
		writeInt(map.size());
		for( Object obj : map.entrySet() ) {
			Map.Entry entry = (Map.Entry)obj;
			writeObject( entry.getKey() );
			writeObject( entry.getValue() );
		}
	}

	public void writeQuery(Query query) throws IOException {
		writeObject(query);
	}

	public void writeBytesRef(BytesRef br) throws IOException {
		writeInt(br.length);
		write(br.bytes,0,br.length);
	}

	public void writeTerm(Term term) throws IOException {
		writeUTF(term.field());
		writeBytesRef( term.bytes() );
	}

}
