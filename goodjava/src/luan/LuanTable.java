package luan;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;


public final class LuanTable implements LuanCloneable {
	private Luan luan;
	private Map map = null;
	private List list = null;
	private LuanTable metatable = null;
	public LuanClosure closure;
	private LuanCloner cloner;
	private String security = null;

	public LuanTable(Luan luan) {
		this.luan = luan;
	}

	public LuanTable(Luan luan,List list){
		this.luan = luan;
		list();
		int n = list.size();
		for( int i=0; i<n; i++ ) {
			Object val = list.get(i);
			if( val != null )
				rawPut2(i+1,val);
		}
	}

	public LuanTable(Luan luan,Map map) {
		this.luan = luan;
		for( Object stupid : map.entrySet() ) {
			Map.Entry entry = (Map.Entry)stupid;
			Object key = entry.getKey();
			Object value = entry.getValue();
			if( key != null && value != null )
				rawPut2(key,value);
		}
	}

	public LuanTable(Luan luan,Set set){
		this.luan = luan;
		for( Object el : set ) {
			if( el != null )
				rawPut2(el,Boolean.TRUE);
		}
	}

	public LuanTable(LuanTable tbl) {
		this.luan = tbl.luan;
		if( tbl.map != null && !tbl.map.isEmpty() )
			this.map = new LinkedHashMap<Object,Object>(tbl.map);
		if( tbl.rawLength() > 0 )
			this.list = new ArrayList<Object>(tbl.list);
		this.metatable = tbl.metatable;
	}

	@Override public LuanTable shallowClone() {
		return new LuanTable(luan);
	}

	@Override public void deepenClone(LuanCloneable dc,LuanCloner cloner) {
		check();
		LuanTable clone = (LuanTable)dc;
		clone.security = security;
		switch( cloner.type ) {
		case COMPLETE:
			completeClone(clone,cloner);
			return;
		case INCREMENTAL:
			clone.cloner = cloner;
			clone.map = map;
			clone.list = list;
			clone.metatable = metatable;
			clone.closure = closure;
			return;
		}
	}

	private void check() {
		if( cloner != null ) {
			completeClone(this,cloner);
			cloner = null;
		}
	}

	public Luan luan() {
		check();
		return luan;
	}

	private void completeClone(LuanTable clone,LuanCloner cloner) {
		clone.luan = (Luan)cloner.clone(luan);
		if( map != null ) {
			Map newMap = newMap();
			for( Object stupid : map.entrySet() ) {
				Map.Entry entry = (Map.Entry)stupid;
				newMap.put( cloner.get(entry.getKey()), cloner.get(entry.getValue()) );
			}
			clone.map = newMap;
		}
		if( list != null ) {
			List newList = new ArrayList<Object>();
			for( Object obj : list ) {
				newList.add(cloner.get(obj));
			}
			clone.list = newList;
		}
		clone.metatable = (LuanTable)cloner.clone(metatable);
		clone.closure = (LuanClosure)cloner.clone(closure);
	}

	public boolean isList() {
		return map==null || map.isEmpty();
	}

	public boolean isMap() {
		return map!=null || list==null;
	}

	public List<Object> asList() {
		check();
		return list!=null ? list : Collections.emptyList();
	}

	public Map rawMap() {
		check();
		return map!=null ? map : Collections.emptyMap();
	}

	public String toStringLuan() throws LuanException {
		Object h = getHandler("__to_string");
		if( h == null )
			return rawToString();
		LuanFunction fn = Luan.checkFunction(h);
		return Luan.checkString( Luan.first( fn.call(this) ) );
	}

	public String rawToString() {
		return "table: " + Integer.toHexString(hashCode());
	}

	public Object get(Object key) throws LuanException {
		Object value = rawGet(key);
		if( value != null )
			return value;
		Object h = getHandler("__index");
		if( h==null )
			return null;
		if( h instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)h;
			return Luan.first(fn.call(this,key));
		}
		return luan.index(h,key);
	}

	public Object rawGet(Object key) {
		check();
		if( list != null ) {
			Integer iT = Luan.asInteger(key);
			if( iT != null ) {
				int i = iT - 1;
				if( i>=0 && i<list.size() )
					return list.get(i);
			}
		}
		if( map==null )
			return null;
		if( key instanceof Number && !(key instanceof Double) ) {
			Number n = (Number)key;
			key = Double.valueOf(n.doubleValue());
		}
		return map.get(key);
	}

	public void put(Object key,Object value) throws LuanException {
		Object h = getHandler("__new_index");
		if( h==null || rawGet(key)!=null ) {
			rawPut(key,value);
			return;
		}
		if( h instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)h;
			fn.call(this,key,value);
			return;
		}
		if( h instanceof LuanTable ) {
			LuanTable tbl = (LuanTable)h;
			tbl.put(key,value);
			return;
		}
		throw new LuanException("invalid type "+Luan.type(h)+" for metamethod __new_index");
	}

	public void rawPut(Object key,Object val) throws LuanException {
		if( security != null )
			Luan.checkSecurity(luan,"table",security,"put",key,val);
		rawPut2(key,val);
	}

	private void rawPut2(Object key,Object val) {
		check();
		Integer iT = Luan.asInteger(key);
		if( iT != null ) {
			int i = iT - 1;
			if( list != null || i == 0 ) {
				if( i == list().size() ) {
					if( val != null ) {
						list.add(val);
						mapToList();
					}
					return;
				} else if( i>=0 && i<list.size() ) {
					list.set(i,val);
					if( val == null ) {
						listToMap(i);
					}
					return;
				}
			}
		}
		if( key instanceof Number && !(key instanceof Double) ) {
			Number n = (Number)key;
			key = Double.valueOf(n.doubleValue());
		}
		if( val == null ) {
			if( map!=null )
				map.remove(key);
		} else {
			if( map==null )
				map = newMap();
			map.put(key,val);
		}
	}

	private void mapToList() {
		if( map != null ) {
			while(true) {
				Object v = map.remove(Double.valueOf(list.size()+1));
				if( v == null )
					break;
				list.add(v);
			}
		}
	}

	private void listToMap(int from) {
		if( list != null ) {
			while( list.size() > from ) {
				int i = list.size() - 1;
				Object v = list.remove(i);
				if( v != null ) {
					if( map==null )
						map = newMap();
					map.put(i+1,v);
				}
			}
		}
	}

	private List<Object> list() {
		if( list == null ) {
			list = new ArrayList<Object>();
			mapToList();
		}
		return list;
	}

	public void rawInsert(int pos,Object value) {
		check();
		if( value==null )
			throw new IllegalArgumentException("can't insert a nil value");
		list().add(pos-1,value);
		mapToList();
	}

	public void rawAdd(Object value) {
		check();
		if( value==null )
			throw new IllegalArgumentException("can't insert a nil value");
		list().add(value);
		mapToList();
	}

	public Object removeFromList(int pos) {
		check();
		return list().remove(pos-1);
	}

	public void rawSort(Comparator<Object> cmp) {
		check();
		Collections.sort(list(),cmp);
	}

	public int length() throws LuanException {
		Object h = getHandler("__len");
		if( h != null ) {
			LuanFunction fn = Luan.checkFunction(h);
			return (Integer)Luan.first(fn.call(this));
		}
		return rawLength();
	}

	public int rawLength() {
		check();
		return list==null ? 0 : list.size();
	}

	public Iterable<Map.Entry> iterable() throws LuanException {
		final Iterator<Map.Entry> iter = iterator();
		return new Iterable<Map.Entry>() {
			public Iterator<Map.Entry> iterator() {
				return iter;
			}
		};
	}

	public Iterable<Map.Entry> rawIterable() {
		final Iterator<Map.Entry> iter = rawIterator();
		return new Iterable<Map.Entry>() {
			public Iterator<Map.Entry> iterator() {
				return iter;
			}
		};
	}

	public Iterator<Map.Entry> iterator() throws LuanException {
		if( getHandler("__pairs") == null )
			return rawIterator();
		final LuanFunction fn = pairs();
		return new Iterator<Map.Entry>() {
			private Map.Entry<Object,Object> next = getNext();

			private Map.Entry<Object,Object> getNext() {
				try {
					Object obj = fn.call();
					if( obj==null )
						return null;
					Object[] a = (Object[])obj;
					if( a.length == 0 || a[0]==null )
						return null;
					return new AbstractMap.SimpleEntry<Object,Object>(a[0],a[1]);
				} catch(LuanException e) {
					throw new LuanRuntimeException(e);
				}
			}

			public boolean hasNext() {
				return next != null;
			}

			public Map.Entry<Object,Object> next() {
				Map.Entry<Object,Object> rtn = next;
				next = getNext();
				return rtn;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public LuanFunction pairs() throws LuanException {
		Object h = getHandler("__pairs");
		if( h != null ) {
			if( h instanceof LuanFunction ) {
				LuanFunction fn = (LuanFunction)h;
				Object obj = Luan.first(fn.call(this));
				if( !(obj instanceof LuanFunction) )
					throw new LuanException( "metamethod __pairs should return function but returned " + Luan.type(obj) );
				return (LuanFunction)obj;
			}
			throw new LuanException( "invalid type of metamethod __pairs: " + Luan.type(h) );
		}
		return rawPairs();
	}

	private LuanFunction rawPairs() {
		return new LuanFunction(false) {  // ???
			final Iterator<Map.Entry> iter = rawIterator();

			@Override public Object[] call(Object[] args) {
				if( !iter.hasNext() )
					return LuanFunction.NOTHING;
				Map.Entry<Object,Object> entry = iter.next();
				return new Object[]{entry.getKey(),entry.getValue()};
			}
		};
	}

	public Iterator<Map.Entry> rawIterator() {
		check();
		if( list == null ) {
			if( map == null )
				return Collections.<Map.Entry>emptyList().iterator();
			return map.entrySet().iterator();
		}
		if( map == null )
			return listIterator();
		return new Iterator<Map.Entry>() {
			Iterator<Map.Entry> iter = listIterator();
			boolean isList = true;

			public boolean hasNext() {
				boolean b = iter.hasNext();
				if( !b && isList ) {
					iter = map.entrySet().iterator();
					isList = false;
					b = iter.hasNext();
				}
				return b;
			}

			public Map.Entry<Object,Object> next() {
				return iter.next();
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	private Iterator<Map.Entry> listIterator() {
		if( list == null )
			return Collections.<Map.Entry>emptyList().iterator();
		final ListIterator iter = list.listIterator();
		return new Iterator<Map.Entry>() {

			public boolean hasNext() {
				return iter.hasNext();
			}

			public Map.Entry<Object,Object> next() {
				Integer key = iter.nextIndex()+1;
				return new AbstractMap.SimpleEntry<Object,Object>(key,iter.next());
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public LuanTable rawSubList(int from,int to) {
		check();
		LuanTable tbl = new LuanTable(luan);
		tbl.list = new ArrayList<Object>(list().subList(from-1,to-1));
		return tbl;
	}

	public LuanTable getMetatable() {
		check();
		return metatable;
	}

	public void setMetatable(LuanTable metatable) throws LuanException {
		if( security != null )
			Luan.checkSecurity(luan,"table",security,"set_metatable",metatable);
		check();
		this.metatable = metatable;
	}

	public Object getHandler(String op) throws LuanException {
		check();
		return metatable==null ? null : metatable.get(op);
	}

	private Map<Object,Object> newMap() {
		return new LinkedHashMap<Object,Object>();
	}

	public boolean isSet() throws LuanException {
		for( Map.Entry<Object,Object> entry : iterable() ) {
			if( !entry.getValue().equals(Boolean.TRUE) )
				return false;
		}
		return true;
	}

	public Set<Object> asSet() throws LuanException {
		Set<Object> set = new HashSet<Object>();
		for( Map.Entry<Object,Object> entry : iterable() ) {
			set.add(entry.getKey());
		}
		return set;
	}

	public Map<Object,Object> asMap() throws LuanException {
		Map<Object,Object> map = newMap();
		for( Map.Entry<Object,Object> entry : iterable() ) {
			map.put(entry.getKey(),entry.getValue());
		}
		return map;
	}

	public void rawClear() {
		check();
		map = null;
		list = null;
	}

	public int hashValue() {
		int n = 99;
		if( map != null )
			n ^= map.hashCode();
		if( list != null )
			n ^= list.hashCode();
		return n;
	}

	public boolean isEmpty() {
		return (map==null || map.isEmpty()) && (list==null || list.isEmpty());
	}

	public int rawSize() {
		int n = 0;
		if( map != null )
			n += map.size();
		if( list != null )
			n += list.size();
		return n;
	}

	public Object remove(Object key) {
		Object old = rawGet(key);
		rawPut2(key,null);
		return old;
	}

	protected void finalize() throws Throwable {
		Object h = getHandler("__gc");
		if( h != null ) {
			LuanFunction fn = Luan.checkFunction(h);
			fn.call(this);
		}
		super.finalize();
	}

	public LuanFunction fn(String fnName) throws LuanException {
		return (LuanFunction)get(fnName);
	}

	public static void setSecurity(LuanTable tbl,String security) {
		tbl.security = security;
	}

	public static void debug(LuanTable table) {
		System.out.println("isMap "+table.isMap());
	}
}
