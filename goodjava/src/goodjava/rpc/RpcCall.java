package goodjava.rpc;

import java.io.InputStream;


public final class RpcCall {
	public final InputStream in;
	public final long lenIn;
	public final String cmd;
	public final Object[] args;

	public RpcCall(String cmd,Object... args) {
		this(null,-1L,cmd,args);
	}

	public RpcCall(InputStream in,long lenIn,String cmd,Object... args) {
		this.in = in;
		this.lenIn = lenIn;
		this.cmd = cmd;
		this.args = args;
	}
}
