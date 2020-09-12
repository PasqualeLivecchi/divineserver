package luan.impl;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.modules.JavaLuan;


public final class LuanImpl {
	private LuanImpl() {}  // never

	public static int len(Object o) throws LuanException {
		if( o instanceof String ) {
			String s = (String)o;
			return s.length();
		}
		if( o instanceof byte[] ) {
			byte[] a = (byte[])o;
			return a.length;
		}
		if( o instanceof LuanTable ) {
			LuanTable t = (LuanTable)o;
			return t.length();
		}
		throw new LuanException( "attempt to get length of a " + Luan.type(o) + " value" );
	}

	public static Object unm(Object o) throws LuanException {
		if( o instanceof Number )
			return -((Number)o).doubleValue();
		if( o instanceof LuanTable ) {
			LuanFunction fn = Luan.getHandlerFunction("__unm",(LuanTable)o);
			if( fn != null ) {
				return Luan.first(fn.call(o));
			}
		}
		throw new LuanException("attempt to perform arithmetic on a "+Luan.type(o)+" value");
	}

	private static Object arithmetic(String op,Object o1,Object o2) throws LuanException {
		LuanFunction fn = Luan.getBinHandler(op,o1,o2);
		if( fn != null )
			return Luan.first(fn.call(o1,o2));
		String type = !(o1 instanceof Number) ? Luan.type(o1) : Luan.type(o2);
		throw new LuanException("attempt to perform arithmetic on a "+type+" value");
	}

	public static Object pow(Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number )
			return Math.pow( ((Number)o1).doubleValue(), ((Number)o2).doubleValue() );
		return arithmetic("__pow",o1,o2);
	}

	public static Object mul(Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number )
			return ((Number)o1).doubleValue() * ((Number)o2).doubleValue();
		return arithmetic("__mul",o1,o2);
	}

	public static Object div(Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number )
			return ((Number)o1).doubleValue() / ((Number)o2).doubleValue();
		return arithmetic("__div",o1,o2);
	}

	public static Object mod(Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number ) {
			double d1 = ((Number)o1).doubleValue();
			double d2 = ((Number)o2).doubleValue();
			return d1 - Math.floor(d1/d2)*d2;
		}
		return arithmetic("__mod",o1,o2);
	}

	public static Object add(Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number )
			return ((Number)o1).doubleValue() + ((Number)o2).doubleValue();
		return arithmetic("__add",o1,o2);
	}

	public static Object sub(Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number )
			return ((Number)o1).doubleValue() - ((Number)o2).doubleValue();
		return arithmetic("__sub",o1,o2);
	}

	public static Object concat(Object o1,Object o2) throws LuanException {
		LuanFunction fn = Luan.getBinHandler("__concat",o1,o2);
		if( fn != null )
			return Luan.first(fn.call(o1,o2));
		String s1 = Luan.luanToString(o1);
		String s2 = Luan.luanToString(o2);
		return s1 + s2;
	}

	public static boolean eq(Object o1,Object o2) throws LuanException {
		if( o1 == o2 || o1 != null && o1.equals(o2) )
			return true;
		if( o1 instanceof Number && o2 instanceof Number ) {
			Number n1 = (Number)o1;
			Number n2 = (Number)o2;
			return n1.doubleValue() == n2.doubleValue();
		}
		if( o1 instanceof byte[] && o2 instanceof byte[] ) {
			byte[] b1 = (byte[])o1;
			byte[] b2 = (byte[])o2;
			return Arrays.equals(b1,b2);
		}
		if( !(o1 instanceof LuanTable && o2 instanceof LuanTable) )
			return false;
		LuanTable t1 = (LuanTable)o1;
		LuanTable t2 = (LuanTable)o2;
		LuanTable mt1 = t1.getMetatable();
		LuanTable mt2 = t2.getMetatable();
		if( mt1==null || mt2==null )
			return false;
		Object f = mt1.rawGet("__eq");
		if( f == null || !f.equals(mt2.rawGet("__eq")) )
			return false;
		LuanFunction fn = Luan.checkFunction(f);
		return Luan.checkBoolean( Luan.first(fn.call(o1,o2)) );
	}

	public static boolean le(Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number ) {
			Number n1 = (Number)o1;
			Number n2 = (Number)o2;
			return n1.doubleValue() <= n2.doubleValue();
		}
		if( o1 instanceof String && o2 instanceof String ) {
			String s1 = (String)o1;
			String s2 = (String)o2;
			return s1.compareTo(s2) <= 0;
		}
		LuanFunction fn = Luan.getBinHandler("__le",o1,o2);
		if( fn != null )
			return Luan.checkBoolean( Luan.first(fn.call(o1,o2)) );
		fn = Luan.getBinHandler("__lt",o1,o2);
		if( fn != null )
			return !Luan.checkBoolean( Luan.first(fn.call(o2,o1)) );
		throw new LuanException( "attempt to compare " + Luan.type(o1) + " with " + Luan.type(o2) );
	}

	public static boolean lt(Object o1,Object o2) throws LuanException {
		return Luan.isLessThan(o1,o2);
	}

	public static boolean cnd(Object o) throws LuanException {
		return !(o == null || Boolean.FALSE.equals(o));
	}

	public static void nop(Object o) {}

	public static void put(Luan luan,Object t,Object key,Object value) throws LuanException {
		if( t instanceof LuanTable ) {
			LuanTable tbl = (LuanTable)t;
			tbl.put(key,value);
			return;
		}
		if( t != null && luan.peek().javaOk )
			JavaLuan.__new_index(luan,t,key,value);
		else
			throw new LuanException( "attempt to index a " + Luan.type(t) + " value" );
	}

	public static Object pick(Object o,int i) {
		if( i < 1 )
			throw new RuntimeException();
		if( !(o instanceof Object[]) )
			return null;
		Object[] a = (Object[])o;
		return pick(a,i);
	}

	public static Object pick(Object[] a,int i) {
		return i<a.length ? a[i] : null;
	}

	public static void noMore(final Object[] a,final int n) throws LuanException {
		if( a.length > n ) {
			for( int i=n; i<a.length; i++ ) {
				if( a[i] != null )
					throw new LuanException("too many arguments");
			}
		}
	}

	public static Object[] varArgs(Object[] a,int i) {
		if( i >= a.length )
			return LuanFunction.NOTHING;
		Object[] rtn = new Object[a.length - i];
		System.arraycopy(a,i,rtn,0,rtn.length);
		return rtn;
	}

	public static Object[] concatArgs(Object o1,Object o2) {
		if( o1 instanceof Object[] ) {
			Object[] a1 = (Object[])o1;
			if( o2 instanceof Object[] ) {
				Object[] a2 = (Object[])o2;
				Object[] rtn = new Object[a1.length+a2.length];
				System.arraycopy(a1,0,rtn,0,a1.length);
				System.arraycopy(a2,0,rtn,a1.length,a2.length);
				return rtn;
			} else {
				Object[] rtn = new Object[a1.length+1];
				System.arraycopy(a1,0,rtn,0,a1.length);
				rtn[a1.length] = o2;
				return rtn;
			}
		} else {
			if( o2 instanceof Object[] ) {
				Object[] a2 = (Object[])o2;
				Object[] rtn = new Object[1+a2.length];
				rtn[0] = o1;
				System.arraycopy(a2,0,rtn,1,a2.length);
				return rtn;
			} else {
				Object[] rtn = new Object[2];
				rtn[0] = o1;
				rtn[2] = o2;
				return rtn;
			}
		}
	}

	public static LuanTable table(Luan luan,Object[] a) throws LuanException {
		LuanTable table = new LuanTable(luan);
		int i = 0;
		for( Object fld : a ) {
			if( fld instanceof TableField ) {
				TableField tblFld = (TableField)fld;
				Object key = tblFld.key;
				Object value = tblFld.value;
				if( key != null && value != null )
					table.rawPut(key,value);
			} else {
				i++;
				table.rawPut(i,fld);
			}
		}
		return table;
	}

	public static Object first(Object[] a) {
		return a.length==0 ? null : a[0];
	}

	public static String strconcat(String... a) {
		StringBuilder sb = new StringBuilder();
		for( String s : a ) {
			sb.append(s);
		}
		return sb.toString();
	}

	public static void nopTry() throws LuanException {}

}
