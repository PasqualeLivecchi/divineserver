package goodjava.logger.examples;

import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.logger.Layout;
import goodjava.logger.Layouts;
import goodjava.logger.DateLayout;
import goodjava.logger.ListLayout;
import goodjava.logger.Appender;
import goodjava.logger.WriterAppender;
import goodjava.logger.Level;
import goodjava.logger.SimpleConfigurer;
import goodjava.logger.GoodLoggerFactory;
import java.io.FileWriter;
import java.io.IOException;


public class LogFile {
	private static final Logger logger = LoggerFactory.getLogger(LogFile.class);

	public static void main(String[] args) throws Exception {
		config();
		logger.debug("test debug");
		logger.info("test info");
		logger.warn("test warn");
		logger.error("test error");
	}

	static void config() throws IOException {
		Layout layout = new ListLayout(new DateLayout("yyyy-MM-dd HH:mm:ss,SSS")," ",Layouts.LEVEL_PADDED," ",Layouts.LOGGER," - ",Layouts.MESSAGE,"\n",Layouts.THROWABLE);
		Appender appender = new WriterAppender(layout,new FileWriter("test.log",true));
		GoodLoggerFactory.setConfigurer( new SimpleConfigurer(Level.INFO,appender) );
	}
}
