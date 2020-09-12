package goodjava.rpc;

import java.io.EOFException;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;


public class RpcServer extends RpcCon {

	public RpcServer(Socket socket)
		throws RpcError
	{
		super(socket);
	}

	public RpcCall read()
		throws RpcError
	{
		try {
			List list = readJson();
			String cmd = (String)list.remove(0);
			Object[] args = list.toArray();
			return new RpcCall(inBinary,lenBinary,cmd,args);
		} catch(RpcError e) {
			if( e.getCause() instanceof EOFException )
				return null;
			throw e;
		}
	}

	public void write(RpcResult result)
		throws RpcError
	{
		List list = new ArrayList();
		list.add(true);
		for( Object val : result.returnValues ) {
			list.add(val);
		}
		write(result.in,result.lenIn,list);
	}

	public void write(RpcException ex)
		throws RpcError
	{
		List list = new ArrayList();
		list.add(false);
		list.add(ex.getMessage());
		for( Object val : ex.values ) {
			list.add(val);
		}
		write(ex.in,ex.lenIn,list);
	}

}
