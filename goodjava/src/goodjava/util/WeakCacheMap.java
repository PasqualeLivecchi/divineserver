package goodjava.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;


public class WeakCacheMap<K,V> extends CacheMap<K,V> {

	static final class MyWeakReference<K,V> extends WeakReference<V> implements MyReference<K,V> {
		private final K key;

		MyWeakReference(K key,V value,ReferenceQueue<V> q) {
			super(value,q);
			this.key = key;
		}

		public K key() {
			return key;
		}

		public boolean equals(Object obj) {
			Object o = this.get();
			if( o==null )
				return false;
			WeakReference ref = (WeakReference)obj;
			return o.equals(ref.get());
		}

	}

	protected MyReference<K,V> newReference(K key,V value,ReferenceQueue<V> q) {
		return new MyWeakReference<K,V>(key,value,q);
	}

}
