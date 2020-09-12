package org.slf4j.impl;

import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.ILoggerFactory;


public final class GoodFactory implements ILoggerFactory {
	private final Map<String,Logger> map = new HashMap<String,Logger>();

	public synchronized Logger getLogger(String name) {
		Logger logger = map.get(name);
		if( logger == null ) {
			logger = new GoodAdapter(goodjava.logging.LoggerFactory.getLogger(name));
			map.put(name,logger);
		}
		return logger;
	}
}
