package luan.modules;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import goodjava.io.IoUtils;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanCloner;
import luan.LuanException;


public final class PackageLuan {

	public static LuanFunction requireFn(Luan luan) {
		LuanFunction fn = (LuanFunction)luan.registry().get("Package.require");
		if( fn == null ) {
			try {
				fn = new LuanJavaFunction(luan,PackageLuan.class.getMethod("require",Luan.class,String.class),null);
			} catch(NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			luan.registry().put("Package.require",fn);
		}
		return fn;
	}

	public static LuanTable loaded(Luan luan) {
		LuanTable tbl = (LuanTable)luan.registry().get("Package.loaded");
		if( tbl == null ) {
			tbl = new LuanTable(luan);
			luan.registry().put("Package.loaded",tbl);
		}
		return tbl;
	}

	public static Object require(Luan luan,String modName) throws LuanException {
		if( "java".equals(modName) ) {
			JavaLuan.java(luan);
			return true;
		}
		Object mod = load(luan,modName);
		if( mod.equals(Boolean.FALSE) )
			throw new LuanException( "module '"+modName+"' not found" );
		return mod;
	}

	public static Object load(Luan luan,String modName) throws LuanException {
		LuanTable loaded = loaded(luan);
		Object mod = loaded.rawGet(modName);
		if( mod == null ) {
			if( modName.equals("luan:Boot.luan") ) {
				String src;
				try {
					Reader in = new InputStreamReader(ClassLoader.getSystemResourceAsStream("luan/modules/Boot.luan"));
					src = IoUtils.readAll(in);
					in.close();
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
				LuanFunction loader = luan.load(src,modName,true);
				mod = Luan.first(
					loader.call(modName)
				);
				if( mod == null )
					throw new RuntimeException();
			} else if( modName.startsWith("java:") ) {
				mod = JavaLuan.load(luan,modName.substring(5));
				if( mod == null )
					mod = Boolean.FALSE;
			} else {
				String src = read(luan,modName);
				if( src == null ) {
					mod = Boolean.FALSE;
				} else {
					LuanFunction loader = luan.load(src,modName,true);
					mod = Luan.first(
						loader.call(modName)
					);
					if( mod == null ) {
						mod = loaded.rawGet(modName);
						if( mod != null )
							return mod;
						throw new LuanException( "module '"+modName+"' returned nil" );
					}
				}
			}
			loaded.rawPut(modName,mod);
		}
		return mod;
	}

	static String read(Luan luan,String uri) {
		LuanTable boot;
		try {
			boot = (LuanTable)luan.require("luan:Boot.luan");
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
		Luan.Security security = Luan.setSecurity(luan,null);
		try {
			return (String)Luan.first(boot.fn("read").call(uri));
		} catch(LuanException e) {
			return null;
		} finally {
			if( security != null )
				Luan.setSecurity(luan,security);
		}
	}

}
