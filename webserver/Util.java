package goodjava.webserver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;


final class Util {

	static String urlEncode(String s) {
		try {
			return URLEncoder.encode(s,"UTF-8");
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	static void add(Map<String,Object> map,String name,Object value) {
		Object current = map.get(name);
		if( current == null ) {
			map.put(name,value);
		} else if( current instanceof List ) {
			List list = (List)current;
			list.add(value);
		} else {
			List list = new ArrayList();
			list.add(current);
			list.add(value);
			map.put(name,list);
		}
	}

	static String toString(byte[] a,String charset) throws UnsupportedEncodingException {
		if( charset != null )
			return new String(a,charset);
		char[] ac = new char[a.length];
		for( int i=0; i<a.length; i++ ) {
			ac[i] = (char)a[i];
		}
		return new String(ac);
	}

	static byte[] toBytes(String s) {
		char[] ac = s.toCharArray();
		byte[] a = new byte[ac.length];
		for( int i=0; i<ac.length; i++ ) {
			a[i] = (byte)ac[i];
		}
		return a;
	}

	private Util() {}  // never
}
