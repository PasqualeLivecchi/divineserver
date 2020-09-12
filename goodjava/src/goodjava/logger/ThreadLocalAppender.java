package goodjava.logger;


public final class ThreadLocalAppender implements Appender {
	public volatile Appender defaultAppender;
	public final ThreadLocal<Appender> threadLocal = new InheritableThreadLocal<Appender>();

	public ThreadLocalAppender(Appender defaultAppender) {
		this.defaultAppender = defaultAppender;
	}

	public void append(LoggingEvent event) {
		Appender appender = threadLocal.get();
		if( appender == null )
			appender = defaultAppender;
		appender.append(event);
	}

	public void close() {
		defaultAppender.close();
	}
}
