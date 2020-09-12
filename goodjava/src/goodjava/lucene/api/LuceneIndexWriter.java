package goodjava.lucene.api;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public final class LuceneIndexWriter implements GoodIndexWriter {
	private static final Logger logger = LoggerFactory.getLogger(LuceneIndexWriter.class);
	private final FieldAnalyzer fieldAnalyzer = new FieldAnalyzer();
	public final Version luceneVersion;
	public final IndexWriterConfig luceneConfig;
	public final IndexWriter luceneWriter;
	public final GoodIndexWriterConfig goodConfig;
	private final Map<String,Boolean> indexedMap = new HashMap<String,Boolean>();

	public LuceneIndexWriter(Version luceneVersion,Directory dir,GoodIndexWriterConfig goodConfig) throws IOException {
		this.luceneVersion = luceneVersion;
		this.luceneConfig = new IndexWriterConfig(luceneVersion,fieldAnalyzer);
		this.luceneWriter = new IndexWriter(dir,luceneConfig);
		this.goodConfig = goodConfig;
		luceneWriter.commit();  // commit index creation
	}

	public void close() throws IOException {
		luceneWriter.close();
	}

	public void commit() throws IOException {
		luceneWriter.commit();
	}

	public void rollback() throws IOException {
		luceneWriter.rollback();
	}

	public void deleteAll() throws IOException {
		luceneWriter.deleteAll();
	}

	public void deleteDocuments(Query query) throws IOException {
		luceneWriter.deleteDocuments(query);
	}

	public void addDocument(Map<String,Object> storedFields) throws IOException {
		Document doc = newDocument(storedFields);
		luceneWriter.addDocument(doc);
	}

	public void updateDocument(String keyFieldName,Map<String,Object> storedFields) throws IOException {
		if( !isIndexed(keyFieldName) )
			throw new RuntimeException("can't update using unindexed field "+keyFieldName);
		if( fieldAnalyzer.isAdded(keyFieldName) )
			throw new RuntimeException("can't update using analyzeed field "+keyFieldName);
		Document doc = newDocument(storedFields);
		Object keyValue = storedFields.get(keyFieldName);
		if( keyValue==null )
			throw new RuntimeException("no value for field "+keyFieldName);
		Term term = LuceneUtils.term(keyFieldName,keyValue);
		luceneWriter.updateDocument(term,doc);
	}

	private Document newDocument(Map<String,Object> storedFields) {
		Document doc = new Document();
		MoreFieldInfo more = goodConfig.getMoreFieldInfo(storedFields);
		addFields(doc,storedFields,Field.Store.YES,more.boosts);
		addFields(doc,more.unstoredFields,Field.Store.NO,more.boosts);
		return doc;
	}

	private void addFields( Document doc, Map<String,Object> fields, Field.Store store, Map<String,Float> boosts ) {
		for( Map.Entry<String,Object> entry : fields.entrySet() ) {
			String name = entry.getKey();
			Object value = entry.getValue();
			Float boost = boosts.get(name);
			if( value instanceof List ) {
				for( Object v : (List)value ) {
					doc.add( newField(name,v,store,boost) );
				}
			} else {
				doc.add( newField(name,value,store,boost) );
			}
		}
	}

	private Field newField( String name, Object value, Field.Store store, Float boost ) {
		Field field = newField(name,value,store);
		if( boost != null )
			field.setBoost(boost);
		return field;
	}

	private Field newField( String name, Object value, Field.Store store ) {
		boolean isIndexed = isIndexed(name);
		if( store==Field.Store.NO && !isIndexed )
			throw new RuntimeException("field '"+name+"' is unstored and unindexed");
		if( value instanceof String ) {
			String s = (String)value;
			if( !isIndexed ) {
				return new StoredField(name,s);
			} else if( !fieldAnalyzer.isAdded(name) ) {
				return new StringField(name,s,store);
			} else {
				return new TextField(name,s,store);
			}
		} else if( value instanceof Integer ) {
			int i = (Integer)value;
			if( !isIndexed ) {
				return new StoredField(name,i);
			} else {
				return new IntField(name,i,store);
			}
		} else if( value instanceof Long ) {
			long i = (Long)value;
			if( !isIndexed ) {
				return new StoredField(name,i);
			} else {
				return new LongField(name,i,store);
			}
		} else if( value instanceof Double ) {
			double i = (Double)value;
			if( !isIndexed ) {
				return new StoredField(name,i);
			} else {
				return new DoubleField(name,i,store);
			}
		} else if( value instanceof Float ) {
			float i = (Float)value;
			if( !isIndexed ) {
				return new StoredField(name,i);
			} else {
				return new FloatField(name,i,store);
			}
		} else if( value instanceof byte[] ) {
			if( isIndexed )
				throw new RuntimeException("can't index byte field "+name);
			byte[] b = (byte[])value;
			return new StoredField(name, b);
		} else
			throw new RuntimeException("invalid value type "+value.getClass()+"' for field '"+name+"'");
	}

	private synchronized boolean isIndexed(String fieldName) {
		Boolean b = indexedMap.get(fieldName);
		if( b==null ) {
			b = goodConfig.isIndexed(fieldName);
			indexedMap.put(fieldName,b);
			Analyzer analyzer = goodConfig.getAnalyzer(fieldName);
			if( analyzer!=null )
				fieldAnalyzer.add(fieldName,analyzer);
		}
		return b;
	}


	public void reindexDocuments(final String keyFieldName,Query query) throws IOException {
		IndexReader reader = openReader();
		final IndexSearcher searcher = new IndexSearcher(reader);
		searcher.search( query, new GoodCollector(){
			public void collectDoc(int iDoc) throws IOException {
				Document doc = searcher.doc(iDoc);
				Map<String,Object> storedFields = LuceneUtils.toMap(doc);
				updateDocument(keyFieldName,storedFields);
			}
		});
		reader.close();
	}

	public IndexReader openReader() throws IOException {
		return DirectoryReader.open(luceneWriter.getDirectory());
	}

	public void check() throws IOException {
		CheckIndex.Status status = new CheckIndex(luceneWriter.getDirectory()).checkIndex();
		if( !status.clean )
			logger.error("index not clean");
	}
}
