package goodjava.io;

import java.io.InputStream;
import java.io.FilterInputStream;
import java.io.IOException;


public class NoMarkInputStream extends FilterInputStream {

	public NoMarkInputStream(InputStream in) {
		super(in);
	}

	public final void mark(int readlimit) {}

	public final void reset() throws IOException {
		throw new IOException("mark/reset not supported");
	}

	public final boolean markSupported() {
		return false;
	}

}
