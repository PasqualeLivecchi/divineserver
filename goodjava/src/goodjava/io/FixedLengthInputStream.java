package goodjava.io;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;


public class FixedLengthInputStream extends NoMarkInputStream {
	protected long left;

	public FixedLengthInputStream(InputStream in, long len) {
		super(in);
		if( len < 0 )
			throw new IllegalArgumentException("len can't be negative");
		this.left = len;
	}

	public int read() throws IOException {
		if( left == 0 )
			return -1;
		int n = in.read();
		if( n == -1 )
			throw new EOFException();
		left--;
		return n;
	}

	public int read(byte[] b, int off, int len) throws IOException {
		if( len == 0 )
			return 0;
		if( left == 0 )
			return -1;
		if( len > left )
			len = (int)left;
		int n = in.read(b,off,len);
		if( n == -1 )
			throw new EOFException();
		left -= n;
		return n;
	}

	public long skip(long n) throws IOException {
		if( n > left )
			n = left;
		n = in.skip(n);
		left -= n;
		return n;
	}

	public int available() throws IOException {
		int n = in.available();
		if( n > left )
			n = (int)left;
		return n;
	}

}
