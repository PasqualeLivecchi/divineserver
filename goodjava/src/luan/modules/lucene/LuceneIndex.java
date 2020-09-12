package luan.modules.lucene;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.CheckIndex;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.TokenGroup;
import goodjava.lucene.analysis.LowercaseAnalyzer;
import goodjava.lucene.queryparser.GoodQueryParser;
import goodjava.lucene.queryparser.FieldParser;
import goodjava.lucene.queryparser.MultiFieldParser;
import goodjava.lucene.queryparser.StringFieldParser;
import goodjava.lucene.queryparser.NumberFieldParser;
import goodjava.parser.ParseException;
import luan.modules.Utils;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanRuntimeException;
import luan.modules.parsers.LuanToString;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public final class LuceneIndex {
	private static final Logger logger = LoggerFactory.getLogger(LuceneIndex.class);

	private static Map<String,Reference<LuceneIndex>> indexes = new HashMap<String,Reference<LuceneIndex>>();

	public static LuceneIndex getLuceneIndex(Luan luan,File indexDir,LuanTable options)
		throws LuanException, IOException, ClassNotFoundException, SQLException
	{
		String key = indexDir.getCanonicalPath();
		synchronized(indexes) {
			Reference<LuceneIndex> ref = indexes.get(key);
			if( ref != null ) {
				LuceneIndex li = ref.get();
				if( li != null ) {
					Object version = options.get("version");
					if( version==null || version.equals(li.version) )
						return li;
					li.closeWriter();
				}
			}
			LuceneIndex li = new LuceneIndex(luan,indexDir,options);
			indexes.put(key, new WeakReference<LuceneIndex>(li));
			return li;
		}
	}

	private static final Version luceneVersion = Version.LUCENE_4_9;
	private static final String FLD_NEXT_ID = "nextId";
	public static final StringFieldParser STRING_FIELD_PARSER = new StringFieldParser(new KeywordAnalyzer());
//	public static final StringFieldParser LOWERCASE_FIELD_PARSER = new StringFieldParser(new LowercaseAnalyzer(luceneVersion));
	public static final StringFieldParser ENGLISH_FIELD_PARSER = new StringFieldParser(new EnglishAnalyzer(luceneVersion));

	private final Object version;

	private final ReentrantLock writeLock = new ReentrantLock();
	private final File indexDir;
	private SnapshotDeletionPolicy snapshotDeletionPolicy;
	private IndexWriter writer;
	private DirectoryReader reader;
	private IndexSearcher searcher;
	private final ThreadLocal<IndexSearcher> threadLocalSearcher = new ThreadLocal<IndexSearcher>();
	private final MultiFieldParser mfp;
	private final Analyzer analyzer;

	private FSDirectory fsDir;
	private int writeCount;
	private AtomicInteger writeCounter = new AtomicInteger();

	private Set<String> indexOnly = new HashSet<String>();
//	private final FieldParser defaultFieldParser;
//	private final String[] defaultFields;

	private final PostgresBackup postgresBackup;
	private boolean wasCreated;

	private LuceneIndex(Luan luan,File indexDir,LuanTable options)
		throws LuanException, IOException, ClassNotFoundException, SQLException
	{
		options = new LuanTable(options);
		this.version = options.remove("version");
		FieldParser defaultFieldParser = (FieldParser)options.remove("default_type");
		LuanTable defaultFieldsTbl = Utils.removeTable(options,"default_fields");
		String[] defaultFields = defaultFieldsTbl==null ? null : (String[])defaultFieldsTbl.asList().toArray(new String[0]);
		LuanTable postgresSpec = Utils.removeTable(options,"postgres_spec");
		Utils.checkEmpty(options);

//		this.defaultFieldParser = defaultFieldParser;
//		this.defaultFields = defaultFields;
		mfp = defaultFieldParser==null ? new MultiFieldParser() : new MultiFieldParser(defaultFieldParser,defaultFields);
		mfp.fields.put( "type", STRING_FIELD_PARSER );
		mfp.fields.put( "id", NumberFieldParser.LONG );
		this.indexDir = indexDir;
		Analyzer analyzer = STRING_FIELD_PARSER.analyzer;
		if( defaultFieldParser instanceof StringFieldParser ) {
			StringFieldParser sfp = (StringFieldParser)defaultFieldParser;
			analyzer = sfp.analyzer;
		}
		this.analyzer = analyzer;
		wasCreated = reopen();
		if( postgresSpec == null ) {
			postgresBackup = null;
		} else {
			postgresBackup = new PostgresBackup(luan,postgresSpec);
			if( !wasCreated && postgresBackup.wasCreated ) {
				logger.error("rebuilding postgres backup");
				rebuild_postgres_backup(luan);
/*
			} else if( wasCreated && !postgresBackup.wasCreated ) {
				logger.error("restoring from postgres");
				restore_from_postgres();
*/
			}
		}
	}

	public boolean reopen() throws IOException {
		IndexWriterConfig conf = new IndexWriterConfig(luceneVersion,analyzer);
		snapshotDeletionPolicy = new SnapshotDeletionPolicy(conf.getIndexDeletionPolicy());
		conf.setIndexDeletionPolicy(snapshotDeletionPolicy);
		fsDir = FSDirectory.open(indexDir);
		boolean wasCreated = !fsDir.getDirectory().exists();
		writer = new IndexWriter(fsDir,conf);
		writer.commit();  // commit index creation
		reader = DirectoryReader.open(fsDir);
		searcher = new IndexSearcher(reader);
		initId();
		return wasCreated;
	}

	private void wrote() {
		writeCounter.incrementAndGet();
	}

	public void delete_all() throws IOException, SQLException {
		boolean commit = !writeLock.isHeldByCurrentThread();
		writeLock.lock();
		try {
			writer.deleteAll();
			id = idLim = 0;
			if( postgresBackup != null )
				postgresBackup.deleteAll();
			if(commit) writer.commit();
		} finally {
			wrote();
			writeLock.unlock();
		}
	}

	private static Term term(String key,long value) {
		BytesRef br = new BytesRef();
		NumericUtils.longToPrefixCoded(value,0,br);
		return new Term(key,br);
	}

	private void backupDelete(Query query)
		throws IOException, SQLException, LuanException
	{
		if( postgresBackup != null ) {
			final List<Long> ids = new ArrayList<Long>();
			IndexSearcher searcher = openSearcher();
			try {
				MyCollector col = new MyCollector() {
					@Override public void collect(int iDoc) throws IOException {
						Document doc = searcher.doc( docBase + iDoc );
						Long id = (Long)doc.getField("id").numericValue();
						ids.add(id);
					}
				};
				searcher.search(query,col);
			} finally {
				close(searcher);
			}
			postgresBackup.begin();
			for( Long id : ids ) {
				postgresBackup.delete(id);
			}
			postgresBackup.commit();
		}
	}

	public void delete(String queryStr)
		throws IOException, ParseException, SQLException, LuanException
	{
		Query query = GoodQueryParser.parseQuery(mfp,queryStr);

		boolean commit = !writeLock.isHeldByCurrentThread();
		writeLock.lock();
		try {
			backupDelete(query);
			writer.deleteDocuments(query);
			if(commit) writer.commit();
		} finally {
			wrote();
			writeLock.unlock();
		}
	}

	public void indexed_only_fields(List<String> fields) {
		indexOnly.addAll(fields);
	}

	public void save(LuanFunction completer,LuanTable doc,LuanTable boosts)
		throws LuanException, IOException, SQLException
	{
		if( boosts!=null && postgresBackup!=null )
			throw new LuanException("boosts are not saved to postgres backup");

		Object obj = doc.get("id");
		Long id;
		try {
			id = (Long)obj;
		} catch(ClassCastException e) {
			throw new LuanException("id should be Long but is "+obj.getClass().getSimpleName());
		}

		boolean commit = !writeLock.isHeldByCurrentThread();
		writeLock.lock();
		try {
			if( id == null ) {
				id = nextId();
				doc.put("id",id);
				if( postgresBackup != null )
					postgresBackup.add(doc);
				writer.addDocument(toLucene(completer,doc,boosts));
			} else {
				if( postgresBackup != null )
					postgresBackup.update(doc);
				writer.updateDocument( term("id",id), toLucene(completer,doc,boosts) );
			}
			if(commit) writer.commit();
		} finally {
			wrote();
			writeLock.unlock();
		}
	}

	public Object run_in_transaction(LuanFunction fn)
		throws IOException, LuanException, SQLException
	{
		boolean commit = !writeLock.isHeldByCurrentThread();
		writeLock.lock();
		boolean ok = false;
		try {
			if( commit && postgresBackup != null )
				postgresBackup.begin();
			Object rtn = fn.call();
			ok = true;
			if(commit) {
				if( postgresBackup != null )
					postgresBackup.commit();
				writer.commit();
			}
			return rtn;
		} finally {
			if( !ok && commit ) {
				if( postgresBackup != null )
					postgresBackup.rollback();
				writer.rollback();
				reopen();
			}
			wrote();
			writeLock.unlock();
		}
	}

	// ???
	public Object run_in_lock(LuanFunction fn) throws IOException, LuanException {
		if( writeLock.isHeldByCurrentThread() )
			throw new RuntimeException();
		writeLock.lock();
		try {
			synchronized(this) {
				return fn.call();
			}
		} finally {
			wrote();
			writeLock.unlock();
		}
	}


	private long id;
	private long idLim;
	private final int idBatch = 10;

	private void initId() throws IOException {
		TopDocs td = searcher.search(new TermQuery(new Term("type","next_id")),1);
		switch(td.totalHits) {
		case 0:
			id = 0;
			idLim = 0;
			break;
		case 1:
			idLim = (Long)searcher.doc(td.scoreDocs[0].doc).getField(FLD_NEXT_ID).numericValue();
			id = idLim;
			break;
		default:
			throw new RuntimeException();
		}
	}

	private void saveNextId(long nextId) throws LuanException, IOException {
		Map doc = new HashMap();
		doc.put( "type", "next_id" );
		doc.put( FLD_NEXT_ID, idLim );
		writer.updateDocument(new Term("type","next_id"),toLucene(doc.entrySet(),null));
	}

	public synchronized long nextId() throws LuanException, IOException {
		if( ++id > idLim ) {
			idLim += idBatch;
			saveNextId(idLim);
			wrote();
		}
		return id;
	}

/*
	public void backup(String zipFile) throws LuanException, IOException {
		if( !zipFile.endsWith(".zip") )
			throw new LuanException("file "+zipFile+" doesn't end with '.zip'");
		IndexCommit ic = snapshotDeletionPolicy.snapshot();
		try {
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
			for( String fileName : ic.getFileNames() ) {
				out.putNextEntry(new ZipEntry(fileName));
				FileInputStream in = new FileInputStream(new File(indexDir,fileName));
				Utils.copyAll(in,out);
				in.close();
				out.closeEntry();
			}
			out.close();
		} finally {
			snapshotDeletionPolicy.release(ic);
		}
	}
*/
	public SnapshotDeletionPolicy snapshotDeletionPolicy() {
		return snapshotDeletionPolicy;
	}

	public Object snapshot(LuanFunction fn) throws LuanException, IOException {
		IndexCommit ic = snapshotDeletionPolicy.snapshot();
		try {
			String dir = fsDir.getDirectory().toString();
			LuanTable fileNames = new LuanTable(fn.luan(),new ArrayList(ic.getFileNames()));
			return fn.call(dir,fileNames);
		} finally {
			snapshotDeletionPolicy.release(ic);
		}
	}



	public String to_string() {
		return writer.getDirectory().toString();
	}

	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	public void close() throws IOException, SQLException {
		closeWriter();
		reader.close();
	}

	private void closeWriter() throws IOException, SQLException {
		writeLock.lock();
		try {
			writer.close();
			if( postgresBackup != null )
				postgresBackup.close();
		} finally {
			writeLock.unlock();
		}
	}


	private static class DocFn extends LuanFunction {
		final IndexSearcher searcher;
		final Query query;
		int docID;

		DocFn(Luan luan,IndexSearcher searcher,Query query) {
			super(luan);
			this.searcher = searcher;
			this.query = query;
		}

		@Override public Object call(Object[] args) throws LuanException {
			try {
				LuanTable doc = toTable(luan(),searcher.doc(docID));
				if( args.length > 0 && "explain".equals(args[0]) ) {
					Explanation explanation = searcher.explain(query,docID);
					return new Object[]{doc,explanation};
				} else {
					return doc;
				}
			} catch(IOException e) {
				throw new LuanException(e);
			}
		}
	}

	private static abstract class MyCollector extends Collector {
		int docBase;
		int i = 0;

		@Override public void setScorer(Scorer scorer) {}
		@Override public void setNextReader(AtomicReaderContext context) {
			this.docBase = context.docBase;
		}
		@Override public boolean acceptsDocsOutOfOrder() {
			return true;
		}
	}

	private synchronized IndexSearcher openSearcher() throws IOException {
		int gwc = writeCounter.get();
		if( writeCount != gwc ) {
			writeCount = gwc;
			DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
			if( newReader != null ) {
				reader.decRef();
				reader = newReader;
				searcher = new IndexSearcher(reader);
			}
		}
		reader.incRef();
		return searcher;
	}

	// call in finally block
	private static void close(IndexSearcher searcher) throws IOException {
		searcher.getIndexReader().decRef();
	}

	public void ensure_open() throws IOException {
		close(openSearcher());
	}

	public int advanced_search( String queryStr, LuanFunction fn, Integer n, String sortStr )
		throws LuanException, IOException, ParseException
	{
		Utils.checkNotNull(queryStr);
		Query query = GoodQueryParser.parseQuery(mfp,queryStr);
		IndexSearcher searcher = threadLocalSearcher.get();
		boolean inTransaction = searcher != null;
		if( !inTransaction )
			searcher = openSearcher();
		try {
			if( fn!=null && n==null ) {
				if( sortStr != null )
					throw new LuanException("sort must be nil when n is nil");
				final DocFn docFn = new DocFn(fn.luan(),searcher,query);
				MyCollector col = new MyCollector() {
					@Override public void collect(int doc) {
						try {
							docFn.docID = docBase + doc;
							fn.call(++i,docFn);
						} catch(LuanException e) {
							throw new LuanRuntimeException(e);
						}
					}
				};
				try {
					searcher.search(query,col);
				} catch(LuanRuntimeException e) {
					throw (LuanException)e.getCause();
				}
				return col.i;
			}
			if( fn==null || n==0 ) {
				TotalHitCountCollector thcc = new TotalHitCountCollector();
				searcher.search(query,thcc);
				return thcc.getTotalHits();
			}
			Sort sort = sortStr==null ? null : GoodQueryParser.parseSort(mfp,sortStr);
			TopDocs td = sort==null ? searcher.search(query,n) : searcher.search(query,n,sort);
			final ScoreDoc[] scoreDocs = td.scoreDocs;
			DocFn docFn = new DocFn(fn.luan(),searcher,query);
			for( int i=0; i<scoreDocs.length; i++ ) {
				ScoreDoc scoreDoc = scoreDocs[i];
				docFn.docID = scoreDoc.doc;
				fn.call(i+1,docFn,scoreDoc.score);
			}
			return td.totalHits;
		} finally {
			if( !inTransaction )
				close(searcher);
		}
	}

	public Object search_in_transaction(LuanFunction fn) throws LuanException, IOException {
		if( threadLocalSearcher.get() != null )
			throw new LuanException("can't nest search_in_transaction calls");
		IndexSearcher searcher = openSearcher();
		threadLocalSearcher.set(searcher);
		try {
			return fn.call();
		} finally {
			threadLocalSearcher.set(null);
			close(searcher);
		}
	}


	public FieldParser getIndexedFieldParser(String field) {
		return mfp.fields.get(field);
	}

	public void setIndexedFieldParser(String field,FieldParser fp) {
		if( fp==null ) {  // delete
			mfp.fields.remove(field);
			return;
		}
		mfp.fields.put( field, fp );
	}


	private IndexableField newField(String name,Object value,Set<String> indexed,Float boost)
		throws LuanException
	{
		boolean hasBoost = boost!=null;
		IndexableField fld = newField2(name,value,indexed,hasBoost);
		if( hasBoost )
			((Field)fld).setBoost(boost);
		return fld;
	}

	private IndexableField newField2(String name,Object value,Set<String> indexed,boolean hasBoost)
		throws LuanException
	{
		Field.Store store = indexOnly.contains(name) ? Field.Store.NO : Field.Store.YES;
		if( value instanceof String ) {
			String s = (String)value;
			FieldParser fp = mfp.fields.get(name);
			if( fp != null ) {
				if( fp instanceof StringFieldParser && fp != STRING_FIELD_PARSER ) {
					return new TextField(name, s, store);
				} else if (hasBoost) {
					// fuck you modern lucene developers
					return new Field(name, s, store, Field.Index.NOT_ANALYZED);
				} else {
					return new StringField(name, s, store);
				}
			} else {
				return new StoredField(name, s);
			}
		} else if( value instanceof Integer ) {
			int i = (Integer)value;
			if( indexed.contains(name) ) {
				return new IntField(name, i, store);
			} else {
				return new StoredField(name, i);
			}
		} else if( value instanceof Long ) {
			long i = (Long)value;
			if( indexed.contains(name) ) {
				return new LongField(name, i, store);
			} else {
				return new StoredField(name, i);
			}
		} else if( value instanceof Double ) {
			double i = (Double)value;
			if( indexed.contains(name) ) {
				return new DoubleField(name, i, store);
			} else {
				return new StoredField(name, i);
			}
		} else if( value instanceof byte[] ) {
			byte[] b = (byte[])value;
			return new StoredField(name, b);
		} else
			throw new LuanException("invalid value type "+value.getClass()+"' for '"+name+"'");
	}

	private Document toLucene(LuanFunction completer,LuanTable table,LuanTable boosts) throws LuanException {
		if( completer != null )
			table = (LuanTable)completer.call(table);
		return toLucene(table.iterable(),boosts);
	}

	private Document toLucene(Iterable<Map.Entry> iterable,LuanTable boosts) throws LuanException {
		Set<String> indexed = mfp.fields.keySet();
		Document doc = new Document();
		for( Map.Entry<Object,Object> entry : iterable ) {
			Object key = entry.getKey();
			if( !(key instanceof String) )
				throw new LuanException("key must be string");
			String name = (String)key;
			Object value = entry.getValue();
			Float boost = null;
			if( boosts != null ) {
				Object obj = boosts.get(name);
				if( obj != null ) {
					if( !(obj instanceof Number) )
						throw new LuanException("boost '"+name+"' must be number");
					boost = ((Number)obj).floatValue();
				}
			}
			if( !(value instanceof LuanTable) ) {
				doc.add(newField( name, value, indexed, boost ));
			} else { // list
				LuanTable list = (LuanTable)value;
				if( !list.isList() )
					throw new LuanException("table value for '"+name+"' must be a list");
				for( Object el : list.asList() ) {
					doc.add(newField( name, el, indexed, boost ));
				}
			}
		}
		return doc;
	}

	private static Object getValue(IndexableField ifld) throws LuanException {
		BytesRef br = ifld.binaryValue();
		if( br != null )
			return br.bytes;
		Number n = ifld.numericValue();
		if( n != null )
			return n;
		String s = ifld.stringValue();
		if( s != null )
			return s;
		throw new LuanException("invalid field type for "+ifld);
	}

	private static LuanTable toTable(Luan luan,Document doc) throws LuanException {
		if( doc==null )
			return null;
		LuanTable table = new LuanTable(luan);
		for( IndexableField ifld : doc ) {
			String name = ifld.name();
			Object value = getValue(ifld);
			Object old = table.rawGet(name);
			if( old == null ) {
				table.rawPut(name,value);
			} else {
				LuanTable list;
				if( old instanceof LuanTable ) {
					list = (LuanTable)old;
				} else {
					list = new LuanTable(luan);
					list.rawPut(1,old);
					table.rawPut(name,list);
				}
				list.rawPut(list.rawLength()+1,value);
			}
		}
		return table;
	}


	private static final Formatter nullFormatter = new Formatter() {
		public String highlightTerm(String originalText,TokenGroup tokenGroup) {
			return originalText;
		}
	};

	public LuanFunction highlighter(String queryStr,final LuanFunction formatter,final Integer fragmentSize,String dotdotdot)
		throws ParseException
	{
		Query query = GoodQueryParser.parseQuery(mfp,queryStr);
		Formatter fmt = new Formatter() {
			public String highlightTerm(String originalText,TokenGroup tokenGroup) {
				if( tokenGroup.getTotalScore() <= 0 )
					return originalText;
				try {
					return (String)Luan.first(formatter.call(originalText));
				} catch(LuanException e) {
					throw new LuanRuntimeException(e);
				}
			}
		};
		QueryScorer queryScorer = new QueryScorer(query);
		final Highlighter chooser = fragmentSize==null ? null : new Highlighter(nullFormatter,queryScorer);
		if( chooser != null )
			chooser.setTextFragmenter( new SimpleSpanFragmenter(queryScorer,fragmentSize) );
		final Highlighter hl = new Highlighter(fmt,queryScorer);
		hl.setTextFragmenter( new NullFragmenter() );
		return new LuanFunction(false) {  // ???
			@Override public String call(Object[] args) throws LuanException {
				String text = (String)args[0];
				try {
					if( chooser != null ) {
						String s = chooser.getBestFragment(analyzer,null,text);
						if( s != null ) {
							if( dotdotdot != null ) {
								boolean atStart = text.startsWith(s);
								boolean atEnd = text.endsWith(s);
								if( !atStart )
									s = dotdotdot + s;
								if( !atEnd )
									s = s + dotdotdot;
							}
							text = s;
						} else if( text.length() > fragmentSize ) {
							text = text.substring(0,fragmentSize);
							if( dotdotdot != null )
								text += "...";
						}
					}
					String s = hl.getBestFragment(analyzer,null,text);
					return s!=null ? s : text;
				} catch(LuanRuntimeException e) {
					throw (LuanException)e.getCause();
				} catch(IOException e) {
					throw new RuntimeException(e);
				} catch(InvalidTokenOffsetsException e) {
					throw new RuntimeException(e);
				}
			}
		};
	}

	public int count_tokens(String text)
		throws IOException
	{
		int n = 0;
		TokenStream ts = analyzer.tokenStream(null,text);
		ts.reset();
		while( ts.incrementToken() ) {
			n++;
		}
		ts.close();
		return n;
	}



	public boolean hasPostgresBackup() {
		return postgresBackup != null;
	}

	public void rebuild_postgres_backup(Luan luan)
		throws IOException, LuanException, SQLException
	{
		logger.info("start rebuild_postgres_backup");
		writeLock.lock();
		IndexSearcher searcher = openSearcher();
		boolean ok = false;
		try {
			postgresBackup.begin();
			postgresBackup.deleteAll();
			Query query = new PrefixQuery(new Term("id"));
			MyCollector col = new MyCollector() {
				@Override public void collect(int iDoc) throws IOException {
					try {
						Document doc = searcher.doc( docBase + iDoc );
						LuanTable tbl = toTable(luan,doc);
						postgresBackup.add(tbl);
					} catch(LuanException e) {
						throw new LuanRuntimeException(e);
					} catch(SQLException e) {
						throw new RuntimeException(e);
					}
				}
			};
			try {
				searcher.search(query,col);
			} catch(LuanRuntimeException e) {
				throw (LuanException)e.getCause();
			}
			ok = true;
			postgresBackup.commit();
		} finally {
			close(searcher);
			if( !ok )
				postgresBackup.rollback();
			writeLock.unlock();
		}
		logger.info("end rebuild_postgres_backup");
	}

	public void restore_from_postgres(LuanFunction completer)
		throws IOException, LuanException, SQLException, ParseException
	{
		if( postgresBackup!=null && wasCreated && !postgresBackup.wasCreated ) {
			logger.error("restoring from postgres");
			force_restore_from_postgres(completer);
		}
	}

	public void force_restore_from_postgres(LuanFunction completer)
		throws IOException, LuanException, SQLException, ParseException
	{
		logger.warn("start restore_from_postgres");
		if( postgresBackup==null )
			throw new NullPointerException();
		if( writeLock.isHeldByCurrentThread() )
			throw new RuntimeException();
		writeLock.lock();
		boolean ok = false;
		try {
			writer.deleteAll();
			long nextId = postgresBackup.maxId() + 1;
			postgresBackup.restoreLucene(this,completer);
			id = idLim = nextId;
			saveNextId(nextId);
			ok = true;
			writer.commit();
			wasCreated = false;
		} finally {
			if( !ok ) {
				writer.rollback();
				reopen();
			}
			wrote();
			writeLock.unlock();
		}
		logger.warn("end restore_from_postgres");
	}

	void restore(LuanFunction completer,LuanTable doc)
		throws LuanException, IOException
	{
		writer.addDocument(toLucene(completer,doc,null));
	}

	public void check(Luan luan) throws IOException, SQLException, LuanException, ParseException {
		boolean hasPostgres = postgresBackup != null;
		String msg = "start check";
		if( hasPostgres )
			msg += " with postgres";
		logger.info(msg);
		CheckIndex.Status status = new CheckIndex(fsDir).checkIndex();
		if( !status.clean )
			logger.error("index not clean");
		if( hasPostgres )
			checkPostgres(luan);
		logger.info("end check");
	}

	private void checkPostgres(Luan luan)
		throws IOException, SQLException, LuanException, ParseException
	{
		//logger.info("start postgres check");
		final PostgresBackup.Checker postgresChecker = postgresBackup.newChecker();
		final IndexSearcher searcher = openSearcher();
		try {
			final List<Long> idsLucene = new ArrayList<Long>();
			Query query = new PrefixQuery(new Term("id"));
			MyCollector col = new MyCollector() {
				@Override public void collect(int iDoc) throws IOException {
					Document doc = searcher.doc( docBase + iDoc );
					Long id = (Long)doc.getField("id").numericValue();
					idsLucene.add(id);
				}
			};
			searcher.search(query,col);
			Collections.sort(idsLucene);
			final List<Long> idsPostgres = postgresChecker.getIds();
			final int nLucene = idsLucene.size();
			final int nPostgres = idsPostgres.size();
			int iLucene = 0;
			int iPostgres = 0;
			LuanToString lts = new LuanToString();
			lts.strict = true;
			lts.numberTypes = true;
			while( iLucene < nLucene && iPostgres < nPostgres ) {
				long idLucene = idsLucene.get(iLucene);
				long idPostgres = idsPostgres.get(iPostgres);
				if( idLucene < idPostgres ) {
					iLucene++;
					checkPostgres(luan,postgresChecker,lts,idLucene);
				} else if( idLucene > idPostgres ) {
					iPostgres++;
					checkPostgres(luan,postgresChecker,lts,idPostgres);
				} else {  // ==
					LuanTable docPostgres = postgresChecker.getDoc(idPostgres);
					TopDocs td = searcher.search(new TermQuery(term("id",idLucene)),1);
					if( td.totalHits != 1 )  throw new RuntimeException();
					Document doc = searcher.doc( td.scoreDocs[0].doc );
					LuanTable docLucene = toTable(luan,doc);
					if( !equal(docPostgres,docLucene) ) {
						checkPostgres(luan,postgresChecker,lts,idPostgres);
					}
					iLucene++;
					iPostgres++;
				}
			}
			while( iLucene < nLucene ) {
				long idLucene = idsLucene.get(iLucene++);
				checkPostgres(luan,postgresChecker,lts,idLucene);
			}
			while( iPostgres < nPostgres ) {
				long idPostgres = idsPostgres.get(iPostgres++);
				checkPostgres(luan,postgresChecker,lts,idPostgres);
			}
		} finally {
			close(searcher);
			postgresChecker.close();
		}
	}

	private void checkPostgres(Luan luan,PostgresBackup.Checker postgresChecker,LuanToString lts,long id)
		throws IOException, SQLException, LuanException, ParseException
	{
		//logger.info("check id "+id);
		writeLock.lock();
		try {
			final IndexSearcher searcher = openSearcher();
			try {
				LuanTable docPostgres = postgresChecker.getDoc(id);
				TopDocs td = searcher.search(new TermQuery(term("id",id)),1);
				LuanTable docLucene;
				if( td.totalHits == 0 )  {
					docLucene = null;
				} else if( td.totalHits == 1 ) {
					Document doc = searcher.doc( td.scoreDocs[0].doc );
					docLucene = toTable(luan,doc);
				} else
					throw new RuntimeException();
				if( docPostgres == null ) {
					if( docLucene != null )
						logger.error("id "+id+" found in lucene but not postgres");
					return;
				}
				if( docLucene == null ) {
					logger.error("id "+id+" found in postgres but not lucene");
					return;
				}
				if( !equal(docPostgres,docLucene) ) {
					logger.error("id "+id+" not equal");
					logger.error("lucene = "+lts.toString(docLucene));
					logger.error("postgres = "+lts.toString(docPostgres));
				}
			} finally {
				close(searcher);
			}
		} finally {
			writeLock.unlock();
		}
	}

	private static boolean equal(LuanTable t1,LuanTable t2) throws LuanException {
		return t1!=null && t2!=null && toJava(t1).equals(toJava(t2));
	}

	private static Map toJava(LuanTable t) throws LuanException {
		Map map = t.asMap();
		for( Object obj : map.entrySet() ) {
			Map.Entry entry = (Map.Entry)obj;
			Object value = entry.getValue();
			if( value instanceof LuanTable ) {
				LuanTable v = (LuanTable)value;
				if( !v.isList() )
					logger.error("not list");
				entry.setValue(v.asList());
			}
		}
		return map;
	}
}
