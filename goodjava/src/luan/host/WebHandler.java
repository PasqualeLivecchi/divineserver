package luan.host;

import java.io.File;
import java.io.IOException;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.io.IoUtils;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.handlers.DomainHandler;
import luan.Luan;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanClosure;
import luan.LuanRuntimeException;
import luan.modules.http.LuanHandler;
import luan.modules.logging.LuanLogger;


public class WebHandler implements Handler {
	private static final Logger logger = LoggerFactory.getLogger(WebHandler.class);

	private static final DomainHandler.Factory factory = new DomainHandler.Factory() {
		public Handler newHandler(String domain) {
			File dir = new File(sitesDir,domain);
			if( !dir.exists() )
				return null;
			String dirStr = dir.toString();

			String logDir = dirStr + "/site/private/local/logs/web";
			try {
				IoUtils.mkdirs(new File(logDir));
			} catch(IOException e) {
				throw new RuntimeException(e);
			}

			Luan luan = new Luan();
			initLuan(luan,dirStr,domain);
			return new LuanHandler(luan,domain);
		}
	};

	public static String securityPassword = "password";  // change for security
	private static final DomainHandler domainHandler = new DomainHandler(factory);
	private static String sitesDir = null;

	public WebHandler(String dir) {
		if( sitesDir != null )
			throw new RuntimeException("already set");
		if( !new File(dir).exists() )
			throw new RuntimeException();
		sitesDir = dir;
		LuanLogger.initThreadLogging();
	}

	@Override public Response handle(Request request) {
		return domainHandler.handle(request);
	}

	public static Object callSite(String domain,String fnName,Object... args) throws LuanException {
		LuanHandler luanHandler = (LuanHandler)domainHandler.getHandler(domain);
		return luanHandler.call_rpc(fnName,args);
	}

	private static void initLuan(Luan luan,String dir,String domain) {
		LuanLogger.startThreadLogging(luan);
		try {
			LuanFunction fn = Luan.loadClasspath(luan,"luan/host/init.luan");
			fn.call(dir,domain);
		} catch(LuanException e) {
			throw new LuanRuntimeException(e);
		} finally {
			LuanLogger.endThreadLogging();
		}
		security(luan,dir);
	}

	public static void removeHandler(String domain) {
		domainHandler.removeHandler(domain);
	}

	public static void loadHandler(String domain) throws LuanException {
		try {
			domainHandler.getHandler(domain);
		} catch(LuanRuntimeException e) {
			throw (LuanException)e.getCause();
		}
	}

	private static final void security(Luan luan,String dir) {
		final String siteUri = "file:" + dir + "/site";
		Luan.Security security = new Luan.Security() {
			public void check(Luan luan,LuanClosure closure,String op,Object... args)
				throws LuanException
			{
				if( op.equals("uri") ) {
					String name = (String)args[0];
					if( name.startsWith("file:") ) {
						if( name.contains("..") )
							throw new LuanException("Security violation - '"+name+"' contains '..'");
						if( !(name.equals(siteUri) || name.startsWith(siteUri+"/")) )
							throw new LuanException("Security violation - '"+name+"' outside of site dir");
					}
					else if( name.startsWith("classpath:luan/host/") ) {
						throw new LuanException("Security violation");
					}
					else if( name.startsWith("os:") || name.startsWith("bash:") ) {
						throw new LuanException("Security violation");
					}
				} else {
					String name = closure.sourceName;
					if( !(
						name.startsWith("luan:")
						|| name.startsWith("classpath:")
						|| name.matches("^file:[^/]+$")
					) )
						throw new LuanException("Security violation - only luan:* modules can load Java");
					if( name.equals("luan:logging/Logging") )
						throw new LuanException("Security violation - cannot reload Logging");
				}
			}
		};
		Luan.setSecurity(luan,security);
	}

}
