package goodjava.logger;


public final class ListAppender implements Appender {
	private final Appender[] appenders;

	public ListAppender(Appender... appenders) {
		this.appenders = appenders;
	}

	public synchronized void append(LoggingEvent event) {
		for( Appender appender : appenders ) {
			appender.append(event);
		}
	}

	public void close() {
		for( Appender appender : appenders ) {
			appender.close();
		}
	}
}
