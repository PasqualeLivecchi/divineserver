package luan.modules;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import goodjava.util.WeakCacheMap;
import luan.Luan;
import luan.LuanFunction;
import luan.LuanTable;
import luan.LuanException;
import luan.LuanCloner;
import luan.LuanCloneable;
import luan.modules.logging.LuanLogger;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public final class ThreadLuan {
	private static final Logger logger = LoggerFactory.getLogger(ThreadLuan.class);

	private static final Executor exec = Executors.newCachedThreadPool();
	public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private static Runnable runnable(final LuanFunction fn) {
		return new Runnable() {
			public synchronized void run() {
				LuanLogger.startThreadLogging(fn.luan());
				try {
					fn.call();
				} catch(LuanException e) {
					e.printStackTrace();
				} finally {
					LuanLogger.endThreadLogging();
				}
			}
		};
	}

	public static void fork(LuanFunction fn) {
		LuanCloner cloner = new LuanCloner(LuanCloner.Type.COMPLETE);
		final LuanFunction newFn = (LuanFunction)cloner.get(fn);
		exec.execute(runnable(newFn));
	}

	private static final Map<String,ScheduledFuture> scheduleds = new WeakCacheMap<String,ScheduledFuture>();

	private static void cancel(ScheduledFuture sf,String src) {
		boolean b = sf.cancel(false);
		if( !sf.isCancelled() )
			logger.error(src+" cancel="+b+" isCancelled="+sf.isCancelled()+" isDone="+sf.isDone()+" "+sf);
	}

	public static synchronized void schedule(LuanFunction fn,LuanTable options)
		throws LuanException
	{
		options = new LuanTable(options);
		Number delay = Utils.removeNumber(options,"delay");
		Number repeatingDelay = Utils.removeNumber(options,"repeating_delay");
		Number repeatingRate = Utils.removeNumber(options,"repeating_rate");
		String id = Utils.removeString(options,"id");
		if( repeatingDelay!=null && repeatingRate!=null )
			throw new LuanException("can't define both repeating_delay and repeating_rate");
		boolean repeating = repeatingDelay!=null || repeatingRate!=null;
		Utils.checkEmpty(options);
		if( id != null ) {
			ScheduledFuture sf = scheduleds.remove(id);
			if( sf != null )
				cancel(sf,"id "+id);
		}
		Luan luan = fn.luan();
		LuanCloner cloner = new LuanCloner(LuanCloner.Type.COMPLETE);
		final Luan newLuan = (Luan)cloner.clone(luan);
		final LuanFunction newFn = (LuanFunction)cloner.get(fn);
		final Runnable r = runnable(newFn);
		final ScheduledFuture sf;
		if( repeatingDelay != null ) {
			if( delay==null )
				delay = repeatingDelay;
			sf = scheduler.scheduleWithFixedDelay(r,delay.longValue(),repeatingDelay.longValue(),TimeUnit.MILLISECONDS);
		} else if( repeatingRate != null ) {
			if( delay==null )
				delay = repeatingRate;
			sf = scheduler.scheduleWithFixedDelay(r,delay.longValue(),repeatingRate.longValue(),TimeUnit.MILLISECONDS);
		} else if( delay != null ) {
			sf = scheduler.schedule(r,delay.longValue(),TimeUnit.MILLISECONDS);
		} else {
			scheduler.schedule(r,0L,TimeUnit.MILLISECONDS);
			return;
		}
		Object c = new Object() {
			protected void finalize() throws Throwable {
				cancel(sf,"gc");
			}
		};
		luan.registry().put(c,c);  // cancel on gc
		if( id != null )
			scheduleds.put(id,sf);
	}

/*
	public static class GlobalMap {

		private static class Value {
			final long time = System.currentTimeMillis();
			final Object v;

			Value(Object v) {
				this.v = v;
			}
		}

		public long timeout = 60000L;  // one minute
		private Map<String,Value> map = new LinkedHashMap<String,Value>() {
			protected boolean removeEldestEntry(Map.Entry<String,Value> eldest) {
				return eldest.getValue().time < System.currentTimeMillis() - timeout;
			}
		};

		public synchronized Object get(String key) {
			Value val = map.get(key);
			return val==null ? null : val.v;
		}

		public synchronized Object put(String key,Object v) throws LuanException {
			Value val;
			if( v == null ) {
				val = map.remove(key);
			} else {
				if( !(v instanceof String || v instanceof Boolean || v instanceof Number) )
					throw new LuanException("can't assign type "+Luan.type(v)+" to Thread.global");
				val = map.put(key,new Value(v));
			}
			return val==null ? null : val.v;
		}
	}
*/

	public static void sleep(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}


	private static class Unsafe {
		private final String reason;

		Unsafe(String reason) {
			this.reason = reason;
		}
	}

	private static Object makeSafe(Luan luan,Object v) throws LuanException {
		if( v instanceof LuanTable ) {
			LuanTable tbl = (LuanTable)v;
			if( tbl.getMetatable() != null )
				return new Unsafe("table with metatable");
			LuanTable rtn = new LuanTable(luan);
			for( Map.Entry entry : tbl.rawIterable() ) {
				Object key = makeSafe( luan, entry.getKey() );
				if( key instanceof Unsafe )
					return key;
				Object value = makeSafe( luan, entry.getValue() );
				if( value instanceof Unsafe )
					return value;
				rtn.rawPut(key,value);
			}
			return rtn;
		} else if( v instanceof Object[] ) {
			Object[] a = (Object[])v;
			for( int i=0; i<a.length; i++ ) {
				Object obj = makeSafe(luan,a[i]);
				if( obj instanceof Unsafe )
					return obj;
				a[i] = obj;
			}
			return a;
		} else {
			if( v instanceof LuanCloneable )
				return new Unsafe("type "+Luan.type(v));
			return v;
		}
	}

	public static final class Callable {
		private long expires;
		private final Luan luan = new Luan();
		private final LuanTable fns;

		Callable(LuanTable fns) {
			LuanCloner cloner = new LuanCloner(LuanCloner.Type.COMPLETE);
			this.fns = (LuanTable)cloner.get(fns);
		}

		public synchronized Object call(Luan callerLuan,String fnName,Object... args) throws LuanException {
			Object obj = makeSafe(luan,args);
			if( obj instanceof Unsafe )
				throw new LuanException("can't pass "+((Unsafe)obj).reason+" to global_callable "+Arrays.asList(args));
			args = (Object[])obj;
			Object f = fns.get(fnName);
			if( f == null )
				throw new LuanException("function '"+fnName+"' not found in global_callable");
			if( !(f instanceof LuanFunction) )
				throw new LuanException("value of '"+fnName+"' not a function in global_callable");
			LuanFunction fn = (LuanFunction)f;
			Object rtn = fn.call(args);
			rtn = makeSafe(callerLuan,rtn);
			if( rtn instanceof Unsafe )
				throw new LuanException("can't return "+((Unsafe)rtn).reason+" from global_callable");
			return rtn;
		}
	}

	private static Map<String,Callable> callableMap = new HashMap<String,Callable>();

	private static void sweep() {
		long now = System.currentTimeMillis();
		for( Iterator<Callable> iter = callableMap.values().iterator(); iter.hasNext(); ) {
			Callable callable = iter.next();
			if( callable.expires < now )
				iter.remove();
		}
	}

	public static synchronized Callable globalCallable(String name,LuanTable fns,long timeout) {
		Callable callable = callableMap.get(name);
		if( callable == null ) {
			sweep();
			callable = new Callable(fns);
			callableMap.put(name,callable);
		}
		callable.expires = System.currentTimeMillis() + timeout;
		return callable;
	}

	public static synchronized void removeGlobalCallable(String name) {
		callableMap.remove(name);
	}


	public static Object runInLock(Lock lock,long timeout,LuanFunction fn,Object... args)
		throws LuanException, InterruptedException
	{
		if( !lock.tryLock(timeout,TimeUnit.MILLISECONDS) )
			throw new LuanException("failed to acquire lock");
		try {
			return fn.call(args);
		} finally {
			lock.unlock();
		}
	}

	private static final Map<String,Lock> locks = new WeakCacheMap<String,Lock>();

	public static synchronized Lock getLock(String key) {
		Lock lock = locks.get(key);
		if( lock == null ) {
			lock = new ReentrantLock();
			locks.put(key,lock);
		}
		return lock;
	}

}
