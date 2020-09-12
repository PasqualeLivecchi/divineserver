package goodjava.rpc;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;


final class FixedLengthInputStream extends goodjava.io.FixedLengthInputStream {

	public FixedLengthInputStream(InputStream in,long len) {
		super(in,len);
	}

    public void close() throws IOException {
        while( left > 0 ) {
			if( skip(left) == 0 )
				throw new EOFException();
		}
    }

}
