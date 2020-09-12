package luan.modules.logging;

import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.logger.LoggingFactory;
import goodjava.logger.GoodLoggerFactory;
import goodjava.logger.Appender;
import goodjava.logger.ThreadLocalAppender;
import goodjava.logger.SimpleConfigurer;
import goodjava.logger.Level;
import luan.Luan;
import luan.LuanException;


public final class LuanLogger {
	private final Logger logger;

	public LuanLogger(String name) {
		this.logger = LoggerFactory.getLogger(name);
	}

	public void error(Object obj) throws LuanException {
		logger.error( Luan.luanToString(obj) );
	}

	public void warn(Object obj) throws LuanException {
		logger.warn( Luan.luanToString(obj) );
	}

	public void info(Object obj) throws LuanException {
		logger.info( Luan.luanToString(obj) );
	}

	public void debug(Object obj) throws LuanException {
		logger.debug( Luan.luanToString(obj) );
	}


	private static final String KEY = "Logger.Appender";
	private static volatile Appender globalAppender = GoodLoggerFactory.DEFAULT_APPENDER;

	public static synchronized void initThreadLogging() {
		if( !(globalAppender instanceof ThreadLocalAppender) ) {
			if( !(LoggerFactory.implementation instanceof LoggingFactory) )
				throw new RuntimeException("must use goodjava.logger for thread logging");
			globalAppender = new ThreadLocalAppender(globalAppender);
			configure();
		}
	}

	public static void startThreadLogging(Luan luan) {
		if( !(globalAppender instanceof ThreadLocalAppender) )
			return;
		ThreadLocalAppender tla = (ThreadLocalAppender)globalAppender;
		Appender appender = (Appender)luan.registry().get(KEY);
		if( appender == null )
			appender = tla.defaultAppender;
		tla.threadLocal.set(appender);
	}

	public static void endThreadLogging() {
		if( !(globalAppender instanceof ThreadLocalAppender) )
			return;
		ThreadLocalAppender tla = (ThreadLocalAppender)globalAppender;
		tla.threadLocal.remove();
	}

	public static boolean isConfigured() {
		if( !(LoggerFactory.implementation instanceof LoggingFactory) )
			return true;
		return GoodLoggerFactory.getConfigurer() != GoodLoggerFactory.DEFAULT_CONFIGURER;
	}

	public static void configure(Luan luan,Appender appender) {
		if( globalAppender instanceof ThreadLocalAppender ) {
			ThreadLocalAppender tla = (ThreadLocalAppender)globalAppender;
			if( tla.threadLocal.get() != null ) {
				luan.registry().put(KEY,appender);
				tla.threadLocal.set(appender);
			} else {
				tla.defaultAppender = appender;
			}
		} else {
			globalAppender = appender;
			configure();
		}
	}

	private static void configure() {
		GoodLoggerFactory.setConfigurer( new SimpleConfigurer(Level.INFO,globalAppender) );
	}

}
