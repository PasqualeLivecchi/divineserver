package luan.modules;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;


public final class StringLuan {

	static int start(String s,int i) {
		int len = s.length();
		return i==0 ? 0 : i > 0 ? Math.min(i-1,len) : Math.max(len+i,0);
	}

	static int start(String s,Integer i,int dflt) {
		return i==null ? dflt : start(s,i);
	}

	static int end(String s,int i) {
		int len = s.length();
		return i==0 ? 0 : i > 0 ? Math.min(i,len) : Math.max(len+i+1,0);
	}

	static int end(String s,Integer i,int dflt) {
		return i==null ? dflt : end(s,i);
	}

	public static Integer[] unicode(String s,Integer i,Integer j) throws LuanException {
		Utils.checkNotNull(s);
		int start = start(s,i,0);
		int end = end(s,j,start+1);
		Integer[] chars = new Integer[end-start];
		for( int k=0; k<chars.length; k++ ) {
			chars[k] = (int)s.charAt(start+k);
		}
		return chars;
	}

	public static String char_(int... chars) {
		char[] a = new char[chars.length];
		for( int i=0; i<chars.length; i++ ) {
			a[i] = (char)chars[i];
		}
		return new String(a);
	}

	public static byte[] to_binary(String s) {
		return s.getBytes();
	}

	public static String lower(String s) throws LuanException {
		Utils.checkNotNull(s);
		return s.toLowerCase();
	}

	public static String upper(String s) throws LuanException {
		Utils.checkNotNull(s);
		return s.toUpperCase();
	}

	public static String trim(String s) throws LuanException {
		Utils.checkNotNull(s);
		return s.trim();
	}

	public static String reverse(String s) throws LuanException {
		Utils.checkNotNull(s);
		return new StringBuilder(s).reverse().toString();
	}

	public static String rep(String s,int n,String sep) {
		if( n < 1 )
			return "";
		StringBuilder buf = new StringBuilder(s);
		while( --n > 0 ) {
			if( sep != null )
				buf.append(sep);
			buf.append(s);
		}
		return buf.toString();
	}

	public static String sub(String s,int i,Integer j) throws LuanException {
		Utils.checkNotNull(s);
		int start = start(s,i);
		int end = end(s,j,s.length());
		return s.substring(start,end);
	}

	public static Object[] find(String s,String pattern,Integer init,Boolean plain) {
		int start = start(s,init,0);
		if( Boolean.TRUE.equals(plain) ) {
			int i = s.indexOf(pattern,start);
			return i == -1 ? null : new Integer[]{i+1,i+pattern.length()};
		}
		Matcher m = Pattern.compile(pattern).matcher(s);
		if( !m.find(start) )
			return null;
		int n = m.groupCount();
		Object[] rtn = new Object[2+n];
		rtn[0] = m.start() + 1;
		rtn[1] = m.end();
		for( int i=0; i<n; i++ ) {
			rtn[2+i] = m.group(i+1);
		}
		return rtn;
	}

	public static String[] match(String s,String pattern,Integer init) {
		int start = start(s,init,0);
		Matcher m = Pattern.compile(pattern).matcher(s);
		if( !m.find(start) )
			return null;
		int n = m.groupCount();
		if( n == 0 )
			return new String[]{m.group()};
		String[] rtn = new String[n];
		for( int i=0; i<n; i++ ) {
			rtn[i] = m.group(i+1);
		}
		return rtn;
	}

	public static LuanFunction gmatch(String s,String pattern) throws LuanException {
		Utils.checkNotNull(s);
		final Matcher m = Pattern.compile(pattern).matcher(s);
		return new LuanFunction(false) {
			@Override public Object call(Object[] args) {
				if( !m.find() )
					return null;
				final int n = m.groupCount();
				if( n == 0 )
					return m.group();
				String[] rtn = new String[n];
				for( int i=0; i<n; i++ ) {
					rtn[i] = m.group(i+1);
				}
				return rtn;
			}
		};
	}

	public static Object[] gsub(String s,String pattern,Object repl,Integer n) throws LuanException {
		Utils.checkNotNull(s);
		int max = n==null ? Integer.MAX_VALUE : n;
		final Matcher m = Pattern.compile(pattern).matcher(s);
		if( repl instanceof String ) {
			String replacement = (String)repl;
			int i = 0;
			StringBuffer sb = new StringBuffer();
			while( i<max && m.find() ) {
				m.appendReplacement(sb,replacement);
				i++;
			}
			m.appendTail(sb);
			return new Object[]{ sb.toString(), i };
		}
		if( repl instanceof LuanTable ) {
			LuanTable t = (LuanTable)repl;
			int i = 0;
			StringBuffer sb = new StringBuffer();
			while( i<max && m.find() ) {
				String match = m.groupCount()==0 ? m.group() : m.group(1);
				Object val = t.get(match);
				if( val != null ) {
					String replacement = Luan.luanToString(val);
					m.appendReplacement(sb,replacement);
				}
				i++;
			}
			m.appendTail(sb);
			return new Object[]{ sb.toString(), i };
		}
		if( repl instanceof LuanFunction ) {
			LuanFunction fn = (LuanFunction)repl;
			int i = 0;
			StringBuffer sb = new StringBuffer();
			while( i<max && m.find() ) {
				Object[] args;
				final int count = m.groupCount();
				if( count == 0 ) {
					args = new String[]{m.group()};
				} else {
					args = new String[count];
					for( int j=0; j<count; j++ ) {
						args[j] = m.group(j+1);
					}
				}
				Object val = Luan.first( fn.call(args) );
				if( val != null ) {
					String replacement = Luan.luanToString(val);
					m.appendReplacement(sb,replacement);
				}
				i++;
			}
			m.appendTail(sb);
			return new Object[]{ sb.toString(), i };
		}
		throw new LuanException( "bad argument #3 to 'gsub' (string/function/table expected)" );
	}

	// note - String.format() is too stupid to convert between ints and floats.
	public static String format(String format,Object... args) {
		return String.format(format,args);
	}

	public static String encode(String s) {
		return Luan.stringEncode(s);
	}

	public static Number to_number(String s,Integer base) throws LuanException {
		Utils.checkNotNull(s);
		try {
			if( base == null ) {
				return Double.valueOf(s);
			} else {
				return Long.valueOf(s,base);
			}
		} catch(NumberFormatException e) {}
		return null;
	}

	public static boolean matches(String s,String pattern) throws LuanException {
		Utils.checkNotNull(s);
		return Pattern.compile(pattern).matcher(s).find();
	}

	public static String[] split(String s,String pattern,Integer limit) throws LuanException {
		Utils.checkNotNull(s);
		int n = limit==null ? -1 : limit;
		return s.split(pattern,n);
	}

}
