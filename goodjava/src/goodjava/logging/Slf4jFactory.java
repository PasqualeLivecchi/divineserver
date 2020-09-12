package goodjava.logging;


public final class Slf4jFactory implements ILoggerFactory {
	private static final class Slf4jLogger implements Logger {
		final org.slf4j.Logger slf4j;

		Slf4jLogger(org.slf4j.Logger slf4j) {
			this.slf4j = slf4j;
		}

		@Override public void error(String msg) {
			slf4j.error(msg);
		}

		@Override public void error(String msg,Throwable t) {
			slf4j.error(msg,t);
		}

		@Override public void warn(String msg) {
			slf4j.warn(msg);
		}

		@Override public void warn(String msg,Throwable t) {
			slf4j.warn(msg,t);
		}

		@Override public void info(String msg) {
			slf4j.info(msg);
		}

		@Override public void info(String msg,Throwable t) {
			slf4j.info(msg,t);
		}

		@Override public boolean isInfoEnabled() {
			return slf4j.isInfoEnabled();
		}

		@Override public void debug(String msg) {
			slf4j.debug(msg);
		}

		@Override public void debug(String msg,Throwable t) {
			slf4j.debug(msg,t);
		}

		@Override public boolean isDebugEnabled() {
			return slf4j.isDebugEnabled();
		}
	}

	@Override public Logger getLogger(String name) {
		return new Slf4jLogger(org.slf4j.LoggerFactory.getLogger(name));
	}
}
