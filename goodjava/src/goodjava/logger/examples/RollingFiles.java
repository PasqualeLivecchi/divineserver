package goodjava.logger.examples;

import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.logger.Layout;
import goodjava.logger.Layouts;
import goodjava.logger.DateLayout;
import goodjava.logger.ListLayout;
import goodjava.logger.Appender;
import goodjava.logger.RollingFileAppender;
import goodjava.logger.LevelAppender;
import goodjava.logger.ListAppender;
import goodjava.logger.Level;
import goodjava.logger.SimpleConfigurer;
import goodjava.logger.GoodLoggerFactory;
import java.io.IOException;


public class RollingFiles {
	private static final Logger logger = LoggerFactory.getLogger(RollingFiles.class);

	public static void main(String[] args) throws Exception {
		config();
		logger.debug("test debug");
		logger.info("test info");
		logger.warn("test warn");
		logger.error("test error");
	}

	static void config() throws IOException {
		Layout layout = new ListLayout(new DateLayout("yyyy-MM-dd HH:mm:ss,SSS")," ",Layouts.LEVEL_PADDED," ",Layouts.LOGGER," - ",Layouts.MESSAGE,"\n",Layouts.THROWABLE);
		Appender error = appender(layout,"error.log",Level.ERROR);
		Appender warn = appender(layout,"warn.log",Level.WARN);
		Appender info = appender(layout,"info.log",Level.INFO);
		Appender appender = new ListAppender(error,warn,info);
		GoodLoggerFactory.setConfigurer( new SimpleConfigurer(Level.INFO,appender) );
	}

	static Appender appender(Layout layout,String fileName,int level) throws IOException {
		RollingFileAppender appender = new RollingFileAppender(layout,fileName);
		return new LevelAppender(appender,level);
	}
}
