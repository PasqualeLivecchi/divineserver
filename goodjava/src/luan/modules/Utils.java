package luan.modules;

import java.io.Reader;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Map;
import goodjava.io.IoUtils;
import luan.Luan;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanFunction;


public final class Utils {
	private Utils() {}  // never

	static final int bufSize = 8192;

	private static void checkNotNull(Object v,String expected,int pos) throws LuanException {
		if( v == null )
			throw new LuanException("bad argument #"+pos+" ("+expected+" expected, got nil)");
	}

	public static void checkNotNull(String s,int pos) throws LuanException {
		checkNotNull(s,"string",pos);
	}

	public static void checkNotNull(String s) throws LuanException {
		checkNotNull(s,1);
	}

	public static void checkNotNull(byte[] b,int pos) throws LuanException {
		checkNotNull(b,"binary",pos);
	}

	public static void checkNotNull(byte[] b) throws LuanException {
		checkNotNull(b,1);
	}

	public static void checkNotNull(LuanTable t,int pos) throws LuanException {
		checkNotNull(t,"table",pos);
	}

	public static void checkNotNull(LuanTable t) throws LuanException {
		checkNotNull(t,1);
	}

	public static void checkNotNull(Number n,int pos) throws LuanException {
		checkNotNull(n,"number",pos);
	}

	public static void checkNotNull(Number n) throws LuanException {
		checkNotNull(n,1);
	}

	public static void checkNotNull(LuanFunction fn,int pos) throws LuanException {
		checkNotNull(fn,"function",pos);
	}

	public static void checkNotNull(LuanFunction fn) throws LuanException {
		checkNotNull(fn,1);
	}

	public static byte[] readAll(InputStream in)
		throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IoUtils.copyAll(in,out);
		return out.toByteArray();
	}


	public static String removeString(LuanTable options,String key) throws LuanException {
		Object val = options.remove(key);
		if( val!=null && !(val instanceof String) )
			throw new LuanException( "parameter '"+key+"' must be a string but is a "+Luan.type(val) );
		return (String)val;
	}

	public static String removeRequiredString(LuanTable options,String key) throws LuanException {
		String s = removeString(options,key);
		if( s==null )
			throw new LuanException( "parameter '"+key+"' is required" );
		return s;
	}

	public static Number removeNumber(LuanTable options,String key) throws LuanException {
		Object val = options.remove(key);
		if( val!=null && !(val instanceof Number) )
			throw new LuanException( "parameter '"+key+"' must be a string but is a "+Luan.type(val) );
		return (Number)val;
	}

	public static Integer removeInteger(LuanTable options,String key) throws LuanException {
		Object val = options.remove(key);
		if( val==null )
			return null;
		Integer i = Luan.asInteger(val);
		if( i==null ) {
			String type = val instanceof Number ? val.getClass().getSimpleName().toLowerCase() : Luan.type(val);
			throw new LuanException( "parameter '"+key+"' must be an integer but is a "+type );
		}
		return i;
	}

	public static LuanTable removeTable(LuanTable options,String key) throws LuanException {
		Object val = options.remove(key);
		if( val!=null && !(val instanceof LuanTable) )
			throw new LuanException( "parameter '"+key+"' must be a table but is a "+Luan.type(val) );
		return (LuanTable)val;
	}

	public static Boolean removeBoolean(LuanTable options,String key) throws LuanException {
		Object val = options.remove(key);
		if( val!=null && !(val instanceof Boolean) )
			throw new LuanException( "parameter '"+key+"' must be a string but is a "+Luan.type(val) );
		return (Boolean)val;
	}

	public static LuanFunction removeFunction(LuanTable options,String key) throws LuanException {
		Object val = options.remove(key);
		if( val!=null && !(val instanceof LuanFunction) )
			throw new LuanException( "parameter '"+key+"' must be a function but is a "+Luan.type(val) );
		return (LuanFunction)val;
	}

	public static LuanFunction removeRequiredFunction(LuanTable options,String key) throws LuanException {
		LuanFunction fn = removeFunction(options,key);
		if( fn==null )
			throw new LuanException( "parameter '"+key+"' is required" );
		return fn;
	}

	public static void checkEmpty(LuanTable options) throws LuanException {
		if( !options.isEmpty() )
			throw new LuanException( "unrecognized options: "+options.asMap() );
	}

}
