package luan.modules;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanRuntimeException;


public final class TableLuan {

	public static String concat(LuanTable list,String sep,Integer i,Integer j) throws LuanException {
		int first = i==null ? 1 : i;
		int last = j==null ? list.length() : j;
		StringBuilder buf = new StringBuilder();
		for( int k=first; k<=last; k++ ) {
			Object val = list.get(k);
			if( val==null )
				break;
			if( sep!=null && k > first )
				buf.append(sep);
			String s = Luan.luanToString(val);
			buf.append(s);
		}
		return buf.toString();
	}

	public static void insert(LuanTable list,int pos,Object value) throws LuanException {
		Utils.checkNotNull(list);
		if( list.getMetatable() != null )
			throw new LuanException("can't insert into a table with a metatable");
		list.rawInsert(pos,value);
	}

	public static Object remove(LuanTable list,int pos) throws LuanException {
		if( list.getMetatable() != null )
			throw new LuanException("can't remove from a table with a metatable");
		return list.removeFromList(pos);
	}

	private static interface LessThan {
		public boolean isLessThan(Object o1,Object o2);
	}

	public static void sort(LuanTable list,final LuanFunction comp) throws LuanException {
		if( list.getMetatable() != null )
			throw new LuanException("can't sort a table with a metatable");
		final LessThan lt;
		if( comp==null ) {
			lt = new LessThan() {
				public boolean isLessThan(Object o1,Object o2) {
					try {
						return Luan.isLessThan(o1,o2);
					} catch(LuanException e) {
						throw new LuanRuntimeException(e);
					}
				}
			};
		} else {
			lt = new LessThan() {
				public boolean isLessThan(Object o1,Object o2) {
					try {
						return Luan.checkBoolean(Luan.first(comp.call(o1,o2)));
					} catch(LuanException e) {
						throw new LuanRuntimeException(e);
					}
				}
			};
		}
		try {
			list.rawSort( new Comparator<Object>() {
				public int compare(Object o1,Object o2) {
					return lt.isLessThan(o1,o2) ? -1 : lt.isLessThan(o2,o1) ? 1 : 0;
				}
			} );
		} catch(LuanRuntimeException e) {
			throw (LuanException)e.getCause();
		}
	}

	public static LuanTable pack(Luan luan,Object... args) throws LuanException {
		LuanTable tbl = new LuanTable(luan,Arrays.asList(args));
		tbl.rawPut( "n", args.length );
		return tbl;
	}

	public static Object[] unpack(LuanTable tbl,Integer iFrom,Integer iTo) throws LuanException {
		int from = iFrom!=null ? iFrom : 1;
		int to;
		if( iTo != null ) {
			to = iTo;
		} else {
			Integer n = Luan.asInteger( tbl.get("n") );
			to = n!=null ? n : tbl.length();
		}
		List<Object> list = new ArrayList<Object>();
		for( int i=from; i<=to; i++ ) {
			list.add( tbl.get(i) );
		}
		return list.toArray();
	}

	public static LuanTable copy(LuanTable list,Integer from,Integer to) {
		if( from == null )
			return new LuanTable(list);
		if( to == null )
			to = list.rawLength();
		return list.rawSubList(from,to);
	}

	public static void clear(LuanTable tbl) {
		tbl.rawClear();
	}

	public static int hash_value(LuanTable tbl) throws LuanException {
		return tbl.hashValue();
	}

	public static boolean is_empty(LuanTable tbl) throws LuanException {
		return tbl.isEmpty();
	}

	public static int size(LuanTable tbl) throws LuanException {
		return tbl.rawSize();
	}

	public static LuanTable toTable(Luan luan,Object obj) {
		return luan.toTable(obj);
	}

}
