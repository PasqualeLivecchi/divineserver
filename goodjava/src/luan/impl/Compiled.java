package luan.impl;

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.JavaFileManager;
import javax.tools.StandardJavaFileManager;
import javax.tools.ForwardingJavaFileManager;
import goodjava.io.BufferedInputStream;
import goodjava.io.DataInputStream;
import goodjava.io.DataOutputStream;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


final class Compiled {
	private static final Logger logger = LoggerFactory.getLogger(Compiled.class);

	private static class MyJavaFileObject extends SimpleJavaFileObject {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		MyJavaFileObject() {
			super(URI.create("whatever"),JavaFileObject.Kind.CLASS);
		}

		@Override public OutputStream openOutputStream() {
			return baos;
		}

		byte[] byteCode(String sourceName) {
			byte[] byteCode = baos.toByteArray();
			final int len = sourceName.length();
			int max = byteCode.length-len-3;
			outer:
			for( int i=0; true; i++ ) {
				if( i > max )
					throw new RuntimeException("len="+len);
				if( byteCode[i]==1 && (byteCode[i+1] << 8 | 0xFF & byteCode[i+2]) == len ) {
					for( int j=i+3; j<i+3+len; j++ ) {
						if( byteCode[j] != '$' )
							continue outer;
					}
					System.arraycopy(sourceName.getBytes(),0,byteCode,i+3,len);
					break;
				}
			}
			return byteCode;
		}
	}

	static Compiled compile(final String className,final String sourceName,final String code) {
		final int len = sourceName.length();
		StringBuilder sb = new StringBuilder(sourceName);
		for( int i=0; i<len; i++ )
			sb.setCharAt(i,'$');
		JavaFileObject sourceFile = new SimpleJavaFileObject(URI.create(sb.toString()),JavaFileObject.Kind.SOURCE) {
			@Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
				return code;
			}
			@Override public String getName() {
				return sourceName;
			}
			@Override public boolean isNameCompatible(String simpleName,JavaFileObject.Kind kind) {
				return true;
			}
		};
		final Map<String,MyJavaFileObject> map = new HashMap<String,MyJavaFileObject>();
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager sjfm = compiler.getStandardFileManager(null,null,null);
		ForwardingJavaFileManager fjfm = new ForwardingJavaFileManager(sjfm) {
			@Override public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
				if( map.containsKey(className) )
					throw new RuntimeException(className);
				MyJavaFileObject classFile = new MyJavaFileObject();
				map.put(className,classFile);
				return classFile;
			}
		};
		StringWriter out = new StringWriter();
		boolean b = compiler.getTask(out, fjfm, null, null, null, Collections.singletonList(sourceFile)).call();
		if( !b )
			throw new RuntimeException("\n"+out+"\ncode:\n"+code+"\n");
		Map<String,byte[]> map2 = new HashMap<String,byte[]>();
		for( Map.Entry<String,MyJavaFileObject> entry : map.entrySet() ) {
			map2.put( entry.getKey(), entry.getValue().byteCode(sourceName) );
		}
		return new Compiled(className,map2);
	}


	private final String className;
	private final Map<String,byte[]> map;
	private final Set<String> set = new HashSet<String>();

	private Compiled(String className,Map<String,byte[]> map) {
		this.className = className;
		this.map = map;
	}

	Class loadClass() {
		try {
			ClassLoader cl = new ClassLoader() {
				@Override protected Class<?> findClass(String name) throws ClassNotFoundException {
					if( !set.add(name) )
						logger.error("dup "+name);
					byte[] byteCode = map.get(name);
					if( byteCode != null ) {
						return defineClass(name, byteCode, 0, byteCode.length);
					}
					return super.findClass(name);
				}
			};
			return cl.loadClass(className);
		} catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}


	private static final int VERSION = 3;
	private static final File tmpDir;
	static {
		File f = new File(System.getProperty("java.io.tmpdir"));
		tmpDir = new File(f,"luan");
		tmpDir.mkdir();
		if( !tmpDir.exists() )
			throw new RuntimeException();
	}

	static Compiled load(String fileName,String key) {
		try {
			File f = new File(tmpDir,fileName);
			if( !f.exists() )
				return null;
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
			if( in.readInt() != VERSION )
				return null;
			if( !in.readString().equals(key) )
				return null;
			String className = in.readUTF();
			int n = in.readInt();
			Map<String,byte[]> map = new HashMap<String,byte[]>();
			for( int i=0; i<n; i++ ) {
				String s = in.readUTF();
				int len = in.readInt();
				byte[] a = new byte[len];
				in.readFully(a);
				map.put(s,a);
			}
			in.close();
			return new Compiled(className,map);
		} catch(IOException e) {
			logger.error("load failed",e);
			return null;
		}
	}

	void save(String fileName,String key) {
		try {
			File f = new File(tmpDir,fileName);
			DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
			out.writeInt(VERSION);
			out.writeString(key);
			out.writeUTF(className);
			out.writeInt(map.size());
			for( Map.Entry<String,byte[]> entry : map.entrySet() ) {
				out.writeUTF( entry.getKey() );
				byte[] a = entry.getValue();
				out.writeInt(a.length);
				out.write(a);
			}
			out.close();
		} catch(IOException e) {
			logger.error("save failed",e);
		}
	}
}
