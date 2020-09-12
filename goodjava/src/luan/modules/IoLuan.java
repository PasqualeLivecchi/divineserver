package luan.modules;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.io.StringReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import goodjava.io.IoUtils;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.modules.url.LuanUrl;


public final class IoLuan {

	public static String read_console_line(String prompt) throws IOException {
		if( prompt==null )
			prompt = "> ";
		return System.console().readLine(prompt);
	}


	public interface LuanWriter {
		public Object out();
		public void write(Object... args) throws LuanException, IOException;
		public void close() throws IOException;
	}

	public static LuanWriter luanWriter(final PrintStream out) {
		return new LuanWriter() {

			public Object out() {
				return out;
			}

			public void write(Object... args) throws LuanException {
				for( Object obj : args ) {
					out.print( Luan.luanToString(obj) );
				}
			}

			public void close() {
				out.close();
			}
		};
	}

	public static LuanWriter luanWriter(final Writer out) {
		return new LuanWriter() {

			public Object out() {
				return out;
			}

			public void write(Object... args) throws LuanException, IOException {
				for( Object obj : args ) {
					out.write( Luan.luanToString(obj) );
				}
			}

			public void close() throws IOException {
				out.close();
			}
		};
	}

	static LuanFunction lines(final BufferedReader in) {
		return new LuanFunction(false) {
			@Override public Object call(Object[] args) throws LuanException {
				try {
					if( args.length > 0 ) {
						if( args.length > 1 || !"close".equals(args[0]) )
							throw new LuanException( "the only argument allowed is 'close'" );
						in.close();
						return null;
					}
					String rtn = in.readLine();
					if( rtn==null )
						in.close();
					return rtn;
				} catch(IOException e) {
					throw new LuanException(e);
				}
			}
		};
	}

	static LuanFunction blocks(final InputStream in,final int blockSize) {
		return new LuanFunction(false) {
			final byte[] a = new byte[blockSize];

			@Override public Object call(Object[] args) throws LuanException {
				try {
					if( args.length > 0 ) {
						if( args.length > 1 || !"close".equals(args[0]) )
							throw new LuanException( "the only argument allowed is 'close'" );
						in.close();
						return null;
					}
					if( in.read(a) == -1 ) {
						in.close();
						return null;
					}
					return a;
				} catch(IOException e) {
					throw new LuanException(e);
				}
			}
		};
	}


	private static File objToFile(Luan luan,Object obj) throws LuanException {
		if( obj instanceof String ) {
			String fileName = (String)obj;
			check(luan,"file:"+fileName);
			return new File(fileName);
		}
		if( obj instanceof LuanTable ) {
			LuanTable t = (LuanTable)obj;
			Object java = t.rawGet("java");
			if( java instanceof LuanFile ) {
				LuanFile luanFile = (LuanFile)java;
				return luanFile.file;
			}
		}
		return null;
	}


	public static abstract class LuanIn {
		protected String charset = null;

		public abstract InputStream inputStream() throws IOException, LuanException;
		public abstract String to_string();
		public abstract String to_uri_string();

		public Reader reader() throws IOException, LuanException {
			InputStream in = inputStream();
			return charset==null ? new InputStreamReader(in) : new InputStreamReader(in,charset);
		}

		public String read_text() throws IOException, LuanException {
			Reader in = reader();
			String s = IoUtils.readAll(in);
			return s;
		}

		public byte[] read_binary() throws IOException, LuanException {
			InputStream in = inputStream();
			byte[] a = Utils.readAll(in);
			return a;
		}

		public LuanFunction read_lines() throws IOException, LuanException {
			return lines(new BufferedReader(reader()));
		}

		public LuanFunction read_blocks(Integer blockSize) throws IOException, LuanException {
			int n = blockSize!=null ? blockSize : Utils.bufSize;
			return blocks(inputStream(),n);
		}

		public boolean exists() throws IOException, LuanException {
			try {
				inputStream().close();
				return true;
			} catch(FileNotFoundException e) {
				return false;
			} catch(UnknownHostException e) {
				return false;
			} catch(LuanException e) {
				if( e.getCause() instanceof FileNotFoundException )
					return false;
				throw e;
			}
		}

		public long checksum() throws IOException, LuanException {
			return IoUtils.checksum( new BufferedInputStream(inputStream()) );
		}

		public String charset() {
			return charset;
		}

		public void set_charset(String charset) {
			this.charset = charset;
		}
	}

	public static final LuanIn defaultStdin = new LuanIn() {

		@Override public InputStream inputStream() {
			return System.in;
		}

		@Override public String to_string() {
			return "<stdin>";
		}

		@Override public String to_uri_string() {
			return "stdin:";
		}

		@Override public String read_text() throws IOException {
			return IoUtils.readAll(new InputStreamReader(System.in));
		}

		@Override public byte[] read_binary() throws IOException {
			return Utils.readAll(System.in);
		}

		@Override public boolean exists() {
			return true;
		}
	};

	public static abstract class LuanIO extends LuanIn {
		abstract OutputStream outputStream() throws IOException;

		private Writer writer() throws IOException {
			OutputStream out = outputStream();
			return charset==null ? new OutputStreamWriter(out) : new OutputStreamWriter(out,charset);
		}

		public void write(Object obj) throws LuanException, IOException {
			if( obj instanceof String ) {
				String s = (String)obj;
				Writer out = writer();
				out.write(s);
				out.close();
				return;
			}
			if( obj instanceof byte[] ) {
				byte[] a = (byte[])obj;
				OutputStream out = outputStream();
				out.write(a);
				out.close();
				return;
			}
			if( obj instanceof LuanTable ) {
				LuanTable t = (LuanTable)obj;
				Object java = t.rawGet("java");
				if( java instanceof LuanIn ) {
					LuanIn luanIn = (LuanIn)java;
					InputStream in = luanIn.inputStream();
					OutputStream out = outputStream();
					IoUtils.copyAll(in,out);
					out.close();
					return;
				}
			}
			throw new LuanException( "bad argument #1 to 'write' (string or binary or Io.uri expected)" );
		}

		public LuanWriter text_writer() throws IOException {
			return luanWriter(new BufferedWriter(writer()));
		}

		public OutputStream binary_writer() throws IOException {
			return new BufferedOutputStream(outputStream());
		}

		public void write_text(Object... args) throws LuanException, IOException {
			LuanWriter luanWriter = text_writer();
			luanWriter.write(args);
			luanWriter.close();
		}

		public void write_binary(byte[] bytes) throws LuanException, IOException {
			OutputStream out = binary_writer();
			out.write(bytes);
			out.close();
		}
	}

	public static final LuanIO nullIO = new LuanIO() {
		private final InputStream in = new InputStream() {
			@Override public int read() {
				return -1;
			}
		};
		private final OutputStream out = new OutputStream() {
			@Override public void write(int b) {}
		};

		@Override public InputStream inputStream() {
			return in;
		}

		@Override OutputStream outputStream() {
			return out;
		}

		@Override public String to_string() {
			return "<null>";
		}

		@Override public String to_uri_string() {
			return "null:";
		}

	};

	public static final class LuanString extends LuanIO {
		private String s;

		public LuanString(String s) throws LuanException {
			Utils.checkNotNull(s);
			this.s = s;
		}

		@Override public InputStream inputStream() {
			throw new UnsupportedOperationException();
		}

		@Override OutputStream outputStream() {
			throw new UnsupportedOperationException();
		}

		@Override public String to_string() {
			return "<string>";
		}

		@Override public String to_uri_string() {
			return "string:" + s;
		}

		@Override public Reader reader() {
			return new StringReader(s);
		}

		@Override public String read_text() {
			return s;
		}

		@Override public boolean exists() {
			return true;
		}

		@Override public LuanWriter text_writer() {
			return new LuanWriter() {
				private final Writer out = new StringWriter();

				public Object out() {
					return out;
				}
	
				public void write(Object... args) throws LuanException, IOException {
					for( Object obj : args ) {
						out.write( Luan.luanToString(obj) );
					}
				}
	
				public void close() throws IOException {
					s = out.toString();
				}
			};
		}
	}

	public static final class LuanFile extends LuanIO {
		public final File file;

		public LuanFile(Luan luan,String path) throws LuanException {
			this(luan,new File(path));
		}

		private LuanFile(Luan luan,File file) throws LuanException {
			this(file);
			check(luan,"file:"+file.toString());
		}

		private LuanFile(File file) {
			this.file = file;
		}

		@Override public InputStream inputStream() throws IOException {
			return new FileInputStream(file);
		}

		@Override OutputStream outputStream() throws IOException {
			return new FileOutputStream(file);
		}

		@Override public String to_string() {
			return file.toString();
		}

		@Override public String to_uri_string() {
			return "file:" + file.toString();
		}

		public LuanFile child(Luan luan,String name) throws LuanException {
			return new LuanFile(luan,new File(file,name));
		}

		public LuanTable children(Luan luan) throws LuanException {
			File[] files = file.listFiles();
			if( files==null )
				return null;
			LuanTable list = new LuanTable(luan);
			for( File f : files ) {
				list.rawPut(list.rawLength()+1,new LuanFile(luan,f));
			}
			return list;
		}

		public LuanFile parent(Luan luan) throws LuanException, IOException {
			File parent = file.getParentFile();
			if( parent==null )
				parent = file.getCanonicalFile().getParentFile();
			return new LuanFile(luan,parent);
		}

		@Override public boolean exists() {
			return file.exists();
		}

		public void rename_to(Luan luan,Object destObj) throws LuanException, IOException {
			File dest = objToFile(luan,destObj);
			if( dest==null )
				throw new LuanException( "bad argument #1 to 'rename_to' (string or file table expected)" );
			IoUtils.move(file,dest);
		}

		public void link_from(Luan luan,Object linkObj) throws LuanException, IOException {
			File link = objToFile(luan,linkObj);
			if( link==null )
				throw new LuanException( "bad argument #1 to 'link_from' (string or file table expected)" );
			IoUtils.link(file,link);
		}

		public void symlink_from(Luan luan,Object linkObj) throws LuanException, IOException {
			File link = objToFile(luan,linkObj);
			if( link==null )
				throw new LuanException( "bad argument #1 to 'symlink_from' (string or file table expected)" );
			IoUtils.symlink(file,link);
		}

		public LuanFile canonical(Luan luan) throws LuanException, IOException {
			return new LuanFile(luan,file.getCanonicalFile());
		}

		public LuanFile create_temp_file(Luan luan,String prefix,String suffix) throws LuanException, IOException {
			File tmp = File.createTempFile(prefix,suffix,file);
			return new LuanFile(luan,tmp);
		}

		public void delete() throws IOException {
			IoUtils.deleteRecursively(file);
		}

		public void mkdir() throws IOException {
			IoUtils.mkdirs(file);
		}

		public void set_last_modified(long time) throws LuanException {
			if( !file.setLastModified(time) )
				throw new LuanException("couldn't set_last_modified on "+file);
		}

		public boolean is_symbolic_link() {
			return IoUtils.isSymbolicLink(file);
		}
	}

	public static LuanUrl classpath(Luan luan,String name) throws LuanException {
		if( name.contains("//") )
			return null;
		String path = name;
		check(luan,"classpath:"+path);
		URL url;
		if( !path.contains("#") ) {
			url = ClassLoader.getSystemResource(path);
		} else {
			String[] a = path.split("#");
			url = ClassLoader.getSystemResource(a[0]);
			if( url==null ) {
				for( int i=1; i<a.length; i++ ) {
					url = ClassLoader.getSystemResource(a[0]+"/"+a[i]);
					if( url != null ) {
						try {
							url = new URL(url,".");
						} catch(MalformedURLException e) {
							throw new RuntimeException(e);
						}
						break;
					}
				}
			}
		}
		if( url != null )
			return new LuanUrl(url,null);

		return null;
	}


	public static class BaseOs extends LuanIO {
		private final String cmd;
		final File dir;
		Process proc;

		private BaseOs(Luan luan,String cmd,LuanTable options) throws IOException, LuanException {
			this.cmd = cmd;
			File dir = null;
			if( options != null ) {
				Map map = options.asMap();
				Object obj = map.remove("dir");
				dir = objToFile(luan,obj);
				if( dir==null )
					throw new LuanException( "bad option 'dir' (string or file table expected)" );
				if( !map.isEmpty() )
					throw new LuanException( "unrecognized options: "+map );
			}
			this.dir = dir;
		}

		@Override public InputStream inputStream() throws IOException {
			return proc.getInputStream();
		}

		@Override OutputStream outputStream() throws IOException {
			return proc.getOutputStream();
		}

		@Override public String to_string() {
			return proc.toString();
		}

		@Override public String to_uri_string() {
			throw new UnsupportedOperationException();
		}

		@Override public boolean exists() {
			return true;
		}

		public void wait_for()
			throws IOException
		{
			IoUtils.waitFor(proc);
		}

		@Override public String read_text() throws IOException, LuanException {
			String s = super.read_text();
			wait_for();
			return s;
		}
	}

	public static final class LuanOs extends BaseOs {
		public LuanOs(Luan luan,String cmd,LuanTable options) throws IOException, LuanException {
			super(luan,cmd,options);
			check(luan,"os:"+cmd);
			this.proc = Runtime.getRuntime().exec(cmd,null,dir);
		}
	}

	public static final class LuanBash extends BaseOs {
		public LuanBash(Luan luan,String cmd,LuanTable options) throws IOException, LuanException {
			super(luan,cmd,options);
			check(luan,"bash:"+cmd);
			this.proc = Runtime.getRuntime().exec(new String[]{"bash","-c",cmd},null,dir);
		}
	}


	public static class LuanInput extends LuanIn {
		private final InputStream in;

		public LuanInput(InputStream in) {
			this.in = in;
		}

		@Override public InputStream inputStream() {
			return in;
		}

		@Override public String to_string() {
			return "<input_stream>";
		}

		@Override public String to_uri_string() {
			throw new UnsupportedOperationException();
		}

		@Override public boolean exists() {
			return true;
		}
	};


	public static String ip(String domain) {
		try {
			return InetAddress.getByName(domain).getHostAddress();
		} catch(UnknownHostException e) {
			return null;
		}
	}

	public static LuanTable my_ips(Luan luan) throws IOException, LuanException {
		LuanTable tbl = new LuanTable(luan);
		for( Enumeration<NetworkInterface> e1 = NetworkInterface.getNetworkInterfaces(); e1.hasMoreElements(); ) {
			NetworkInterface ni = e1.nextElement();
			for( Enumeration<InetAddress> e2 = ni.getInetAddresses(); e2.hasMoreElements(); ) {
				InetAddress ia = e2.nextElement();
				if( ia instanceof Inet4Address )
					tbl.put(ia.getHostAddress(),true);
			}
		}
		return tbl;
	}

	public static LuanTable dns_lookup(Luan luan,String domain,String type)
		throws NamingException
	{
		LuanTable tbl = new LuanTable(luan);
		InitialDirContext idc = new InitialDirContext();
		Attribute attribute;
		try {
			attribute = idc.getAttributes("dns:/" + domain, new String[] {type}).get(type);
		} catch(NameNotFoundException e) {
			return tbl;
		}
		if( attribute==null )
			return tbl;
		final int n = attribute.size();
		for( int i=0; i<n; i++ ) {
			Object obj = attribute.get(i);
			tbl.rawInsert(i+1,obj);
		}
		return tbl;
	}


	private static void check(Luan luan,String name) throws LuanException {
		Luan.checkSecurity(luan,"uri",name);
	}


	private void IoLuan() {}  // never
}
