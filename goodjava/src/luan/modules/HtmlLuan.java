package luan.modules;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import luan.Luan;
import luan.LuanTable;
import luan.LuanException;


public final class HtmlLuan {

	public static String encode(String s) throws LuanException {
		Utils.checkNotNull(s);
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
			default:
				buf.append(c);
			}
		}
		return buf.toString();
	}

	private static final Pattern entityPtn = Pattern.compile(
		"&#(\\d+);"
	);

	public static String decode(String s) {
		StringBuffer buf = new StringBuffer();
		Matcher m = entityPtn.matcher(s);
		while( m.find() ) {
			String entity = new String(new char[]{(char)Integer.parseInt(m.group(1))});
			m.appendReplacement(buf,entity);
		}
		m.appendTail(buf);
		s = buf.toString();
		s = s.replace("&nbsp;"," ");
		s = s.replace("&quot;","\"");
		s = s.replace("&gt;",">");
		s = s.replace("&lt;","<");
		s = s.replace("&amp;","&");
		return s;
	}

	public static String quote(String s) {
		StringBuilder buf = new StringBuilder();
		buf.append('"');
		int i = 0;
		while(true) {
			int i2 = s.indexOf('"',i);
			if( i2 == -1 ) {
				buf.append(s.substring(i));
				break;
			} else {
				buf.append(s.substring(i,i2));
				buf.append("&quot;");
				i = i2 + 1;
			}
		}
		buf.append('"');
		return buf.toString();
	}

}
