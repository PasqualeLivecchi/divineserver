package goodjava.logger;


public class SimpleConfigurer implements Configurer {
	public final int level;
	public final Appender appender;

	public SimpleConfigurer(int level,Appender appender) {
		this.level = level;
		this.appender = appender;
	}

	public void configure(GoodLogger logger) {
		logger.level = level;
		logger.appender = appender;
	}	
}
