package luan.modules.http;

import java.io.Closeable;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.BindException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.Status;
import goodjava.webserver.Server;
import goodjava.webserver.Handler;
import goodjava.webserver.ResponseOutputStream;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanJavaFunction;
import luan.LuanCloner;
import luan.LuanException;
import luan.modules.PackageLuan;
import luan.modules.BasicLuan;
import luan.modules.logging.LuanLogger;


public final class LuanHandler implements Handler, Closeable {
	private static final Logger logger = LoggerFactory.getLogger(LuanHandler.class);

	private static final Set<LuanHandler> dontGc = Collections.newSetFromMap(new ConcurrentHashMap<LuanHandler,Boolean>());

	private final Luan luanInit;
	private final String domain;
	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private volatile Luan currentLuan;
	private volatile boolean isDisabled = false;

	private static final Method resetLuanMethod;
	private static final Method evalInRootMethod;
	private static final Method disableLuanMethod;
	private static final Method dontGcMethod;
	static {
		try {
			resetLuanMethod = LuanHandler.Fns.class.getMethod( "reset_luan" );
			evalInRootMethod = LuanHandler.Fns.class.getMethod( "eval_in_root", String.class );
			disableLuanMethod = LuanHandler.Fns.class.getMethod( "disable_luan" );
			dontGcMethod = LuanHandler.Fns.class.getMethod( "dont_gc" );
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public LuanHandler(Luan luanInit,String domain) {
		this.luanInit = luanInit;
		this.domain = domain;
		try {
			Fns fns = new Fns(this);
			LuanTable Http = (LuanTable)luanInit.require("luan:http/Http.luan");
			if( Http.get("reset_luan") == null )
				Http.put( "reset_luan", new LuanJavaFunction(luanInit,resetLuanMethod,fns) );
			Http.put( "eval_in_root", new LuanJavaFunction(luanInit,evalInRootMethod,fns) );
			Http.put( "disable_luan", new LuanJavaFunction(luanInit,disableLuanMethod,fns) );
			Http.put( "dont_gc", new LuanJavaFunction(luanInit,dontGcMethod,fns) );
		} catch(LuanException e) {
			throw new RuntimeException(e);
		}
		if( domain != null )
			logger.info("new "+domain);
		currentLuan = newLuan();
	}

	protected void finalize() throws Throwable {
		if( domain != null )
			logger.info("gc "+domain);
	}

	private Luan newLuan() {
		Luan luan;
		synchronized(luanInit) {
			LuanCloner cloner = new LuanCloner(LuanCloner.Type.COMPLETE);
			luan = (Luan)cloner.clone(luanInit);
		}
		LuanLogger.startThreadLogging(luan);
		try {
			PackageLuan.load(luan,"site:/init.luan");
		} catch(LuanException e) {
			//e.printStackTrace();
			String err = e.getLuanStackTraceString();
			logger.error(err);
		} finally {
			LuanLogger.endThreadLogging();
		}
		return luan;
	}

	static final String NOT_FOUND = "luan-not-found";

	@Override public Response handle(Request request) {
		if( isDisabled )
			return null;
		if( request.path.endsWith("/") )
			return null;
		return handle( request, request.headers.containsKey(NOT_FOUND) );
	}

	private Response handle(Request request,boolean notFound) {
		Thread thread = Thread.currentThread();
		String oldName = thread.getName();
		thread.setName(request.headers.get("host")+request.path);
		rwLock.readLock().lock();
		LuanLogger.startThreadLogging(currentLuan);
		try {
			return service(request,notFound);
		} catch(LuanException e) {
			String err = e.getLuanStackTraceString();
			logger.error(err+"\n"+request.rawHead.trim()+"\n");
			String msg = "Internel Server Error\n\n" + err;
			return Response.errorResponse( Status.INTERNAL_SERVER_ERROR, msg );
		} finally {
			LuanLogger.endThreadLogging();
			rwLock.readLock().unlock();
			thread.setName(oldName);
		}
	}

	public void close() {
		Object obj = dontGc.remove(this);
		//logger.info("close "+domain+" "+(obj!=null));
	}

	public Object call_rpc(String fnName,Object... args) throws LuanException {
		rwLock.readLock().lock();
		LuanLogger.startThreadLogging(currentLuan);
		try {
			LuanFunction fn;
			synchronized(luanInit) {
				enableLoad("luan:Rpc.luan");
				LuanTable rpc = (LuanTable)currentLuan.require("luan:Rpc.luan");
				LuanTable fns = (LuanTable)rpc.get("functions");
				fn = (LuanFunction)fns.get(fnName);
				if( fn == null )
					throw new LuanException( "function not found: " + fnName );
				LuanCloner cloner = new LuanCloner(LuanCloner.Type.INCREMENTAL);
				fn = (LuanFunction)cloner.get(fn);
			}
			return fn.call(args);
		} finally {
			LuanLogger.endThreadLogging();
			rwLock.readLock().unlock();
		}
	}

	public static void start(Server server) throws Exception {
		try {
			server.start();
		} catch(BindException e) {
			throw new LuanException(e.toString());
		}
	}

	private void reset_luan() {
		new Thread() {public void run(){
			rwLock.writeLock().lock();
			try {
				close();
				currentLuan = newLuan();
			} finally {
				rwLock.writeLock().unlock();
			}
		}}.start();
	}

	private void disable_luan() {
		isDisabled = true;
	}

	private void eval_in_root(String text) throws LuanException {
		Luan luan;
		synchronized(luanInit) {
			LuanCloner cloner = new LuanCloner(LuanCloner.Type.COMPLETE);
			luan = (Luan)cloner.clone(currentLuan);
		}
		luan.load(text,"<eval_in_root>",false,null).call();
		currentLuan = luan;
	}

	private void dont_gc() {
		dontGc.add(this);
		//logger.info("dont_gc "+domain);
	}

	public static final class Fns {
		private final Reference<LuanHandler> ref;

		private Fns(LuanHandler lh) {
			this.ref = new WeakReference<LuanHandler>(lh);
		}

		private LuanHandler lh() throws LuanException {
			LuanHandler lh = ref.get();
			if( lh == null )
				throw new LuanException("HTTP handler has been garbage collected");
			return lh;
		}

		public void reset_luan() throws LuanException {
			lh().reset_luan();
		}

		public void disable_luan() throws LuanException {
			lh().disable_luan();
		}

		public void eval_in_root(String text) throws LuanException {
			lh().eval_in_root(text);
		}

		public void dont_gc() throws LuanException {
			lh().dont_gc();
		}
	}


	// from HttpServicer

	private Response service(Request request,boolean notFound)
		throws LuanException
	{
		try {
			if( !notFound )
				return serviceLuan(request);
			else
				return serviceNotFound(request);
		} catch(LuanException e) {
			return handleError(request,e);
		}
	}

	private Response handleError(Request request,LuanException e)
		throws LuanException
	{
//e.printStackTrace();
		Luan luan;
		synchronized(luanInit) {
			LuanCloner cloner = new LuanCloner(LuanCloner.Type.INCREMENTAL);
			luan = (Luan)cloner.clone(currentLuan);
		}
		LuanTable module = (LuanTable)luan.require("luan:http/Http.luan");
		return (Response)module.fn("handle_error").call( request, e.table(luan) );
	}

	private Response serviceLuan(Request request)
		throws LuanException
	{
		String modName = "site:" + request.path +".luan";
		LuanFunction fn;
		Luan luan;
		synchronized(luanInit) {
			enableLoad("luan:http/Http.luan",modName);
 			currentLuan.require("luan:http/Http.luan");
			Object mod = PackageLuan.load(currentLuan,modName);
			if( mod.equals(Boolean.FALSE) )
				return null;
			if( !(mod instanceof LuanFunction) )
				throw new LuanException( "module '"+modName+"' must return a function" );
			LuanCloner cloner = new LuanCloner(LuanCloner.Type.INCREMENTAL);
			luan = (Luan)cloner.clone(currentLuan);
			fn = (LuanFunction)cloner.get(mod);
		}
		LuanTable module = (LuanTable)luan.require("luan:http/Http.luan");
		module.fn("new_request").call(request);
		module.fn("new_response").call();
		fn.call();
		return (Response)module.fn("finish").call();
	}

	private Response serviceNotFound(Request request)
		throws LuanException
	{
		LuanFunction fn;
		Luan luan;
		synchronized(luanInit) {
			enableLoad("luan:http/Http.luan");
 			LuanTable module = (LuanTable)currentLuan.require("luan:http/Http.luan");
			fn = module.fn("not_found_handler");
			if( fn == null )
				return null;
			LuanCloner cloner = new LuanCloner(LuanCloner.Type.INCREMENTAL);
			luan = (Luan)cloner.clone(currentLuan);
			fn = (LuanFunction)cloner.get(fn);
		}
		LuanTable module = (LuanTable)luan.require("luan:http/Http.luan");
		module.fn("new_request").call(request);
		module.fn("new_response").call();
		Object obj = Luan.first(fn.call());
		if( !(obj instanceof Boolean) )
			throw new LuanException("not_found_handler must return boolean");
		boolean handled = (Boolean)obj;
		return handled ? (Response)module.fn("finish").call() : null;
	}

	private void enableLoad(String... mods) throws LuanException {
		LuanTable loaded = PackageLuan.loaded(currentLuan);
		for( String mod : mods ) {
			if( loaded.rawGet(mod) == null ) {
				LuanCloner cloner = new LuanCloner(LuanCloner.Type.COMPLETE);
				currentLuan = (Luan)cloner.clone(currentLuan);
				break;
			}
		}
	}

}
