package luan;

import java.util.Map;
import java.util.Collection;
import java.util.IdentityHashMap;


public final class LuanCloner {
	public enum Type { COMPLETE, INCREMENTAL }

	public final Type type;
	private final Map cloned = new IdentityHashMap();
	private Luan luan = null;

	public LuanCloner(Type type) {
		this.type = type;
	}

	public LuanCloneable clone(LuanCloneable obj) {
		if( obj==null )
			return null;
		LuanCloneable rtn = (LuanCloneable)cloned.get(obj);
		if( rtn == null ) {
			if( obj instanceof Luan ) {
				if( luan != null )
					throw new RuntimeException("2 luans in "+type+" "+this+" - "+luan+" "+obj);
				luan = (Luan)obj;
			}
			rtn = obj.shallowClone();
			cloned.put(obj,rtn);
			obj.deepenClone(rtn,this);
		}
		return rtn;
	}

	public Object[] clone(Object[] obj) {
		if( obj.length == 0 )
			return obj;
		Object[] rtn = (Object[])cloned.get(obj);
		if( rtn == null ) {
			rtn = obj.clone();
			cloned.put(obj,rtn);
			for( int i=0; i<rtn.length; i++ ) {
				rtn[i] = get(rtn[i]);
			}
		}
		return rtn;
	}

	public Map clone(Map obj) {
		Map rtn = (Map)cloned.get(obj);
		if( rtn == null ) {
			try {
				rtn = obj.getClass().newInstance();
			} catch(InstantiationException e) {
				throw new RuntimeException(e);
			} catch(IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			for( Object stupid : obj.entrySet() ) {
				Map.Entry entry = (Map.Entry)stupid;
				rtn.put( get(entry.getKey()), get(entry.getValue()) );
			}
		}
		return rtn;
	}

	public Collection clone(Collection obj) {
		Collection rtn = (Collection)cloned.get(obj);
		if( rtn == null ) {
			try {
				rtn = obj.getClass().newInstance();
			} catch(InstantiationException e) {
				throw new RuntimeException(e);
			} catch(IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			for( Object entry : (Collection)obj ) {
				rtn.add( get(entry) );
			}
		}
		return rtn;
	}

	public Object get(Object obj) {
		if( obj instanceof LuanCloneable )
			return clone((LuanCloneable)obj);
		if( obj instanceof Object[] )
			return clone((Object[])obj);
		if( obj instanceof Map )
			return clone((Map)obj);
		if( obj instanceof Collection )
			return clone((Collection)obj);
		return obj;
	}
}
