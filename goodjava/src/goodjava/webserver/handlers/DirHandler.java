package goodjava.webserver.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import goodjava.webserver.Handler;
import goodjava.webserver.Request;
import goodjava.webserver.Response;
import goodjava.webserver.ResponseOutputStream;


public final class DirHandler implements Handler {
	private final FileHandler fileHandler;

	public DirHandler(FileHandler fileHandler) {
		this.fileHandler = fileHandler;
	}

	private static final Comparator<File> sorter = new Comparator<File>() {
		public int compare(File f1, File f2) {
			return f1.getName().compareTo(f2.getName());
		}
	};

	public Response handle(Request request) {
		try {
			File file = fileHandler.file(request);
			if( request.path.endsWith("/") && file.isDirectory() ) {
				DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
				Response response = new Response();
				response.headers.put( "content-type", "text/html; charset=utf-8" );
				Writer writer = new OutputStreamWriter( new ResponseOutputStream(response) );
				writer.write( "<!doctype html><html>" );
				writer.write( "<head><style>td{padding: 2px 8px}</style></head>" );
				writer.write( "<body>" );
				writer.write( "<h1>Directory: "+request.path+"</h1>" );
				writer.write( "<table border=0>" );
				File[] a = file.listFiles();
				Arrays.sort(a,sorter);
				for( File child : a ) {
					String name = child.getName();
					if( child.isDirectory() )
						name += '/';
					writer.write( "<tr>" );
					writer.write( "<td><a href='"+name+"'>"+name+"</a></td>" );
					writer.write( "<td>"+child.length()+" bytes</td>" );
					writer.write( "<td>"+fmt.format(new Date(child.lastModified()))+"</td>" );
					writer.write( "</tr>" );
				}
				writer.write( "</table>" );
				writer.write( "</body>" );
				writer.write( "</html>" );
				writer.close();
				return response;
			}
			return null;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
