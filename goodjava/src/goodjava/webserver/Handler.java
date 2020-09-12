package goodjava.webserver;


public interface Handler {
	public Response handle(Request request);
}
