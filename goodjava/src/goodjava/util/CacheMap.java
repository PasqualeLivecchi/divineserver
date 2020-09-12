package goodjava.util;

import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;
import java.util.AbstractSet;
import java.util.Iterator;


public abstract class CacheMap<K,V> extends AbstractMap<K,V> {

	protected static interface MyReference<K,V> {
		public K key();
		public V get();
		public void clear();
	}

	protected abstract MyReference<K,V> newReference(K key,V value,ReferenceQueue<V> q);
 
	private final Map<K,MyReference<K,V>> cache = new HashMap<K,MyReference<K,V>>();
	private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

	private void sweep() {
		while(true) {
			@SuppressWarnings("unchecked")
			MyReference<K,V> ref = (MyReference<K,V>)queue.poll();
			if( ref == null )
				return;
			MyReference<K,V> mappedRef = cache.remove(ref.key());
			if( mappedRef != ref && mappedRef != null )
				cache.put( mappedRef.key(), mappedRef );  // put it back
		}
	}

	public int size() {
		return cache.size();
	}

	public boolean isEmpty() {
		return cache.isEmpty();
	}

	public boolean containsKey(Object key) {
		return cache.containsKey(key);
	}

	public V get(Object key) {
		MyReference<K,V> ref = cache.get(key);
		return ref==null ? null : ref.get();
	}

	public V put(K key,V value) {
		sweep();
		MyReference<K,V> ref = cache.put( key, newReference(key,value,queue) );
		return ref==null ? null : ref.get();
	}

	public V remove(Object key) {
		sweep();
		MyReference<K,V> ref = cache.remove(key);
		return ref==null ? null : ref.get();
	}

	public void clear() {
		sweep();
		cache.clear();
	}

/*
	public Object clone() {
		GCCacheMap map = new GCCacheMap();
		map.cache = (HashMap)cache.clone();
		return map;
	}
*/
	public Set<K> keySet() {
		return cache.keySet();
	}

	public Set<Map.Entry<K,V>> entrySet() {
		return new MySet();
	}


	private class MySet extends AbstractSet<Map.Entry<K,V>> {

		public int size() {
			return CacheMap.this.size();
		}

		public Iterator<Map.Entry<K,V>> iterator() {
			return new MyIterator(cache.entrySet().iterator());
		}

	}

	private class MyIterator implements Iterator<Map.Entry<K,V>> {
		Iterator<Map.Entry<K,MyReference<K,V>>> iter;

		MyIterator(Iterator<Map.Entry<K,MyReference<K,V>>> iter) {
			this.iter = iter;
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public void remove() {
			iter.remove();
		}

		public Map.Entry<K,V> next() {
			return new MyEntry( iter.next() );
		}
	}

	private class MyEntry implements Map.Entry<K,V> {
		Map.Entry<K,MyReference<K,V>> entry;

		MyEntry(Map.Entry<K,MyReference<K,V>> entry) {
			this.entry = entry;
		}

		public K getKey() {
			return entry.getKey();
		}

		public V getValue() {
			MyReference<K,V> ref = entry.getValue();
			return ref.get();
		}

		public V setValue(V value) {
			MyReference<K,V> ref = entry.setValue( newReference(getKey(),value,queue) );
			return ref.get();
		}

		public boolean equals(Object o) {
			if( o==null || !(o instanceof CacheMap.MyEntry) )
				return false;
			@SuppressWarnings("unchecked")
			MyEntry m = (MyEntry)o;
			return entry.equals(m.entry);
		}

		public int hashCode() {
			K key = getKey();
			V value = getValue();
			return (key==null ? 0 : key.hashCode()) ^
					(value==null ? 0 : value.hashCode());
		}
	}

}
