package goodjava.lucene.backup;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.Writer;
import java.io.FileWriter;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import goodjava.util.SoftCacheMap;
import goodjava.io.IoUtils;
import goodjava.rpc.RpcServer;
import goodjava.rpc.RpcCall;
import goodjava.rpc.RpcClient;
import goodjava.rpc.RpcResult;
import goodjava.rpc.Rpc;
import goodjava.rpc.RpcException;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public class BackupServer {
	private static final Logger logger = LoggerFactory.getLogger(BackupServer.class);

	public static int port = 9101;
	public static String[] cipherSuites = new String[] {
		"TLS_DH_anon_WITH_AES_128_GCM_SHA256",
		"TLS_DH_anon_WITH_AES_128_CBC_SHA256",
		"TLS_ECDH_anon_WITH_AES_128_CBC_SHA",
		"TLS_DH_anon_WITH_AES_128_CBC_SHA",
		"TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
		"SSL_DH_anon_WITH_3DES_EDE_CBC_SHA",
		"TLS_ECDH_anon_WITH_RC4_128_SHA",
		"SSL_DH_anon_WITH_RC4_128_MD5",
		"SSL_DH_anon_WITH_DES_CBC_SHA",
		"SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA",
		"SSL_DH_anon_EXPORT_WITH_RC4_40_MD5",
	};

	private final File backupDir;
	private static final ExecutorService threadPool = Executors.newCachedThreadPool();
	private static final Map<String,Backup> backups = new SoftCacheMap<String,Backup>();

	public BackupServer(File backupDir) throws IOException {
		this.backupDir = backupDir;
		IoUtils.mkdirs(backupDir);
	}

	public synchronized void start() throws IOException {
		final ServerSocket ss;
		if( cipherSuites == null ) {
			ss = new ServerSocket(port);
		} else {
			ss = IoUtils.getSSLServerSocketFactory().createServerSocket(port);
			((SSLServerSocket)ss).setEnabledCipherSuites(cipherSuites);
		}
		threadPool.execute(new Runnable(){public void run() {
			try {
				while(!threadPool.isShutdown()) {
					final Socket socket = ss.accept();
					threadPool.execute(new Runnable(){public void run() {
						handle(socket);
					}});
				}
			} catch(IOException e) {
				logger.error("",e);
			}
		}});
		logger.info("started server on port "+port);
	}

	private void handle(Socket socket) {
		RpcServer rpc = new RpcServer(socket);
		Backup backup;
		{
			RpcCall call = rpc.read();
			if( !call.cmd.equals("login") ) {
				rpc.write( new RpcException("login expected") );
				rpc.close();
				return;
			}
			String name = (String)call.args[0];
			String password = (String)call.args[1];
			synchronized(backups) {
				backup = backups.get(name);
				if( backup == null ) {
					backup = new Backup(new File(backupDir,name));
					backups.put(name,backup);
				}
			}
			File pwd = new File(backupDir,name+".pwd");
			try {
				if( !pwd.exists() ) {
					Writer out = new FileWriter(pwd);
					out.write(password);
					out.close();
				} else {
					Reader in = new FileReader(pwd);
					if( !IoUtils.readAll(in).equals(password) ) {
						rpc.write( new RpcException("wrong password") );
						rpc.close();
						return;
					}
				}
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			rpc.write(Rpc.OK);
		}
		while( !rpc.isClosed() ) {
			RpcCall call = rpc.read();
			if( call == null )
				break;
			backup.handle(rpc,call);
		}
	}


	// for client

	public static RpcClient rpcClient(String backupDomain) throws IOException {
		Socket socket;
		if( BackupServer.cipherSuites == null ) {
			socket = new Socket(backupDomain,BackupServer.port);
		} else {
			socket = IoUtils.getSSLSocketFactory().createSocket(backupDomain,BackupServer.port);
			((SSLSocket)socket).setEnabledCipherSuites(BackupServer.cipherSuites);
		}
		return new RpcClient(socket);
	}

	public static void getBackup(String backupDomain,String name,File zip) throws IOException, RpcException {
		RpcClient rpc = BackupServer.rpcClient(backupDomain);
		RpcCall call = new RpcCall("zip",name);
		rpc.write(call);
		RpcResult result = rpc.read();
		OutputStream out = new BufferedOutputStream(new FileOutputStream(zip));
		IoUtils.copyAll(result.in,out);
		out.close();
		rpc.close();
	}

}
