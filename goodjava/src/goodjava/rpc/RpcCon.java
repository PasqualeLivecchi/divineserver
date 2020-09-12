package goodjava.rpc;

import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import goodjava.parser.ParseException;
import goodjava.json.JsonParser;
import goodjava.json.JsonToString;
import goodjava.io.BufferedInputStream;
import goodjava.io.DataInputStream;
import goodjava.io.DataOutputStream;
import goodjava.io.IoUtils;
import goodjava.io.CountingInputStream;


public class RpcCon {
	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;
	InputStream inBinary = null;
	long lenBinary = -1;

	RpcCon(Socket socket)
		throws RpcError
	{
		try {
			this.socket = socket;
			this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
		} catch(IOException e) {
			close();
			throw new RpcError(e);
		}
	}

	public void close()
		throws RpcError
	{
		try {
			socket.close();
		} catch(IOException e) {
			throw new RpcError(e);
		}
	}

	public boolean isClosed() {
		return socket.isClosed();
	}

	void write(InputStream in,long lenIn,List list)
		throws RpcError
	{
		if( in != null )
			list.add(0,lenIn);
		String json = new JsonToString().toString(list);
		try {
			out.writeString(json);
			if( in != null ) {
				CountingInputStream countIn = new CountingInputStream(in);
				IoUtils.copyAll(countIn,out);
				if( countIn.count() != lenIn ) {
					close();
					throw new RpcError("InputStream wrong length "+countIn.count()+" when should be "+lenIn+" - list = "+list);
				}
			}
			out.flush();
		} catch(IOException e) {
			close();
			throw new RpcError(e);
		}
	}

	List readJson()
		throws RpcError
	{
		try {
			if( inBinary != null ) {
				inBinary.close();
				inBinary = null;
				lenBinary = -1;
			}
			String json = in.readString();
			List list = (List)JsonParser.parse(json);
			if( list.get(0) instanceof Long ) {
				lenBinary = (Long)list.remove(0);
				inBinary = new FixedLengthInputStream(in,lenBinary);
			}
			return list;
		} catch(IOException e) {
			close();
			throw new RpcError(e);
		} catch(ParseException e) {
			close();
			throw new RpcError(e);
		}
	}

}
