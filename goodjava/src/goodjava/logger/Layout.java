package goodjava.logger;


public interface Layout {
	public String format(LoggingEvent event);
}
