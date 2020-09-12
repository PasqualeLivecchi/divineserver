package goodjava.logger.examples;

import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


public class Min {
	private static final Logger logger = LoggerFactory.getLogger(Min.class);

	public static void main(String[] args) {
		logger.debug("test debug");
		logger.info("test info");
		logger.warn("test warn");
		logger.error("test error");
	}
}
