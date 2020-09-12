package goodjava.logger;


public final class LoggingEvent {
	public final GoodLogger logger;
	public final int level;
	public final String message;
	public final Throwable throwable;
	public final long time = System.currentTimeMillis();

	LoggingEvent(GoodLogger logger,int level,String message,Throwable throwable) {
		this.logger = logger;
		this.level = level;
		this.message = message;
		this.throwable = throwable;
	}
}
