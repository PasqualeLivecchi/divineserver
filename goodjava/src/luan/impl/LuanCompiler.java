package luan.impl;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import luan.Luan;
import luan.LuanFunction;
import luan.LuanException;
import luan.LuanTable;
import luan.LuanClosure;
import luan.modules.JavaLuan;
import luan.modules.PackageLuan;


public final class LuanCompiler {
	private static final Map<String,WeakReference<Class>> map = new HashMap<String,WeakReference<Class>>();

	public static LuanFunction compile(Luan luan,String sourceText,String sourceName,boolean persist,LuanTable env)
		throws LuanException
	{
		if( persist && env!=null )
			throw new LuanException("can't persist with env");
		Class fnClass = persist ? getClass(sourceText,sourceName) : getClass(sourceText,sourceName,env);
		boolean javaOk = false;
		if( env != null && env.closure != null )
			javaOk = env.closure.javaOk;
		LuanClosure closure;
		try {
			closure = (LuanClosure)fnClass.getConstructor(Luan.class,Boolean.TYPE,String.class).newInstance(luan,javaOk,sourceName);
		} catch(NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch(InstantiationException e) {
			throw new RuntimeException(e);
		} catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch(InvocationTargetException e) {
			throw new RuntimeException(e);
		}
		closure.upValues[0].o = PackageLuan.requireFn(luan);
		if( env != null ) {
			closure.upValues[1].o = env;
			env.closure = closure;
		}
		return closure;
	}

	private static synchronized Class getClass(String sourceText,String sourceName)
		throws LuanException
	{
		String key = sourceName + "~~~" + sourceText;
		WeakReference<Class> ref = map.get(key);
		if( ref != null ) {
			Class cls = ref.get();
			if( cls != null )
				return cls;
		}
		String fileName;
		try {
			byte[] a = MessageDigest.getInstance("MD5").digest(key.getBytes());
			String s = Base64.getUrlEncoder().encodeToString(a);
//System.err.println("qqqqqqqqqq "+s);
			fileName = s + ".luanc";
		} catch(NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		Compiled compiled = Compiled.load(fileName,key);
		//Compiled compiled = null;
		if( compiled==null ) {
			compiled = getCompiled(sourceText,sourceName,null);
			compiled.save(fileName,key);
		}
		Class cls = compiled.loadClass();
		map.put(key,new WeakReference<Class>(cls));
		return cls;
	}

	private static Class getClass(String sourceText,String sourceName,LuanTable env)
		throws LuanException
	{
		return getCompiled(sourceText,sourceName,env).loadClass();
	}

	private static Compiled getCompiled(String sourceText,String sourceName,LuanTable env)
		throws LuanException
	{
		LuanParser parser = new LuanParser(sourceText,sourceName);
		parser.addVar( "require" );
		if( env != null )  parser.addVar( "_ENV" );
		try {
			return parser.RequiredModule();
		} catch(ParseException e) {
//e.printStackTrace();
			throw new LuanException( e.getFancyMessage() );
		}
	}

	public static String toJava(String sourceText,String sourceName)
		throws LuanException
	{
		LuanParser parser = new LuanParser(sourceText,sourceName);
		parser.addVar( "require" );
		try {
			return parser.RequiredModuleSource();
		} catch(ParseException e) {
//e.printStackTrace();
			throw new LuanException( e.getFancyMessage() );
		}
	}

	private LuanCompiler() {}  // never
}
