package goodjava.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import goodjava.io.IoUtils;


public class RollingFileAppender extends WriterAppender {
	protected final String fileName;
	protected final File file;
	public long maxFileSize = 10*1024*1024;
	public int backups = 1;

	public RollingFileAppender(Layout layout,String fileName) throws IOException {
		super(layout,null);
		this.fileName = fileName;
		this.file = new File(fileName);
		open();
	}

	protected void open() throws IOException {
		this.writer = new FileWriter(file,true);
	}

	public synchronized void append(LoggingEvent event) {
		super.append(event);
		if( file.length() > maxFileSize ) {
			rollOver();
		}
	}

	protected void rollOver() {
		close();
		File backup = new File(fileName+'.'+backups);
		try {
			IoUtils.delete(backup);
			for( int i=backups-1; i>=1; i-- ) {
				File f = backup;
				backup = new File(fileName+'.'+i);
				IoUtils.move(backup,f);
			}
			IoUtils.move(file,backup);
			open();
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
