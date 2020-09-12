package goodjava.logger;

import goodjava.logging.Logger;


public final class GoodLogger implements Logger {
	public final String name;
	public volatile int level;
	public volatile Appender appender;

	GoodLogger(String name) {
		this.name = name;
	}

	private void log(int level,String msg,Throwable t) {
		if( level < this.level )
			return;
		LoggingEvent event = new LoggingEvent(this,level,msg,t);
		appender.append(event);
	}


	@Override public void error(String msg) {
		error(msg,null);
	}

	@Override public void error(String msg,Throwable t) {
		log(Level.ERROR,msg,t);
	}

	@Override public void warn(String msg) {
		warn(msg,null);
	}

	@Override public void warn(String msg,Throwable t) {
		log(Level.WARN,msg,t);
	}

	@Override public void info(String msg) {
		info(msg,null);
	}

	@Override public void info(String msg,Throwable t) {
		log(Level.INFO,msg,t);
	}

	@Override public boolean isInfoEnabled() {
		return isEnabled(Level.INFO);
	}

	@Override public void debug(String msg) {
		debug(msg,null);
	}

	@Override public void debug(String msg,Throwable t) {
		log(Level.DEBUG,msg,t);
	}

	@Override public boolean isDebugEnabled() {
		return isEnabled(Level.DEBUG);
	}

	private boolean isEnabled(int level) {
		return true;
	}
}
