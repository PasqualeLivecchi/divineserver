package goodjava.lucene.logging;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Sort;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import goodjava.io.IoUtils;
import goodjava.lucene.api.GoodIndexWriter;
import goodjava.lucene.api.LuceneIndexWriter;
import goodjava.lucene.api.GoodCollector;
import goodjava.lucene.api.LuceneUtils;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public class LoggingIndexWriter implements GoodIndexWriter {
	private static final Logger logger = LoggerFactory.getLogger(LoggingIndexWriter.class);
	private static final int version = 1;
	private static final int OP_DELETE_ALL = 1;
	private static final int OP_DELETE_DOCUMENTS = 2;
	private static final int OP_ADD_DOCUMENT = 3;
	private static final int OP_UPDATE_DOCUMENT = 4;
	private static final Random rnd = new Random();

	public final LuceneIndexWriter indexWriter;
	private final File logDir;
	protected final List<LogFile> logs = new ArrayList<LogFile>();
	private LogOutputStream log;
	private final File index;
	private boolean isMerging = false;

	public LoggingIndexWriter(LuceneIndexWriter indexWriter,File logDir) throws IOException {
		this.indexWriter = indexWriter;
		this.logDir = logDir;
		IoUtils.mkdirs(logDir);
		if( !logDir.isDirectory() )
			throw new RuntimeException();
		index = new File(logDir,"index");
		if( index.exists() ) {
			DataInputStream dis = new DataInputStream(new FileInputStream(index));
			try {
				if( dis.readInt() == version ) {
					final int n = dis.readInt();
					for( int i=0; i<n; i++ ) {
						File file = new File( logDir, dis.readUTF() );
						logs.add( new LogFile(file) );
					}
					deleteUnusedFiles();
					setLog();
					return;
				}
			} finally {
				dis.close();
			}
		}
		newLogs();
	}

	private void setLog() throws IOException {
		if( log != null )
			log.close();
		log = logs.get(logs.size()-1).output();
	}

	public synchronized boolean isMerging() {
		return isMerging;
	}

	private synchronized void isNotMerging() {
		isMerging = false;
	}

	public synchronized void newLogs() throws IOException {
		if( isMerging )
			throw new RuntimeException("merging");
		logger.info("building new logs");
		logs.clear();
		for( int i=0; i<2; i++ ) {
			logs.add( newLogFile() );
		}
		logLucene( System.currentTimeMillis(), logs.get(0), indexWriter );
		writeIndex();
		setLog();
		logger.info("done building new logs");
	}

	private static void logLucene(long time,LogFile logLucene,LuceneIndexWriter indexWriter) throws IOException {
		LogOutputStream log = logLucene.output();
		IndexReader reader = indexWriter.openReader();
		final IndexSearcher searcher = new IndexSearcher(reader);
		Query query = new MatchAllDocsQuery();
		searcher.search( query, new GoodCollector(){
			public void collectDoc(int iDoc) throws IOException {
				Document doc = searcher.doc(iDoc);
				Map<String,Object> storedFields = LuceneUtils.toMap(doc);
				log.writeLong(time);
				log.writeByte(OP_ADD_DOCUMENT);
				log.writeMap(storedFields);
			}
		});
		reader.close();
		log.commit();
		log.close();
	}

	private LogFile newLogFile() throws IOException {
		File file;
		do {
			file = new File(logDir,"_"+rnd.nextInt(100)+".log");
		} while( file.exists() );
		return new LogFile(file);
	}

	private void deleteUnusedFiles() throws IOException {
		deleteUnusedFiles(logs,index);
	}

	private static void deleteUnusedFiles(List<LogFile> logs,File index) throws IOException {
		Set<String> used = new HashSet<String>();
		used.add( index.getName() );
		for( LogFile lf : logs ) {
			used.add( lf.file.getName() );
		}
		for( File f : index.getParentFile().listFiles() ) {
			if( !used.contains(f.getName()) ) {
				IoUtils.deleteRecursively(f);
			}
		}
	}

	private void writeIndex() throws IOException {
		writeIndex(logs,index);
	}

	public static void writeIndex(List<LogFile> logs,File index) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(version);
		dos.writeInt(logs.size());
		for( LogFile lf : logs ) {
			String fileName = lf.file.getName();
			dos.writeUTF(fileName);
		}
		dos.close();
		RandomAccessFile raf = new RandomAccessFile( index, "rwd" );
		raf.write( baos.toByteArray() );
		raf.close();
		deleteUnusedFiles(logs,index);
		//logger.info("writeIndex "+logs.toString());
	}

	private void mergeLogs() throws IOException {
		//logger.info("merge");
		LogFile first = logs.get(0);
		LogFile second = logs.get(1);
		long lastTime = second.file.lastModified();
		File dirFile = new File(logDir,"merge");
		if( dirFile.exists() )
			throw new RuntimeException();
		Directory dir = FSDirectory.open(dirFile);
		LuceneIndexWriter mergeWriter = new LuceneIndexWriter( indexWriter.luceneVersion, dir, indexWriter.goodConfig );
		playLog( first.input(), mergeWriter );
		playLog( second.input(), mergeWriter );
		mergeWriter.commit();
		LogFile merge = newLogFile();
		logLucene( lastTime, merge, mergeWriter );
		mergeWriter.close();
		synchronized(this) {
			//check();
			logs.remove(0);
			logs.set(0,merge);
			writeIndex();
			//check(null);
		}
	}
	private final Runnable mergeLogs = new Runnable() { public void run() {
		try {
			mergeLogs();
		} catch(IOException e) {
			throw new RuntimeException(e);
		} finally {
			isNotMerging();
		}
	} };

	private static class DocIter {
		final IndexReader reader;
		final TopDocs td;
		final int n;
		int i = 0;

		DocIter(IndexReader reader,Query query,Sort sort) throws IOException {
			this.reader = reader;
			IndexSearcher searcher = new IndexSearcher(reader);
			this.td = searcher.search(query,10000000,sort);
			this.n = td.scoreDocs.length;
			if( td.totalHits != n )
				throw new RuntimeException();
		}

		Document next() throws IOException {
			return i < n ? reader.document(td.scoreDocs[i++].doc) : null;
		}
	}

	private volatile boolean isChecking = false;

	public void check(SortField sortField) throws IOException {
		if( isChecking )
			throw new RuntimeException("another check is running");
		isChecking = true;
		try {
			doCheck(sortField);
		} finally {
			isChecking = false;
		}
	}

	protected void doCheck(SortField sortField) throws IOException {
		IndexReader indexReader;
		List<LogInputStream> logReaders;
		synchronized(this) {
			indexReader = indexWriter.openReader();
			logReaders = logReaders(logs);
		}
		try {
			logger.info("check start");
			indexWriter.check();
			File dirFile = new File(logDir,"check");
			IoUtils.deleteRecursively(dirFile);
			Directory dir = FSDirectory.open(dirFile);
			LuceneIndexWriter checkWriter = new LuceneIndexWriter( indexWriter.luceneVersion, dir, indexWriter.goodConfig );
			playLogs(logReaders,checkWriter);
			logger.info("check lucene");
			IndexReader checkReader = checkWriter.openReader();
			if( sortField == null ) {
				int nCheck = checkReader.numDocs();
				int nOrig = indexReader.numDocs();
				if( nCheck != nOrig ) {
					logger.error("numDocs mismatch: lucene="+nOrig+" logs="+nCheck);
				}
				logger.info("numDocs="+nOrig);
				if( hash(indexReader) != hash(checkReader) ) {
					logger.error("hash mismatch");
				}
			} else {
				Sort sort = new Sort(sortField);
				String sortFieldName = sortField.getField();
				Query query = new PrefixQuery(new Term(sortFieldName));
				DocIter origIter = new DocIter(indexReader,query,sort);
				DocIter checkIter = new DocIter(checkReader,query,sort);
				Map<String,Object> origFields = LuceneUtils.toMap(origIter.next());
				Map<String,Object> checkFields = LuceneUtils.toMap(checkIter.next());
				while( origFields!=null && checkFields!=null ) {
					Comparable origFld = (Comparable)origFields.get(sortFieldName);
					Comparable checkFld = (Comparable)checkFields.get(sortFieldName);
					int cmp = origFld.compareTo(checkFld);
					if( cmp==0 ) {
						if( !origFields.equals(checkFields) ) {
							logger.error(sortFieldName+" "+origFld+" not equal");
							logger.error("lucene = "+origFields);
							logger.error("logs = "+checkFields);
						}
						origFields = LuceneUtils.toMap(origIter.next());
						checkFields = LuceneUtils.toMap(checkIter.next());
					} else if( cmp < 0 ) {
						logger.error(sortFieldName+" "+origFld+" found in lucene but not logs");
						origFields = LuceneUtils.toMap(origIter.next());
					} else {  // >
						logger.error(sortFieldName+" "+checkFld+" found in logs but not lucene");
						checkFields = LuceneUtils.toMap(checkIter.next());
					}
				}
				while( origFields!=null ) {
					Comparable origFld = (Comparable)origFields.get(sortFieldName);
					logger.error(sortFieldName+" "+origFld+" found in lucene but not logs");
					origFields = LuceneUtils.toMap(origIter.next());
				}
				while( checkFields!=null ) {
					Comparable checkFld = (Comparable)checkFields.get(sortFieldName);
					logger.error(sortFieldName+" "+checkFld+" found in logs but not lucene");
					checkFields = LuceneUtils.toMap(checkIter.next());
				}
				//logger.info("check done");
			}
			checkReader.close();
			checkWriter.close();
			IoUtils.deleteRecursively(dirFile);
			logger.info("check done");
		} finally {
			indexReader.close();
		}
	}

	private static abstract class HashCollector extends GoodCollector {
		int total = 0;
	}

	private static int hash(IndexReader reader) throws IOException {
		final IndexSearcher searcher = new IndexSearcher(reader);
		Query query = new MatchAllDocsQuery();
		HashCollector col = new HashCollector() {
			public void collectDoc(int iDoc) throws IOException {
				Document doc = searcher.doc(iDoc);
				Map<String,Object> storedFields = LuceneUtils.toMap(doc);
				total += storedFields.hashCode();
			}
		};
		searcher.search(query,col);
		return col.total;
	}

	public synchronized void close() throws IOException {
		indexWriter.close();
		log.commit();
		log.close();
	}

	public synchronized void commit() throws IOException {
		indexWriter.commit();
		log.commit();
		if( isMerging )
			return;
		if( log.logFile.end() > logs.get(0).end() ) {
			logs.add( newLogFile() );
			writeIndex();
			setLog();
		}
		if( logs.size() > 3 ) {
			isMerging = true;
			new Thread(mergeLogs).start();
//			mergeLogs.run();
		}
	}

	public synchronized void rollback() throws IOException {
		indexWriter.rollback();
		log.rollback();
	}

	public synchronized void deleteAll() throws IOException {
		indexWriter.deleteAll();
		writeOp(OP_DELETE_ALL);
	}

	public synchronized void deleteDocuments(Query query) throws IOException {
		indexWriter.deleteDocuments(query);
		writeOp(OP_DELETE_DOCUMENTS);
		log.writeQuery(query);
	}

	public synchronized void addDocument(Map<String,Object> storedFields) throws IOException {
		indexWriter.addDocument(storedFields);
		writeOp(OP_ADD_DOCUMENT);
		log.writeMap(storedFields);
	}

	public synchronized void updateDocument(String keyFieldName,Map<String,Object> storedFields) throws IOException {
		indexWriter.updateDocument(keyFieldName,storedFields);
		writeOp(OP_UPDATE_DOCUMENT);
		log.writeUTF(keyFieldName);
		log.writeMap(storedFields);
	}

	public synchronized void reindexDocuments(String keyFieldName,Query query) throws IOException {
		indexWriter.reindexDocuments(keyFieldName,query);
	}

	private void writeOp(int op) throws IOException {
		log.writeLong(System.currentTimeMillis());
		log.writeByte(op);
	}

	public synchronized void playLogs() throws IOException {
		playLogs( logReaders(logs), indexWriter );
	}

	private static List<LogInputStream> logReaders(List<LogFile> logs) throws IOException {
		List<LogInputStream> logReaders = new ArrayList<LogInputStream>();
		for( LogFile log : logs ) {
			logReaders.add( log.input() );
		}
		return logReaders;
	}

	private static void playLogs(List<LogInputStream> logReaders,LuceneIndexWriter indexWriter) throws IOException {
		if( numDocs(indexWriter) != 0 )
			throw new RuntimeException ("not empty");
		for( LogInputStream reader : logReaders ) {
			playLog(reader,indexWriter);
		}
		indexWriter.commit();
	}

	private static int numDocs(LuceneIndexWriter indexWriter) throws IOException {
		IndexReader reader = indexWriter.openReader();
		int n = reader.numDocs();
		reader.close();
		return n;
	}

	private static void playLog(LogInputStream in,LuceneIndexWriter indexWriter) throws IOException {
		while( in.available() > 0 ) {
			playOp(in,indexWriter);
		}
		in.close();
	}

	private static void playOp(LogInputStream in,LuceneIndexWriter indexWriter) throws IOException {
		in.readLong();  // time
		int op = in.readByte();
		switch(op) {
		case OP_DELETE_ALL:
			indexWriter.deleteAll();
			return;
		case OP_DELETE_DOCUMENTS:
			indexWriter.deleteDocuments( in.readQuery() );
			return;
		case OP_ADD_DOCUMENT:
			{
				Map storedFields = in.readMap();
				indexWriter.addDocument(storedFields);
				return;
			}
		case OP_UPDATE_DOCUMENT:
			{
				String keyFieldName = in.readUTF();
				Map storedFields = in.readMap();
				indexWriter.updateDocument(keyFieldName,storedFields);
				return;
			}
		default:
			throw new RuntimeException("invalid op "+op);
		}
	}

	private static void dump(LuceneIndexWriter indexWriter) throws IOException {
		IndexReader reader = indexWriter.openReader();
		IndexSearcher searcher = new IndexSearcher(reader);
		Query query = new MatchAllDocsQuery();
		TopDocs td = searcher.search(query,100);
		System.out.println("totalHits = "+td.totalHits);
		for( int i=0; i<td.scoreDocs.length; i++ ) {
			Document doc = searcher.doc(td.scoreDocs[i].doc);
			System.out.println(LuceneUtils.toMap(doc));
		}
		System.out.println();
		reader.close();
	}
 
}
