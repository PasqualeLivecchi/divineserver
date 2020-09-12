package goodjava.logging;


public final class LoggerFactory {
	public static final ILoggerFactory implementation;
	static {
		String s = System.getProperty("goodjava.logger","goodjava.logger.LoggingFactory");
		try {
			implementation = (ILoggerFactory)Class.forName(s).newInstance();
		} catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch(InstantiationException e) {
			throw new RuntimeException(e);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static Logger getLogger(String name) {
		return implementation.getLogger(name);
	}

	public static Logger getLogger(Class cls) {
		return getLogger(cls.getName());
	}
}
