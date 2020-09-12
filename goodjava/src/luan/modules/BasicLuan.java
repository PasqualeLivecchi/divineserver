package luan.modules;

import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import goodjava.json.JsonToString;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanCloner;
import luan.modules.parsers.LuanToString;


public final class BasicLuan {

	public static String type(Object obj) {
		return Luan.type(obj);
	}

	public static LuanFunction load(Luan luan,String text,String sourceName,boolean persist,LuanTable env)
		throws LuanException
	{
		Utils.checkNotNull(text);
		Utils.checkNotNull(sourceName,1);
		return luan.load(text,sourceName,persist,env);
	}
/*
	public static LuanFunction load_file(Luan luan,String fileName) throws LuanException {
		if( fileName == null ) {
			fileName = "stdin:";
		} else if( fileName.indexOf(':') == -1 ) {
			fileName = "file:" + fileName;
		}
		String src = PackageLuan.read(luan,fileName);
		if( src == null )
			return null;
		return load(luan,src,fileName,null);
	}
*/
	public static LuanFunction pairs(final LuanTable t) throws LuanException {
		Utils.checkNotNull(t);
		return t.pairs();
	}

	private static class Ipairs extends LuanFunction {
		List<Object> list;
		int i = 0;
		final int size;

		Ipairs(LuanTable t) {
			super(true);
			list = t.asList();
			size = list.size();
		}

		@Override public Object[] call(Object[] args) {
			if( i >= size )
				return LuanFunction.NOTHING;
			Object val = list.get(i++);
			return new Object[]{i,val};
		}

		@Override protected void completeClone(LuanFunction dc,LuanCloner cloner) {
			Ipairs clone = (Ipairs)dc;
			clone.list = (List)cloner.clone(list);
			super.completeClone(dc,cloner);
		}
	}

	public static LuanFunction ipairs(LuanTable t) throws LuanException {
		Utils.checkNotNull(t);
		return new Ipairs(t);
	}

	public static Object get_metatable(LuanTable table) throws LuanException {
		Utils.checkNotNull(table);
		LuanTable metatable = table.getMetatable();
		if( metatable == null )
			return null;
		Object obj = metatable.rawGet("__metatable");
		return obj!=null ? obj : metatable;
	}

	public static void set_metatable(LuanTable table,LuanTable metatable) throws LuanException {
		Utils.checkNotNull(table);
		if( table.getHandler("__metatable") != null )
			throw new LuanException("cannot change a protected metatable");
		table.setMetatable(metatable);
	}

	public static boolean raw_equal(Object v1,Object v2) {
		return v1 == v2 || v1 != null && v1.equals(v2);
	}

	public static Object raw_get(LuanTable table,Object index) {
		return table.rawGet(index);
	}

	public static void raw_set(LuanTable table,Object index,Object value) throws LuanException {
		table.rawPut(index,value);
	}

	public static int raw_len(Object v) throws LuanException {
		if( v instanceof String ) {
			String s = (String)v;
			return s.length();
		}
		if( v instanceof LuanTable ) {
			LuanTable t = (LuanTable)v;
			return t.rawLength();
		}
		throw new LuanException( "bad argument #1 to 'raw_len' (table or string expected)" );
	}

	public static String to_string(Object v) throws LuanException {
		return Luan.luanToString(v);
	}

	public static LuanTable new_error(Luan luan,Object msg) throws LuanException {
		String s = Luan.luanToString(msg);
		LuanTable tbl = new LuanException(s).table(luan);
		tbl.rawPut( "message", msg );
		return tbl;
	}

	public static int assert_integer(int v) {
		return v;
	}

	public static long assert_long(long v) {
		return v;
	}

	public static double assert_double(double v) {
		return v;
	}

	public static float assert_float(float v) {
		return v;
	}

	public static LuanFunction range(final double from,final double to,Double stepV) throws LuanException {
		final double step = stepV==null ? 1.0 : stepV;
		if( step == 0.0 )
			throw new LuanException("bad argument #3 (step may not be zero)");
		return new LuanFunction(false) {
			double v = from;

			@Override public Object call(Object[] args) {
				if( step > 0.0 && v > to || step < 0.0 && v < to )
					return LuanFunction.NOTHING;
				double rtn = v;
				v += step;
				return rtn;
			}
		};
	}

	private static class Values extends LuanFunction {
		Object[] args;
		int i = 0;

		Values(Object[] args) {
			super(true);
			this.args = args;
		}

		@Override public Object[] call(Object[] x) {
			if( i >= args.length )
				return LuanFunction.NOTHING;
			Object val = args[i++];
			return new Object[]{i,val};
		}

		@Override protected void completeClone(LuanFunction dc,LuanCloner cloner) {
			Values clone = (Values)dc;
			clone.args = (Object[])cloner.clone(args);
			super.completeClone(dc,cloner);
		}
	}

	public static LuanFunction values(final Object... args) throws LuanException {
		return new Values(args);
	}

	private LuanFunction fn(Object obj) {
		return obj instanceof LuanFunction ? (LuanFunction)obj : null;
	}

	public static String number_type(Number v) throws LuanException {
		Utils.checkNotNull(v);
		return v.getClass().getSimpleName().toLowerCase();
	}

	public static int hash_code(Object obj) throws LuanException {
		if( obj == null ) {
			return 0;
		} else if( obj instanceof byte[] ) {
			return Arrays.hashCode((byte[])obj);
		} else {
			return obj.hashCode();
		}
	}

	public static String stringify(Object obj,LuanTable options) throws LuanException {
		LuanToString lts = new LuanToString();
		if( options != null ) {
			options = new LuanTable(options);
			Boolean strict = Utils.removeBoolean(options,"strict");
			if( strict != null )
				lts.strict = strict;
			Boolean numberTypes = Utils.removeBoolean(options,"number_types");
			if( numberTypes != null )
				lts.numberTypes = numberTypes;
			Boolean compressed = Utils.removeBoolean(options,"compressed");
			if( compressed != null )
				lts.compressed = compressed;
			Utils.checkEmpty(options);
		}
		return lts.toString(obj);
	}

	public static String json_string(Object obj,LuanTable options) throws LuanException {
		JsonToString jts = new JsonToString();
		if( options != null ) {
			options = new LuanTable(options);
			Boolean compressed = Utils.removeBoolean(options,"compressed");
			if( compressed != null )
				jts.compressed = compressed;
			Utils.checkEmpty(options);
		}
		return jts.toString(Luan.toJava(obj));
	}

	private void BasicLuan() {}  // never
}
