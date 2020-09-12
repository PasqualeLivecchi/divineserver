package goodjava.logger;


public final class LevelAppender implements Appender {
	private final Appender appender;
	private final int level;

	public LevelAppender(Appender appender,int level) {
		this.appender = appender;
		this.level = level;
	}

	public void append(LoggingEvent event) {
		if( event.level >= this.level )
			appender.append(event);
	}

	public void close() {
		appender.close();
	}
}
