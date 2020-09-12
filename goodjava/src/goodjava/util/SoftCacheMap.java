package goodjava.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;


public final class SoftCacheMap<K,V> extends CacheMap<K,V> {

	static final class MySoftReference<K,V> extends SoftReference<V> implements MyReference<K,V> {
		private final K key;

		MySoftReference(K key,V value,ReferenceQueue<V> q) {
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
			SoftReference ref = (SoftReference)obj;
			return o.equals(ref.get());
		}

	}

	protected MyReference<K,V> newReference(K key,V value,ReferenceQueue<V> q) {
		return new MySoftReference<K,V>(key,value,q);
	}

}
