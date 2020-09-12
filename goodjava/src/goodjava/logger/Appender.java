package goodjava.logger;


public interface Appender {
	public void append(LoggingEvent event);
	public void close();
}
