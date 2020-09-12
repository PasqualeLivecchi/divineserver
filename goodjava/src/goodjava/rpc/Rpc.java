package goodjava.rpc;

import java.io.IOException;


// static utils
public final class Rpc {
	private Rpc() {}  // never

	public static final RpcResult OK = new RpcResult(new Object[0]);

	public static final RpcCall CLOSE = new RpcCall("close");
	public static final RpcCall PING = new RpcCall("ping");
	public static final String ECHO = "echo";

	public static final RpcException COMMAND_NOT_FOUND = new RpcException("command_not_found");

	public static boolean handle(RpcServer server,RpcCall call)
		throws IOException
	{
		if( CLOSE.cmd.equals(call.cmd) ) {
			server.close();
			return true;
		}
		if( PING.cmd.equals(call.cmd) ) {
			server.write(OK);
			return true;
		}
		if( ECHO.equals(call.cmd) ) {
			server.write(new RpcResult(call.args));
			return true;
		}
		return false;
	}

}
