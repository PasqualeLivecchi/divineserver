package luan.modules.parsers;

import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import goodjava.parser.ParseException;
import goodjava.xml.XmlElement;
import goodjava.xml.XmlParser;
import luan.Luan;
import luan.LuanTable;
import luan.LuanException;
import luan.modules.BasicLuan;


public final class Xml {

	public static String toString(LuanTable tbl) throws LuanException {
		XmlElement[] elements = elements(tbl);
		if( elements.length != 1 )
			throw new LuanException("XML must have 1 root element");
		return elements[0].toString();
	}

	private static final String ATTRIBUTES = "xml_attributes";
	private static final String TEXT = "xml_text";

	private static XmlElement[] elements(LuanTable tbl) throws LuanException {
		List<XmlElement> list = new ArrayList<XmlElement>();
		for( Map.Entry entry : tbl.iterable() ) {
			Object key = entry.getKey();
			if( !(key instanceof String) )
				throw new LuanException("XML key must be string");
			String name = (String)key;
			Object value = entry.getValue();
			if( name.equals(ATTRIBUTES) )
				continue;
			if( name.equals(TEXT) )
				throw new LuanException("Can't mix text and elements");
			LuanTable t = (LuanTable)value;
			if( t.isMap() ) {
				list.add( element(name,t) );
			} else {
				for( Object obj : t.asList() ) {
					list.add( element(name,obj) );
				}
			}
		}
		return list.toArray(new XmlElement[0]);
	}

	private static XmlElement element(String name,Object obj) throws LuanException {
		if( obj instanceof String ) {
			return new XmlElement( name, Collections.emptyMap(), (String)obj );
		}
		LuanTable t = (LuanTable)obj;
		Map<String,String> attributes = attributes(t);
		String s = (String)t.get(TEXT);
		if( s != null ) {
			return new XmlElement(name,attributes,s);
		} else {
			XmlElement[] elements = elements(t);
			if( elements.length==0 ) {
				return new XmlElement(name,attributes);
			} else {
				return new XmlElement(name,attributes,elements);
			}
		}
	}

	private static Map<String,String> attributes(LuanTable tbl) throws LuanException {
		Object obj = tbl.get(ATTRIBUTES);
		if( obj==null )
			return Collections.emptyMap();
		LuanTable t = (LuanTable)obj;
		Map<String,String> map = new LinkedHashMap<String,String>();
		for( Map.Entry entry : t.iterable() ) {
			String name =(String)entry.getKey();
			String value =(String)entry.getValue();
			map.put(name,value);
		}
		return map;
	}


	public static LuanTable parse(Luan luan,String s) throws ParseException, LuanException {
		XmlElement element = XmlParser.parse(s);
		LuanTable tbl = new LuanTable(luan);
		addElements(tbl,new XmlElement[]{element});
		return tbl;
	}

	private static LuanTable addElements(LuanTable tbl,XmlElement[] elements) throws LuanException {
		for( XmlElement element : elements ) {
			LuanTable t = new LuanTable(tbl.luan());
			if( !element.attributes.isEmpty() ) {
				LuanTable attrs = new LuanTable(tbl.luan());
				for( Map.Entry<String,String> entry : element.attributes.entrySet() ) {
					attrs.put(entry.getKey(),entry.getValue());
				}
				t.put( ATTRIBUTES, attrs );
			}
			if( element.content == null ) {
				// nothing
			} else if( element.content instanceof String ) {
				t.put( TEXT, element.content );
			} else {
				XmlElement[] els = (XmlElement[])element.content;
				addElements(t,els);
			}
			LuanTable old = (LuanTable)tbl.get(element.name);
			if( old==null ) {
				tbl.put(element.name,t);
			} else {
				if( !old.isList() ) {
					LuanTable list = new LuanTable(tbl.luan());
					list.rawAdd(old);
					old = list;
					tbl.put(element.name,old);
				}
				old.rawAdd(t);
			}
		}
		return tbl;
	}

}
