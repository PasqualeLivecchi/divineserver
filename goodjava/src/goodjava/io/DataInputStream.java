package goodjava.io;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class DataInputStream extends java.io.DataInputStream {

	public DataInputStream(InputStream in) {
		super(in);
	}

	public String readString() throws IOException {
		int len = readInt();
		byte[] a = new byte[len];
		readFully(a);
		return new String(a,StandardCharsets.UTF_8);
	}
}
