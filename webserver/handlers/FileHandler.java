package goodjava.webserver.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.ResponseOutputStream;


public class FileHandler implements Handler {
	final File dir;

	public FileHandler() {
		this(".");
	}

	public FileHandler(String pathname) {
		this(new File(pathname));
	}

	public FileHandler(File dir) {
		if( !dir.isDirectory() )
			throw new RuntimeException("must be a directory");
		this.dir = dir;
	}

	File file(Request request) {
		return new File(dir,request.path);
	}

	public Response handle(Request request) {
		try {
			File file = file(request);
			if( file.isFile() ) {
				Response response = new Response();
				response.body = new Response.Body( file.length(), new FileInputStream(file) );
				return response;
			}
			return null;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
