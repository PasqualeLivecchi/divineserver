package goodjava.rpc;


public class RpcError extends RuntimeException {

	public RpcError(String msg) {
		super(msg);
	}

	public RpcError(Exception e) {
		super(e);
	}

}
