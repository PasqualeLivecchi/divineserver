package goodjava.lucene.backup;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.apache.lucene.search.SortField;
import goodjava.io.IoUtils;
import goodjava.rpc.RpcClient;
import goodjava.rpc.RpcCall;
import goodjava.rpc.RpcResult;
import goodjava.rpc.RpcException;
import goodjava.lucene.api.LuceneIndexWriter;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.lucene.logging.LoggingIndexWriter;
import goodjava.lucene.logging.LogFile;


public class BackupIndexWriter extends LoggingIndexWriter {
	private static final Logger logger = LoggerFactory.getLogger(BackupIndexWriter.class);
	public static String[] backupDomains;
	private final String name;
	private final String password;
	private final File dir;
	private boolean isSyncPending = false;
	private final ExecutorService exec = Executors.newSingleThreadExecutor();

	public BackupIndexWriter(LuceneIndexWriter indexWriter,File logDir,String name,String password) throws IOException {
		super(indexWriter,logDir);
		if( backupDomains == null )
			throw new RuntimeException("must set backupDomains");
		this.name = name;
		this.password = password;
		File f = new File(System.getProperty("java.io.tmpdir"));
		dir = new File(f,"goodjava.lucene/"+name);
		IoUtils.mkdirs(dir);
	}

	public synchronized void close() throws IOException {
		super.close();
		exec.shutdown();
	}

	public synchronized void commit() throws IOException {
		super.commit();
		//sync();
		if( !isSyncPending ) {
			exec.execute(sync);
			isSyncPending = true;
		}
	}

	protected void doCheck(SortField sortField) throws IOException {
		super.doCheck(sortField);
		runSyncWithChecksum();
	}

	public void runSync() {
		try {
			exec.submit(sync).get();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void runSyncWithChecksum() {
		try {
			exec.submit(syncWithChecksum).get();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private final Runnable sync = new Runnable() {
		public void run() {
			try {
				sync(false);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private final Runnable syncWithChecksum = new Runnable() {
		public void run() {
			try {
				sync(true);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private void sync(boolean withChecksum) throws IOException {
		List<LogFile> logs = new ArrayList<LogFile>();
		synchronized(this) {
			isSyncPending = false;
			clearDir();
			for( LogFile log : this.logs ) {
				File f = new File(dir,log.file.getName());
				IoUtils.link(log.file,f);
				logs.add( new LogFile(f) );
			}
		}
		List logInfo = new ArrayList();
		Map<String,LogFile> logMap = new HashMap<String,LogFile>();
		for( LogFile log : logs ) {
			Map fileInfo = new HashMap();
			fileInfo.put("name",log.file.getName());
			fileInfo.put("end",log.end());
			if( withChecksum )
				fileInfo.put("checksum",log.checksum());
			logInfo.add(fileInfo);
			logMap.put(log.file.getName(),log);
		}
		for( String backupDomain : backupDomains ) {
			RpcClient rpc = BackupServer.rpcClient(backupDomain);
			try {
				RpcCall call = new RpcCall("login",name,password);
				rpc.write(call);
				rpc.read();
				call = new RpcCall("check",logInfo);
				while(true) {
					rpc.write(call);
					RpcResult result = rpc.read();
					//logger.info(Arrays.asList(result.returnValues).toString());
					String status = (String)result.returnValues[0];
					if( status.equals("ok") ) {
						break;
					} else if( status.equals("missing") || status.equals("bad_checksum") ) {
						String fileName = (String)result.returnValues[1];
						if( status.equals("bad_checksum") )
							logger.error("bad_checksum "+fileName);
						LogFile log = logMap.get(fileName);
						long len = log.end() - 8;
						InputStream in = log.input();
						call = new RpcCall(in,len,"add",logInfo,fileName);
					} else if( status.equals("incomplete") ) {
						String fileName = (String)result.returnValues[1];
						long logEnd = (Long)result.returnValues[2];
						LogFile log = logMap.get(fileName);
						long len = log.end() - logEnd;
						InputStream in = log.input();
						in.skip(logEnd-8);
						call = new RpcCall(in,len,"append",logInfo,fileName);
					} else
						throw new RuntimeException("status "+status);
				}
			} catch(RpcException e) {
				logger.warn("",e);
			}
			rpc.close();
		}
		clearDir();
	}

	private void clearDir() throws IOException {
		for( File f : dir.listFiles() ) {
			IoUtils.delete(f);
		}
	}

}
