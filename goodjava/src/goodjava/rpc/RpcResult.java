package goodjava.rpc;

import java.io.InputStream;


public final class RpcResult {
	public final InputStream in;
	public final long lenIn;
	public final Object[] returnValues;

	public RpcResult(Object[] returnValues) {
		this(null,-1L,returnValues);
	}

	public RpcResult(InputStream in,long lenIn,Object[] returnValues) {
		this.in = in;
		this.lenIn = lenIn;
		this.returnValues = returnValues;
	}
}
