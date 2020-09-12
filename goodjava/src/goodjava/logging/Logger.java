package goodjava.logging;


public interface Logger {
	public void error(String msg);
	public void error(String msg,Throwable t);
	public void warn(String msg);
	public void warn(String msg,Throwable t);
	public void info(String msg);
	public void info(String msg,Throwable t);
	public boolean isInfoEnabled();
	public void debug(String msg);
	public void debug(String msg,Throwable t);
	public boolean isDebugEnabled();
}
