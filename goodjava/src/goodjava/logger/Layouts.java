package goodjava.logger;

import java.io.StringWriter;
import java.io.PrintWriter;


public final class Layouts {
	private Layouts() {}  // never

	public static final Layout MESSAGE = new Layout() {
		public String format(LoggingEvent event) {
			return event.message;
		}
	};

	public static final Layout LOGGER = new Layout() {
		public String format(LoggingEvent event) {
			return event.logger.name;
		}
	};

	public static final Layout LEVEL = new Layout() {
		public String format(LoggingEvent event) {
			return Level.toString(event.level);
		}
	};

	public static final Layout LEVEL_PADDED = new Layout() {
		public String format(LoggingEvent event) {
			return Level.toPaddedString(event.level);
		}
	};

	public static final Layout THROWABLE = new Layout() {
		public String format(LoggingEvent event) {
			if( event.throwable == null )
				return "";
			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			event.throwable.printStackTrace(printWriter);
			return stringWriter.toString();
		}
	};

	public static final Layout THREAD = new Layout() {
		public String format(LoggingEvent event) {
			return Thread.currentThread().getName();
		}
	};

}
