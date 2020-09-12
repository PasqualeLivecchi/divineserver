package goodjava.io;

import java.io.InputStream;
import java.io.IOException;


public final class CountingInputStream extends NoMarkInputStream {
	private long count = 0;

	public CountingInputStream(InputStream in) {
		super(in);
	}

	public long count() {
		return count;
	}

	public int read() throws IOException {
		int n = in.read();
		if( n != -1 )
			count++;
		return n;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		int n = in.read(b,off,len);
		if( n != -1 )
			count += n;
		return n;
	}

	public long skip(long n) throws IOException {
		n = in.skip(n);
		if( n != -1 )
			count += n;
		return n;
	}

}
