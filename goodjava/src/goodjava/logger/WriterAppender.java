package goodjava.logger;

import java.io.Writer;
import java.io.IOException;


public class WriterAppender implements Appender {
	protected final Layout layout;
	protected Writer writer;

	public WriterAppender(Layout layout,Writer writer) {
		this.layout = layout;
		this.writer = writer;
	}

	public synchronized void append(LoggingEvent event) {
		try {
			writer.write( layout.format(event) );
			flush();
		} catch(IOException e) {
			printStackTrace(e);
		}
	}

	protected void flush() throws IOException {
		writer.flush();
	}

	public void close() {
		try {
			writer.close();
		} catch(IOException e) {
			printStackTrace(e);
		}
	}

	protected void printStackTrace(IOException e) {
		e.printStackTrace();
	}
}
