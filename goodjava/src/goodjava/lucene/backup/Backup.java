package goodjava.lucene.backup;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import goodjava.io.IoUtils;
import goodjava.io.BufferedInputStream;
import goodjava.rpc.RpcServer;
import goodjava.rpc.RpcCall;
import goodjava.rpc.RpcResult;
import goodjava.rpc.RpcException;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.lucene.logging.LogFile;
import goodjava.lucene.logging.LoggingIndexWriter;
import goodjava.lucene.logging.LogOutputStream;


class Backup {
	private static final Logger logger = LoggerFactory.getLogger(Backup.class);

	private final File dir;
	private final File index;

	Backup(File dir) {
		this.dir = dir;
		this.index = new File(dir,"index");
	}

	void handle(RpcServer rpc,RpcCall call) {
		try {
			IoUtils.mkdirs(dir);
			if( call.cmd.equals("zip") ) {
				handleZip(rpc);
			} else {
				handle2(rpc,call);
			}
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final RpcResult OK = new RpcResult(new Object[]{"ok"});

	synchronized void handle2(RpcServer rpc,RpcCall call) throws IOException {
		//logger.info(call.cmd+" "+Arrays.asList(call.args));
		String fileName = null;
		if( call.cmd.equals("check") ) {
			// nothing
		} else if( call.cmd.equals("add") || call.cmd.equals("append")  ) {
			fileName = (String)call.args[1];
			File f = new File(dir,fileName);
			if( call.cmd.equals("add") )
				IoUtils.delete(f);
			LogFile log = new LogFile(f);
			LogOutputStream out = log.output();
			IoUtils.copyAll(call.in,out);
			out.commit();
			out.close();
			//logger.info(call.cmd+" "+fileName+" "+call.lenIn);
		} else {
			logger.error("bad cmd '"+call.cmd+"'");
			rpc.write( new RpcException("bad cmd '"+call.cmd+"'") );
			return;
		}
		List logInfo = (List)call.args[0];
		//logger.info("check "+logInfo);
		RpcResult result = OK;
		for( Object obj : logInfo ) {
			Map fileInfo = (Map)obj;
			String name = (String)fileInfo.get("name");
			File f = new File(dir,name);
			if( !f.exists() ) {
				if( name.equals(fileName) )  logger.error("missing");
				result = new RpcResult(new Object[]{"missing",name});
				break;
			}
			long end = (Long)fileInfo.get("end");
			LogFile log = new LogFile(f);
			long logEnd = log.end();
			if( logEnd > end ) {
				logger.error("logEnd > end - shouldn't happen, file="+name+" logEnd="+logEnd+" end="+end);
				result = new RpcResult(new Object[]{"missing",name});
				break;
			}
			if( logEnd < end ) {
				if( name.equals(fileName) )  logger.error("incomplete");
				result = new RpcResult(new Object[]{"incomplete",name,logEnd});
				break;
			}
			Object checksumObj = fileInfo.get("checksum");
			if( checksumObj != null ) {
				long checksum = (Long)checksumObj;
				if( log.checksum() != checksum ) {
					index.delete();
					result = new RpcResult(new Object[]{"bad_checksum",name});
					break;
				}
			}
		}
		if( call.cmd.equals("add") ) {
			boolean complete = true;
			List<LogFile> logs = new ArrayList<LogFile>();
			for( Object obj : logInfo ) {
				Map fileInfo = (Map)obj;
				String name = (String)fileInfo.get("name");
				File f = new File(dir,name);
				if( !f.exists() ) {
					complete = false;
					break;
				}
				logs.add( new LogFile(f) );
			}
			if( complete ) {
				LoggingIndexWriter.writeIndex(logs,index);
				//logger.info("write index");
			}
		}
		rpc.write(result);
	}

	void handleZip(RpcServer rpc) throws IOException {
		File zip = File.createTempFile("luan_",".zip");
		IoUtils.delete(zip);
		String cmd = "zip -r " + zip + " " + dir.getName();
		synchronized(this) {
			Process proc = Runtime.getRuntime().exec(cmd,null,dir.getParentFile());
			IoUtils.waitFor(proc);
		}
		InputStream in = new BufferedInputStream(new FileInputStream(zip));
		RpcResult result = new RpcResult(in,zip.length(),new Object[0]);
		rpc.write(result);
		IoUtils.delete(zip);
	}

}
