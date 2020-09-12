package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;


public final class StaticLoggerBinder implements LoggerFactoryBinder {
	private static final ILoggerFactory loggerFactory = new GoodFactory();

	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	public String getLoggerFactoryClassStr() {
		return GoodFactory.class.getName();
	}


	private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

	public static final StaticLoggerBinder getSingleton() {
		return SINGLETON;
	}
}
