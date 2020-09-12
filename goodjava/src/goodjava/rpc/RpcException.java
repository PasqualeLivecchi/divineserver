package goodjava.rpc;

import java.io.InputStream;


public class RpcException extends Exception {
	public final InputStream in;
	public final long lenIn;
	public final Object[] values;

	public RpcException(String id,Object... values) {
		this(null,-1,id,values);
	}

	public RpcException(InputStream in,long lenIn,String id,Object... values) {
		super(id);
		this.in = in;
		this.lenIn = lenIn;
		this.values = values;
	}
}
