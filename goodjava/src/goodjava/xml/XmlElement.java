package goodjava.xml;

import java.util.Map;


public final class XmlElement {
	public final String name;
	public final Map<String,String> attributes;
	public final Object content;

	public XmlElement(String name,Map<String,String> attributes) {
		this.name = name;
		this.attributes = attributes;
		this.content = null;
	}

	public XmlElement(String name,Map<String,String> attributes,String content) {
		if( content == null )
			throw new IllegalArgumentException("content can't be null");
		this.name = name;
		this.attributes = attributes;
		this.content = content;
	}

	public XmlElement(String name,Map<String,String> attributes,XmlElement[] content) {
		if( content == null )
			throw new IllegalArgumentException("content can't be null");
		if( content.length == 0 )
			throw new IllegalArgumentException("content can't be empty");
		this.name = name;
		this.attributes = attributes;
		this.content = content;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb,0);
		return sb.toString();
	}

	private void toString(StringBuilder sb,int indented) {
		indent(sb,indented);
		sb.append( '<' );
		sb.append( name );
		for( Map.Entry<String,String> attribute : attributes.entrySet() ) {
			sb.append( ' ' );
			sb.append( attribute.getKey() );
			sb.append( "=\"" );
			sb.append( encode(attribute.getValue()) );
			sb.append( '"' );
		}
		if( content == null ) {
			sb.append( "/>\n" );
		} else if( content instanceof String ) {
			sb.append( '>' );
			String s = (String)content;
			sb.append( encode(s) );
			closeTag(sb,name);
		} else {
			sb.append( '>' );
			XmlElement[] elements = (XmlElement[])content;
			sb.append( '\n' );
			for( XmlElement element : elements ) {
				element.toString(sb,indented+1);
			}
			indent(sb,indented);
			closeTag(sb,name);
		}
	}

	private static void closeTag(StringBuilder sb,String name) {
		sb.append( "</" );
		sb.append( name );
		sb.append( ">\n" );
	}

	private static void indent(StringBuilder sb,int indented) {
		for( int i=0; i<indented; i++ ) {
			sb.append('\t');
		}
	}

	public static String encode(String s) {
		final char[] a = s.toCharArray();
		StringBuilder buf = new StringBuilder();
		for( char c : a ) {
			switch(c) {
			case '&':
				buf.append("&amp;");
				break;
			case '<':
				buf.append("&lt;");
				break;
			case '>':
				buf.append("&gt;");
				break;
			case '"':
				buf.append("&quot;");
				break;
			case '\'':
				buf.append("&apos;");
				break;
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}

}
