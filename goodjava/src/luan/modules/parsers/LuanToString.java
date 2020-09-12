package luan.modules.parsers;

import java.util.List;
import java.util.Map;
import luan.Luan;
import luan.LuanTable;
import luan.LuanException;
import luan.LuanRuntimeException;


public final class LuanToString {
	public boolean strict = false;
	public boolean numberTypes = false;
	public boolean compressed = false;

	public String toString(Object obj) throws LuanException {
		StringBuilder sb = new StringBuilder();
		toString(obj,sb,0);
		return sb.toString();
	}

	private void toString(Object obj,StringBuilder sb,int indented) throws LuanException {
		if( obj == null ) {
			sb.append( "nil" );
			return;
		}
		if( obj instanceof Boolean ) {
			sb.append( obj );
			return;
		}
		if( obj instanceof Number ) {
			toString((Number)obj,sb);
			return;
		}
		if( obj instanceof String ) {
			sb.append( '"' );
			sb.append( Luan.stringEncode((String)obj) );
			sb.append( '"' );
			return;
		}
		if( obj instanceof LuanTable ) {
			toString((LuanTable)obj,sb,indented);
			return;
		}
		if( strict )
			throw new LuanException("can't handle type "+Luan.type(obj));
		sb.append( '<' );
		sb.append( obj );
		sb.append( '>' );
	}

	private void toString(LuanTable tbl,StringBuilder sb,int indented) throws LuanException {
		List list = tbl.asList();
		Map map = tbl.rawMap();
		sb.append( '{' );
		boolean first = true;
		for( Object obj : list ) {
			if( !compressed )
				indent(sb,indented+1);
			else if( first )
				first = false;
			else
				sb.append( ',' );
			toString(obj,sb,indented+1);
		}
		for( Object obj : map.entrySet() ) {
			Map.Entry entry = (Map.Entry)obj;
			if( !compressed )
				indent(sb,indented+1);
			else if( first )
				first = false;
			else
				sb.append( ',' );
			toString(entry,sb,indented+1);
		}
		if( !compressed && (!list.isEmpty() || !map.isEmpty()) )
			indent(sb,indented);
		sb.append( '}' );
		return;
	}

	private void toString(Map.Entry entry,StringBuilder sb,int indented) throws LuanException {
		Object key = entry.getKey();
		if( key instanceof String && ((String)key).matches("[a-zA-Z_][a-zA-Z_0-9]*") ) {
			sb.append( (String)key );
		} else {
			sb.append( '[' );
			toString( key, sb, indented );
			sb.append( ']' );
		}
		sb.append( compressed ? "=" : " = " );
		toString( entry.getValue(), sb, indented );
	}

	private void indent(StringBuilder sb,int indented) {
		sb.append( '\n' );
		for( int i=0; i<indented; i++ ) {
			sb.append( '\t' );
		}
	}

	private void toString(Number n,StringBuilder sb) throws LuanException {
		if( numberTypes ) {
			sb.append( n.getClass().getSimpleName().toLowerCase() );
			sb.append( '(' );
		}
		sb.append( Luan.toString(n) );
		if( numberTypes )
			sb.append( ')' );
	}

	public static void addNumberTypes(LuanTable env) {
		try {
			LuanTable module = (LuanTable)env.luan().require("luan:Number.luan");
			env.put( "double", module.fn("double") );
			env.put( "float", module.fn("float") );
			env.put( "integer", module.fn("integer") );
			env.put( "long", module.fn("long") );
		} catch(LuanException e) {
			throw new LuanRuntimeException(e);
		}
	}
}
