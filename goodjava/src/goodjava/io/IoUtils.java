package goodjava.io;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Security;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public final class IoUtils {
	private static final Logger logger = LoggerFactory.getLogger(IoUtils.class);

	private IoUtils() {}  // never

	public static void move( File from, File to ) throws IOException {
		Files.move( from.toPath(), to.toPath() );
	}

	public static void delete(File file) throws IOException {
		Files.deleteIfExists( file.toPath() );
	}

	public static void mkdirs(File file) throws IOException {
		Files.createDirectories( file.toPath() );
	}

	public static boolean isSymbolicLink(File file) {
		return Files.isSymbolicLink(file.toPath());
	}

	public static void deleteRecursively(File file) throws IOException {
		if( file.isDirectory() && !isSymbolicLink(file) ) {
			for( File f : file.listFiles() ) {
				deleteRecursively(f);
			}
		}
		delete(file);
	}

	public static void link(File existing,File link) throws IOException {
		Files.createLink( link.toPath(), existing.toPath() );
	}

	public static void symlink(File existing,File link) throws IOException {
		Files.createSymbolicLink( link.toPath(), existing.toPath() );
	}

	public static void copyAll(InputStream in,OutputStream out)
		throws IOException
	{
		byte[] a = new byte[8192];
		int n;
		while( (n=in.read(a)) != -1 ) {
			out.write(a,0,n);
		}
		in.close();
	}

	public static void copyAll(Reader in,Writer out)
		throws IOException
	{
		char[] a = new char[8192];
		int n;
		while( (n=in.read(a)) != -1 ) {
			out.write(a,0,n);
		}
		in.close();
	}

	public static String readAll(Reader in)
		throws IOException
	{
		StringWriter sw = new StringWriter();
		copyAll(in,sw);
		return sw.toString();
	}

	public static long checksum(InputStream in) throws IOException {
		long cs = 0;
		int c;
		while( (c=in.read()) != -1 ) {
			cs = 31 * cs + c;
		}
		in.close();
		return cs;
	}



	public static class ProcException extends IOException {
		private ProcException(String msg) {
			super(msg);
		}
	}

	public static void waitFor(Process proc)
		throws IOException, ProcException
	{
		try {
			proc.waitFor();
		} catch(InterruptedException e) {
			throw new RuntimeException(e);
		}
		int exitVal = proc.exitValue();
		if( exitVal != 0 ) {
			StringWriter sw = new StringWriter();
			copyAll( new InputStreamReader(proc.getInputStream()), sw );
			copyAll( new InputStreamReader(proc.getErrorStream()), sw );
			String error = sw.toString();
			throw new ProcException(error);
		}
	}


	static {
		// undo restrictions of modern scum
		Security.setProperty("jdk.tls.disabledAlgorithms","SSLv3, RC4, DES, MD5withRSA, DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC");
	}

	public static SSLSocketFactory getSSLSocketFactory() {
		return (SSLSocketFactory)SSLSocketFactory.getDefault();
	}

	public static SSLServerSocketFactory getSSLServerSocketFactory() {
		return (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
	}

}
