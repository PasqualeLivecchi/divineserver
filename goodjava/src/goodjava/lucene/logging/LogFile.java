package goodjava.lucene.logging;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.RandomAccessFile;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import goodjava.io.FixedLengthInputStream;
import goodjava.io.BufferedInputStream;
import goodjava.io.IoUtils;


public class LogFile {
	private static final Logger logger = LoggerFactory.getLogger(LogFile.class);
	public final File file;
	long end;

	public LogFile(File file) throws IOException {
		this.file = file;
		RandomAccessFile raf = new RandomAccessFile(file,"rwd");
		if( raf.length() == 0 ) {
			end = 8;
			raf.writeLong(end);
		} else {
			raf.seek(0L);
			end = raf.readLong();
			raf.seek(end);
		}
		raf.close();
	}

	public String toString() {
		return "LogFile<" + file.getName() + ">";
	}

	public long end() {
		return end;
	}

	public LogOutputStream output() throws IOException {
		RandomAccessFile raf = new RandomAccessFile(file,"rwd");
		OutputStream out = new FileOutputStream(raf.getFD());
		out = new BufferedOutputStream(out);
		return newLogOutputStream(raf,out);
	}

	protected LogOutputStream newLogOutputStream(RandomAccessFile raf,OutputStream out) throws IOException {
		return new LogOutputStream(this,raf,out);
	}

	public LogInputStream input() throws IOException {
		InputStream in = new FileInputStream(file);
		in = new FixedLengthInputStream(in,end);
		in = new BufferedInputStream(in);
		LogInputStream lis = newLogInputStream(in);
		lis.readLong();  // skip end
		return lis;
	}

	protected LogInputStream newLogInputStream(InputStream in) {
		return new LogInputStream(in);
	}

	public long checksum() throws IOException {
		return IoUtils.checksum(input());
	}

	static final int TYPE_NULL = 0;
	static final int TYPE_STRING = 1;
	static final int TYPE_INT = 2;
	static final int TYPE_LONG = 3;
	static final int TYPE_FLOAT = 4;
	static final int TYPE_DOUBLE = 5;
	static final int TYPE_BYTES = 6;
	static final int TYPE_LIST = 7;
	static final int TYPE_QUERY_MATCH_ALL_DOCS = 8;
	static final int TYPE_QUERY_TERM = 9;
	static final int TYPE_QUERY_PREFIX = 10;
	static final int TYPE_QUERY_WILDCARD = 11;
	static final int TYPE_QUERY_TERM_RANGE = 12;
	static final int TYPE_QUERY_PHRASE = 13;
	static final int TYPE_QUERY_NUMERIC_RANGE = 14;
	static final int TYPE_QUERY_BOOLEAN = 15;
}
