package goodjava.logger;

import java.io.PrintStream;
import java.io.OutputStreamWriter;


public final class ConsoleAppender extends WriterAppender {

	public ConsoleAppender(Layout layout,PrintStream ps) {
		super( layout, new OutputStreamWriter(ps) );
	}

	public void close() {}
}
