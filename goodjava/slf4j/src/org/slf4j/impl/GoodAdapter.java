package org.slf4j.impl;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.helpers.FormattingTuple;


final class GoodAdapter extends MarkerIgnoringBase {
	private final goodjava.logging.Logger logger;

	GoodAdapter(goodjava.logging.Logger logger) {
		this.logger = logger;
	}

	public void error​(String msg) {
		logger.error(msg);
	}

	public void error​(String msg, Throwable t) {
		logger.error(msg,t);
	}

	public boolean isErrorEnabled() {
		return true;
	}

	public void error(String format, Object... arguments) {
		FormattingTuple ft = MessageFormatter.arrayFormat(format,arguments);
		logger.error( ft.getMessage(), ft.getThrowable() );
	}

	public void error(String format, Object arg1, Object arg2) {
		error(format,new Object[]{arg1,arg2});
	}

	public void error(String format, Object arg) {
		error(format,new Object[]{arg});
	}

	public void warn(String msg) {
		logger.warn(msg);
	}

	public void warn(String msg, Throwable t) {
		logger.warn(msg,t);
	}

	public boolean isWarnEnabled() {
		return true;
	}

	public void warn(String format, Object... arguments) {
		FormattingTuple ft = MessageFormatter.arrayFormat(format,arguments);
		logger.warn( ft.getMessage(), ft.getThrowable() );
	}

	public void warn(String format, Object arg1, Object arg2) {
		warn(format,new Object[]{arg1,arg2});
	}

	public void warn(String format, Object arg) {
		warn(format,new Object[]{arg});
	}

	public void info(String msg) {
		logger.info(msg);
	}

	public void info(String msg, Throwable t) {
		logger.info(msg,t);
	}

	public boolean isInfoEnabled() {
		return logger.isInfoEnabled();
	}

	public void info(String format, Object... arguments) {
		FormattingTuple ft = MessageFormatter.arrayFormat(format,arguments);
		logger.info( ft.getMessage(), ft.getThrowable() );
	}

	public void info(String format, Object arg1, Object arg2) {
		info(format,new Object[]{arg1,arg2});
	}

	public void info(String format, Object arg) {
		info(format,new Object[]{arg});
	}

	public void debug(String msg) {
		logger.debug(msg);
	}

	public void debug(String msg, Throwable t) {
		logger.debug(msg,t);
	}

	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	public void debug(String format, Object... arguments) {
		FormattingTuple ft = MessageFormatter.arrayFormat(format,arguments);
		logger.debug( ft.getMessage(), ft.getThrowable() );
	}

	public void debug(String format, Object arg1, Object arg2) {
		debug(format,new Object[]{arg1,arg2});
	}

	public void debug(String format, Object arg) {
		debug(format,new Object[]{arg});
	}

	public void trace(String msg) {
	}

	public void trace(String msg, Throwable t) {
	}

	public boolean isTraceEnabled() {
		return false;
	}

	public void trace(String format, Object... arguments) {
	}

	public void trace(String format, Object arg1, Object arg2) {
	}

	public void trace(String format, Object arg) {
	}

}
