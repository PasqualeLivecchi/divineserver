package goodjava.rpc;

import java.net.Socket;
import java.util.List;
import java.util.ArrayList;


public class RpcClient extends RpcCon {

	public RpcClient(Socket socket)
		throws RpcError
	{
		super(socket);
	}

	public void write(RpcCall call)
		throws RpcError
	{
		List list = new ArrayList();
		list.add(call.cmd);
		for( Object arg : call.args ) {
			list.add(arg);
		}
		write(call.in,call.lenIn,list);
	}

	public RpcResult read()
		throws RpcError, RpcException
	{
		List list = readJson();
		boolean ok = (Boolean)list.remove(0);
		if( !ok ) {
			String errorId = (String)list.remove(0);
			Object[] args = list.toArray();
			throw new RpcException(inBinary,lenBinary,errorId,args);
		}
		Object[] args = list.toArray();
		return new RpcResult(inBinary,lenBinary,args);
	}

}
