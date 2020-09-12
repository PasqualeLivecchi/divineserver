package luan.modules;

import java.io.UnsupportedEncodingException;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;


public final class BinaryLuan {

	static int start(byte[] binary,int i) {
		int len = binary.length;
		return i==0 ? 0 : i > 0 ? Math.min(i-1,len) : Math.max(len+i,0);
	}

	static int start(byte[] binary,Integer i,int dflt) {
		return i==null ? dflt : start(binary,i);
	}

	static int end(byte[] binary,int i) {
		int len = binary.length;
		return i==0 ? 0 : i > 0 ? Math.min(i,len) : Math.max(len+i+1,0);
	}

	static int end(byte[] binary,Integer i,int dflt) {
		return i==null ? dflt : end(binary,i);
	}

	public static Byte[] byte_(byte[] binary,Integer i,Integer j) throws LuanException {
		Utils.checkNotNull(binary);
		int start = start(binary,i,1);
		int end = end(binary,j,start+1);
		Byte[] bytes = new Byte[end-start];
		for( int k=0; k<bytes.length; k++ ) {
			bytes[k] = binary[start+k];
		}
		return bytes;
	}

	public static byte[] binary(byte... bytes) {
		return bytes;
	}

	private static String toString(byte[] a) {
		char[] ac = new char[a.length];
		for( int i=0; i<a.length; i++ ) {
			ac[i] = (char)a[i];
		}
		return new String(ac);
	}

	public static String to_string(byte[] binary,String charsetName) throws LuanException, UnsupportedEncodingException {
		Utils.checkNotNull(binary);
		return charsetName!=null ? new String(binary,charsetName) : toString(binary);
	}

}
