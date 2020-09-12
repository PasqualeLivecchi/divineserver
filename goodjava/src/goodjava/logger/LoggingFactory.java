package goodjava.logger;

import goodjava.logging.ILoggerFactory;
import goodjava.logging.Logger;


public final class LoggingFactory implements ILoggerFactory {
	@Override public Logger getLogger(String name) {
		return GoodLoggerFactory.getLogger(name);
	}
}
