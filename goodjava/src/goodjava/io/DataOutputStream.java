package goodjava.io;

import java.io.OutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class DataOutputStream extends java.io.DataOutputStream {

	public DataOutputStream(OutputStream out) {
		super(out);
	}

	public void writeString(String s) throws IOException {
		byte[] a = s.getBytes(StandardCharsets.UTF_8);
		writeInt(a.length);
		write(a);
	}
}
