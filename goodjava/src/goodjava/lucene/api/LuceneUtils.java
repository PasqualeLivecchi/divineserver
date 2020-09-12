package goodjava.lucene.api;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.NumericUtils;


public final class LuceneUtils {
	private LuceneUtils() {}  // never

	public static Object getValue(IndexableField ifld) {
		BytesRef br = ifld.binaryValue();
		if( br != null )
			return br.bytes;
		Number n = ifld.numericValue();
		if( n != null )
			return n;
		String s = ifld.stringValue();
		if( s != null )
			return s;
		throw new RuntimeException("invalid field type for "+ifld);
	}

	public static Map<String,Object> toMap(Document doc) {
		if( doc==null )
			return null;
		Map<String,Object> map = new HashMap<String,Object>();
		for( IndexableField ifld : doc ) {
			String name = ifld.name();
			Object value = getValue(ifld);
			Object old = map.get(name);
			if( old == null ) {
				map.put(name,value);
			} else {
				List list;
				if( old instanceof List ) {
					list = (List)old;
				} else {
					list = new ArrayList();
					list.add(old);
					map.put(name,list);
				}
				list.add(value);
			}
		}
		return map;
	}

	public static Term term(String name,Object value) {
		if( value instanceof String ) {
			return new Term(name,(String)value);
		} else if( value instanceof Long ) {
			BytesRef br = new BytesRef();
			NumericUtils.longToPrefixCoded((Long)value,0,br);
			return new Term(name,br);
		} else if( value instanceof Integer ) {
			BytesRef br = new BytesRef();
			NumericUtils.intToPrefixCoded((Integer)value,0,br);
			return new Term(name,br);
		} else
			throw new RuntimeException("invalid value type "+value.getClass()+"' for term '"+name+"'");
	}

}
