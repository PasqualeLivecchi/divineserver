package luan;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


public final class LuanException extends Exception implements LuanCloneable {
	private LuanTable table;
	private Map extra = new HashMap();

	public LuanException(String msg,Throwable cause) {
		super(msg,cause);
	}

	public LuanException(String msg) {
		super(msg);
	}

	public LuanException(Throwable cause) {
		super(cause);
	}

	@Override public LuanException shallowClone() {
		return new LuanException(getMessage(),getCause());
	}

	@Override public void deepenClone(LuanCloneable dc,LuanCloner cloner) {
		LuanException clone = (LuanException)dc;
		clone.table = (LuanTable)cloner.clone(table);
		clone.extra = (Map)cloner.clone(extra);
	}

	public void put(String key,Object value) throws LuanException {
		if( table == null ) {
			extra.put(key,value);
		} else {
			table.put(key,value);
		}
	}

	public LuanTable table(Luan luan) {
		if( table==null ) {
			try {
				LuanTable Boot = (LuanTable)luan.require("luan:Boot.luan");
				table = (LuanTable)Boot.fn("new_error_table").call(this );
				for( Object stupid : extra.entrySet() ) {
					Map.Entry entry = (Map.Entry)stupid;
					table.put( entry.getKey(), entry.getValue() );
				}
			} catch(LuanException e) {
				throw new RuntimeException(e);
			}
		}
		return table;
	}

	public void throwThis() throws LuanException {
		throw this;
	}

	public String getJavaStackTraceString() {
		return getJavaStackTraceString(this);
	}

	private static String getJavaStackTraceString(Throwable th) {
		StringWriter sw = new StringWriter();
		th.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	public static List<StackTraceElement> justLuan(StackTraceElement[] orig) {
		List<StackTraceElement> list = new ArrayList<StackTraceElement>();
		for( int i=0; i<orig.length; i++ ) {
			StackTraceElement ste = orig[i];
			if( !ste.getClassName().startsWith("luan.impl.EXP") )
				continue;
			list.add(ste);
			if( !ste.getMethodName().equals("doCall") )
				i++;
		}
		return list;
	}

	public static String toLuanString(StackTraceElement ste) {
		int line = ste.getLineNumber();
		String method = ste.getMethodName();
		boolean hasMethod = !method.equals("doCall");
		if( hasMethod ) {
			int i = method.indexOf('$');
			if( i != -1 ) {
				int n = Integer.parseInt(method.substring(i+1));
				line -= n;
				method = method.substring(0,i);
				if( method.equals("_") )
					hasMethod = false;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append( ste.getFileName() ).append( " line " ).append( line );
		if( hasMethod )
			sb.append( " in function '" ).append( method.substring(1) ).append( "'" );
		return sb.toString();
	}

	private StringBuilder luanStackTrace() {
		StringBuilder sb = new StringBuilder();
		sb.append( getMessage() );
		for( StackTraceElement ste : justLuan(getStackTrace()) ) {
			sb.append( "\n\t" ).append( toLuanString(ste) );
		}
		return sb;
	}

	public String getLuanStackTraceString() {
		StringBuilder sb = luanStackTrace();
		Throwable cause = getCause();
		if( cause != null )
			sb.append( "\nCaused by: " ).append( getJavaStackTraceString(cause) );
		return sb.toString();
	}

	@Override public void printStackTrace(PrintStream s) {
		s.print("Luan: ");
		s.println(luanStackTrace());
		s.print("Caused by: ");
		super.printStackTrace(s);
	}

	@Override public void printStackTrace(PrintWriter s) {
		s.print("Luan: ");
		s.println(luanStackTrace());
		s.print("Caused by: ");
		super.printStackTrace(s);
	}

}
