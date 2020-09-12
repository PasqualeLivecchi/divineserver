package luan;

import java.lang.reflect.Array;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Set;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import luan.modules.JavaLuan;
import luan.modules.PackageLuan;
import luan.modules.IoLuan;
import luan.modules.logging.LuanLogger;
import luan.impl.LuanCompiler;


public final class Luan implements LuanCloneable {
	private static final Logger logger = LoggerFactory.getLogger(Luan.class);

	private final List<LuanClosure> stack = new ArrayList<LuanClosure>();
	private Map registry;
	private boolean isLocked = false;

	public Luan() {
		registry = new HashMap();
	}

	private Luan(Luan luan) {}

	@Override public Luan shallowClone() {
		return new Luan(this);
	}

	@Override public void deepenClone(LuanCloneable dc,LuanCloner cloner) {
		Luan clone = (Luan)dc;
		clone.registry = cloner.clone(registry);
		if( cloner.type == LuanCloner.Type.INCREMENTAL )
			isLocked = true;
	}

	public LuanClosure peek() {
		return peek(1);
	}

	public LuanClosure peek(int i) {
		int n = stack.size();
		return n < i ? null : stack.get(n-i);
	}

	void push(LuanClosure closure) {
		if( isLocked )
			throw new RuntimeException(this+" is locked "+closure);
		stack.add(closure);
	}

	void pop() {
		stack.remove(stack.size()-1);
	}

	public Map registry() {
		return registry;
	}

	public Object eval(String cmd,Object... args) throws LuanException {
		return load(cmd,"eval",false).call(args);
	}

	public Object require(String modName) throws LuanException {
		return PackageLuan.require(this,modName);
	}

	public static String luanToString(Object obj) throws LuanException {
		if( obj instanceof LuanTable ) {
			LuanTable tbl = (LuanTable)obj;
			return tbl.toStringLuan();
		}
		if( obj == null )
			return "nil";
		if( obj instanceof Number )
			return Luan.toString((Number)obj);
		if( obj instanceof byte[] )
			return "binary: " + Integer.toHexString(obj.hashCode());
		return obj.toString();
	}

	public Object index(Object obj,Object key) throws LuanException {
		if( obj instanceof LuanTable ) {
			LuanTable tbl = (LuanTable)obj;
			return tbl.get(key);
		}
		if( obj != null && peek().javaOk )
			return JavaLuan.__index(this,obj,key);
		throw new LuanException("attempt to index a " + Luan.type(obj) + " value" );
	}


	public static boolean isLessThan(Object o1,Object o2) throws LuanException {
		if( o1 instanceof Number && o2 instanceof Number ) {
			Number n1 = (Number)o1;
			Number n2 = (Number)o2;
			return n1.doubleValue() < n2.doubleValue();
		}
		if( o1 instanceof String && o2 instanceof String ) {
			String s1 = (String)o1;
			String s2 = (String)o2;
			return s1.compareTo(s2) < 0;
		}
		LuanFunction fn = getBinHandler("__lt",o1,o2);
		if( fn != null )
			return Luan.checkBoolean( Luan.first(fn.call(o1,o2)) );
		throw new LuanException( "attempt to compare " + Luan.type(o1) + " with " + Luan.type(o2) );
	}

	public static LuanFunction getBinHandler(String op,Object o1,Object o2) throws LuanException {
		if( o1 instanceof LuanTable ) {
			LuanFunction f1 = getHandlerFunction(op,(LuanTable)o1);
			if( f1 != null )
				return f1;
		}
		return o2 instanceof LuanTable ? getHandlerFunction(op,(LuanTable)o2) : null;
	}

	public static LuanFunction getHandlerFunction(String op,LuanTable t) throws LuanException {
		Object f = t.getHandler(op);
		if( f == null )
			return null;
		return Luan.checkFunction(f);
	}

	public LuanTable toTable(Object obj) {
		if( obj == null )
			return null;
		if( obj instanceof LuanTable )
			return (LuanTable)obj;
		if( obj instanceof List ) {
			return new LuanTable(this,(List)obj);
		}
		if( obj instanceof Map ) {
			return new LuanTable(this,(Map)obj);
		}
		if( obj instanceof Set ) {
			return new LuanTable(this,(Set)obj);
		}
		Class cls = obj.getClass();
		if( cls.isArray() ) {
			if( cls.getComponentType().isPrimitive() ) {
				int len = Array.getLength(obj);
				List list = new ArrayList();
				for( int i=0; i<len; i++ ) {
					list.add(Array.get(obj,i));
				}
				return new LuanTable(this,list);
			} else {
				Object[] a = (Object[])obj;
				return new LuanTable(this,Arrays.asList(a));
			}
		}
		return null;
	}



	// static

	public static void main(String[] args) throws LuanException {
		Luan luan = new Luan();
		LuanFunction fn = loadClasspath(luan,"luan/cmd_line.luan");
		fn.call((Object[])args);
	}

	public static LuanFunction loadClasspath(Luan luan,String classpath)
		throws LuanException
	{
		try {
			String src = IoLuan.classpath(luan,classpath).read_text();
			return luan.load(src,"classpath:"+classpath,true);
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Object first(Object obj) {
		if( !(obj instanceof Object[]) )
			return obj;
		Object[] a = (Object[])obj;
		return a.length==0 ? null : a[0];
	}

	public static Object[] array(Object obj) {
		return obj instanceof Object[] ? (Object[])obj : new Object[]{obj};
	}

	public static String type(Object obj) {
		if( obj == null )
			return "nil";
		if( obj instanceof String )
			return "string";
		if( obj instanceof Boolean )
			return "boolean";
		if( obj instanceof Number )
			return "number";
		if( obj instanceof LuanTable )
			return "table";
		if( obj instanceof LuanFunction )
			return "function";
		if( obj instanceof byte[] )
			return "binary";
		return "java";
	}

	public static String toString(Number n) {
		if( n instanceof Integer )
			return n.toString();
		int i = n.intValue();
		if( i == n.doubleValue() )
			return Integer.toString(i);
		String s = n.toString();
		int iE = s.indexOf('E');
		String ending  = null;
		if( iE != -1 ) {
			ending = s.substring(iE);
			s = s.substring(0,iE);
		}
		if( s.endsWith(".0") )
			s = s.substring(0,s.length()-2);
		if( ending != null )
			s += ending;
		return s;
	}

	public static Integer asInteger(Object obj) {
		if( obj instanceof Integer )
			return (Integer)obj;
		if( !(obj instanceof Number) )
			return null;
		Number n = (Number)obj;
		int i = n.intValue();
		return i==n.doubleValue() ? Integer.valueOf(i) : null;
	}

	public static Long asLong(Object obj) {
		if( obj instanceof Long )
			return (Long)obj;
		if( !(obj instanceof Number) )
			return null;
		Number n = (Number)obj;
		long i = n.longValue();
		return i==n.doubleValue() ? Long.valueOf(i) : null;
	}

	public static Float asFloat(Object obj) {
		if( obj instanceof Float )
			return (Float)obj;
		if( !(obj instanceof Number) )
			return null;
		Number n = (Number)obj;
		float i = n.floatValue();
		return i==n.doubleValue() ? Float.valueOf(i) : null;
	}

	public static Double asDouble(Object obj) {
		if( obj instanceof Double )
			return (Double)obj;
		if( !(obj instanceof Number) )
			return null;
		Number n = (Number)obj;
		double i = n.doubleValue();
		return Double.valueOf(i);
	}

	public static String stringEncode(String s) {
		s = s.replace("\\","\\\\");
		s = s.replace("\u0007","\\a");
		s = s.replace("\b","\\b");
		s = s.replace("\f","\\f");
		s = s.replace("\n","\\n");
		s = s.replace("\r","\\r");
		s = s.replace("\t","\\t");
		s = s.replace("\u000b","\\v");
		s = s.replace("\"","\\\"");
		s = s.replace("\'","\\'");
		return s;
	}


	public static Boolean checkBoolean(Object obj) throws LuanException {
		if( obj instanceof Boolean )
			return (Boolean)obj;
		throw new LuanException("attempt to use a " + Luan.type(obj) + " value as a boolean" );
	}

	public static String checkString(Object obj) throws LuanException {
		if( obj instanceof String )
			return (String)obj;
		throw new LuanException("attempt to use a " + Luan.type(obj) + " value as a string" );
	}

	public static LuanFunction checkFunction(Object obj) throws LuanException {
		if( obj instanceof LuanFunction )
			return (LuanFunction)obj;
		throw new LuanException("attempt to call a " + Luan.type(obj) + " value" );
	}

	public LuanFunction load(String text,String sourceName,boolean persist,LuanTable env)
		throws LuanException
	{
		return LuanCompiler.compile(this,text,sourceName,persist,env);
	}

	public LuanFunction load(String text,String sourceName,boolean persist)
		throws LuanException
	{
		return load(text,sourceName,persist,null);
	}

	public static Object toJava(Object obj) throws LuanException {
		if( !(obj instanceof LuanTable) )
			return obj;
		LuanTable tbl = (LuanTable)obj;
		if( !tbl.isMap() ) {
			List list = new ArrayList();
			for( Object el : tbl.asList() ) {
				list.add( toJava(el) );
			}
			return list;
		} else {
			Map map = new LinkedHashMap();
			Iterator<Map.Entry> iter = tbl.rawIterator();
			while( iter.hasNext() ) {
				Map.Entry entry = iter.next();
				map.put( toJava(entry.getKey()), toJava(entry.getValue()) );
			}
			return map;
		}
	}


	// security

	public interface Security {
		public void check(Luan luan,LuanClosure closure,String op,Object... args) throws LuanException;
	}

	private static String SECURITY_KEY = "Luan.Security";

	public static void checkSecurity(Luan luan,String op,Object... args) throws LuanException {
		check(luan,1,op,args);
	}

	public static void checkCallerSecurity(Luan luan,String op,Object... args) throws LuanException {
		check(luan,2,op,args);
	}

	private static void check(Luan luan,int i,String op,Object... args) throws LuanException {
		Security s = (Security)luan.registry().get(SECURITY_KEY);
		if( s!=null )
			s.check(luan,luan.peek(i),op,args);
	}

	public static Security setSecurity(Luan luan,Security s) {
		return (Security)luan.registry().put(SECURITY_KEY,s);
	}

}
