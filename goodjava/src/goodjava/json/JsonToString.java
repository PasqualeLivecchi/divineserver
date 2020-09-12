package goodjava.json;

import java.util.List;
import java.util.Map;
import java.util.Iterator;


public final class JsonToString {
	public boolean compressed = false;

	public static final class JsonException extends RuntimeException {
		private JsonException(String msg) {
			super(msg);
		}
	}

	public String toString(Object obj) throws JsonException {
		StringBuilder sb = new StringBuilder();
		toString(obj,sb,0);
		if( !compressed )
			sb.append('\n');
		return sb.toString();
	}

	private void toString(Object obj,StringBuilder sb,int indented) throws JsonException {
		if( obj == null || obj instanceof Boolean || obj instanceof Number ) {
			sb.append(obj);
			return;
		}
		if( obj instanceof String ) {
			toString((String)obj,sb);
			return;
		}
		if( obj instanceof List ) {
			toString((List)obj,sb,indented);
			return;
		}
		if( obj instanceof Map ) {
			toString((Map)obj,sb,indented);
			return;
		}
		throw new JsonException("can't handle type "+obj.getClass().getName());
	}

	private static void toString(final String s,StringBuilder sb) {
		sb.append('"');
		for( int i=0; i<s.length(); i++ ) {
			char c = s.charAt(i);
			switch(c) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				sb.append(c);
			}
		}
		sb.append('"');
	}

	public static String javascriptEncode(String s) {
		StringBuilder sb = new StringBuilder();
		for( int i=0; i<s.length(); i++ ) {
			char c = s.charAt(i);
			switch(c) {
			case '"':
				sb.append("\\\"");
				break;
			case '\'':  // added for javascript
				sb.append("\\'");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private void toString(List list,StringBuilder sb,int indented) {
		sb.append('[');
		if( !list.isEmpty() ) {
			indent(sb,indented+1);
			toString(list.get(0),sb,indented+1);
			final int n = list.size();
			for( int i=1; i<n; i++ ) {
				sb.append(',');
				indent(sb,indented+1);
				toString(list.get(i),sb,indented+1);
			}
			indent(sb,indented);
		}
		sb.append(']');
		return;
	}

	private void toString(Map map,StringBuilder sb,int indented) throws JsonException {
		sb.append('{');
		if( !map.isEmpty() ) {
			Iterator<Map.Entry> i = map.entrySet().iterator();
			indent(sb,indented+1);
			toString(i.next(),sb,indented+1);
			while( i.hasNext() ) {
				sb.append(',');
				indent(sb,indented+1);
				toString(i.next(),sb,indented+1);
			}
			indent(sb,indented);
		}
		sb.append('}');
	}

	private void toString(Map.Entry entry,StringBuilder sb,int indented) throws JsonException {
		Object key = entry.getKey();
		if( !(key instanceof String) )
			throw new JsonException("table keys must be strings but got "+key.getClass().getSimpleName()+" ("+key+"="+entry.getValue()+")");
		toString((String)key,sb);
		sb.append( compressed ? ":" : ": " );
		toString(entry.getValue(),sb,indented);
	}

	private void indent(StringBuilder sb,int indented) {
		if( compressed )
			return;
		sb.append('\n');
		for( int i=0; i<indented; i++ ) {
			sb.append('\t');
		}
	}

}
