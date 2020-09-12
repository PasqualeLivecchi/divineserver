package goodjava.logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public final class DateLayout implements Layout {
	private final Date date = new Date();
	private final DateFormat dateFormat;

	public DateLayout(String pattern) {
		dateFormat = new SimpleDateFormat(pattern);
	}

	public synchronized String format(LoggingEvent event) {
		date.setTime(event.time);
		return dateFormat.format(date);
	}
}
