package goodjava.logger;

import java.util.Map;
import java.util.HashMap;


public final class GoodLoggerFactory {
	private GoodLoggerFactory() {}  // never

	public static final Appender DEFAULT_APPENDER;
	public static final Configurer DEFAULT_CONFIGURER;
	static {
		Layout layout = new ListLayout(Layouts.LEVEL," - ",Layouts.MESSAGE,"\n",Layouts.THROWABLE);
		DEFAULT_APPENDER = new ConsoleAppender(layout,System.err);
		DEFAULT_CONFIGURER = new SimpleConfigurer(Level.INFO,DEFAULT_APPENDER);
	}
	private static final Map<String,GoodLogger> map = new HashMap<String,GoodLogger>();
	private static Configurer configurer = DEFAULT_CONFIGURER;

	public static synchronized GoodLogger getLogger(String name) {
		GoodLogger logger = map.get(name);
		if( logger == null ) {
			logger = new GoodLogger(name);
			configurer.configure(logger);
			map.put(name,logger);
		}
		return logger;
	}

	public static synchronized Configurer getConfigurer() {
		return configurer;
	}

	public static synchronized void setConfigurer(Configurer configurer) {
		GoodLoggerFactory.configurer = configurer;
		for( GoodLogger logger : map.values() ) {
			configurer.configure(logger);
		}
	}

}
