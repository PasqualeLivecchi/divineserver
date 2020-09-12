package luan;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;
import java.util.Collection;


public final class LuanJavaFunction extends LuanFunction {
	private final JavaMethod method;
	private Object obj;
	private final RtnConverter rtnConverter;
	private final boolean takesLuan;
	private final ArgConverter[] argConverters;
	private final Class varArgCls;

	public LuanJavaFunction(Luan luan,Method method,Object obj) {
		this( luan, JavaMethod.of(method), obj );
	}

	public LuanJavaFunction(Luan luan,Constructor constr,Object obj) {
		this( luan, JavaMethod.of(constr), obj );
	}

	private LuanJavaFunction(Luan luan,JavaMethod method,Object obj) {
		super(luan);
		this.method = method;
		this.obj = obj;
		this.rtnConverter = getRtnConverter(method);
		this.takesLuan = takesLuan(method);
		this.argConverters = getArgConverters(takesLuan,method);
		if( method.isVarArgs() ) {
			Class[] paramTypes = method.getParameterTypes();
			this.varArgCls = paramTypes[paramTypes.length-1].getComponentType();
		} else {
			this.varArgCls = null;
		}
		if( !takesLuan )
			dontClone();
	}

	@Override public String toString() {
		return "java-function: " + method;
	}

	public int getParameterCount() {
		return argConverters.length;
	}

	public boolean isVarArgs() {
		return method.isVarArgs();
	}

	@Override public Object call(Object[] args) throws LuanException {
		try {
			args = fixArgs(args);
			return doCall(args);
		} catch(IllegalArgumentException e) {
			checkArgs(args);
			throw e;
		}
	}

	public Object rawCall(Object[] args) throws LuanException {
		args = fixArgs(args);
		return doCall(args);
	}

	private Object doCall(Object[] args) throws LuanException {
		Object rtn;
		try {
			rtn = method.invoke(obj,args);
		} catch(IllegalAccessException e) {
			throw new RuntimeException("method = "+method,e);
		} catch(InvocationTargetException e) {
			Throwable cause = e.getCause();
			if( cause instanceof Error )
				throw (Error)cause;
			if( cause instanceof LuanException )
				throw (LuanException)cause;
			throw new LuanException(cause);
		} catch(InstantiationException e) {
			throw new RuntimeException(e);
		}
		return rtnConverter.convert(rtn);
	}

	private static final Map primitiveMap = new HashMap();
	static {
		primitiveMap.put(Boolean.TYPE,Boolean.class);
		primitiveMap.put(Character.TYPE,Character.class);
		primitiveMap.put(Byte.TYPE,Byte.class);
		primitiveMap.put(Short.TYPE,Short.class);
		primitiveMap.put(Integer.TYPE,Integer.class);
		primitiveMap.put(Long.TYPE,Long.class);
		primitiveMap.put(Float.TYPE,Float.class);
		primitiveMap.put(Double.TYPE,Double.class);
		primitiveMap.put(Void.TYPE,Void.class);
	}

	private void checkArgs(Object[] args) throws LuanException {
		Class[] a = method.getParameterTypes();
		int start = takesLuan ? 1 : 0;
		for( int i=start; i<a.length; i++ ) {
			Class paramType = a[i];
			Class type = paramType;
			if( type.isPrimitive() )
				type = (Class)primitiveMap.get(type);
			Object arg = args[i];
			if( !type.isInstance(arg) ) {
				String expected;
				if( i==a.length-1 && method.isVarArgs() )
					expected = fixType(paramType.getComponentType().getSimpleName())+"...";
				else
					expected = fixType(paramType.getSimpleName());
				if( arg==null ) {
					if( paramType.isPrimitive() )
						throw new LuanException("bad argument #"+(i+1-start)+" ("+expected+" expected, got nil)");
				} else {
					String got;
					if( arg instanceof LuanFunction ) {
						got = "function";
					} else {
						got = fixType(arg.getClass().getSimpleName());
						if( got.equals("") )
							got = arg.getClass().toString();
					}
					throw new LuanException("bad argument #"+(i+1-start)+" ("+expected+" expected, got "+got+")");
				}
			}
		}
	}

	private static String fixType(String type) {
		if( type.equals("byte[]") )
			return "binary";
		if( type.equals("Double") )
			return "number";
		if( type.equals("LuanTable") )
			return "table";
		if( type.equals("Boolean") )
			return "boolean";
		if( type.equals("String") )
			return "string";
		if( type.equals("LuanClosure") )
			return "function";
		if( type.equals("LuanJavaFunction") )
			return "function";
		return type;
	}

	private Object[] fixArgs(Object[] args) throws LuanException {
		int n = argConverters.length;
		Object[] rtn;
		int start = 0;
		if( !takesLuan && varArgCls==null && args.length == n ) {
			rtn = args;
		} else {
			if( takesLuan )
				n++;
			rtn = new Object[n];
			if( takesLuan ) {
				rtn[start++] = luan();
			}
			n = argConverters.length;
			if( varArgCls == null ) {
				for( int i=n; i<args.length; i++ ) {
					if( args[i] !=  null )
						throw new LuanException("too many arguments");
				}
			} else {
				n--;
				if( args.length < argConverters.length ) {
					rtn[rtn.length-1] = Array.newInstance(varArgCls,0);
				} else {
					int len = args.length - n;
					Object varArgs = Array.newInstance(varArgCls,len);
					ArgConverter ac = argConverters[n];
					for( int i=0; i<len; i++ ) {
						Array.set( varArgs, i, ac.convert(args[n+i]) );
					}
					rtn[rtn.length-1] = varArgs;
				}
			}
			System.arraycopy(args,0,rtn,start,Math.min(args.length,n));
		}
		for( int i=0; i<n; i++ ) {
			rtn[start+i] = argConverters[i].convert(rtn[start+i]);
		}
		return rtn;
	}


	private interface RtnConverter {
		public Object convert(Object obj);
	}

	private static final RtnConverter RTN_NOTHING = new RtnConverter() {
		@Override public Object[] convert(Object obj) {
			return NOTHING;
		}
	};

	private static final RtnConverter RTN_SAME = new RtnConverter() {
		@Override public Object convert(Object obj) {
			return obj;
		}
	};
/*
	private static final RtnConverter RTN_ARRAY = new RtnConverter() {
		@Override public Object convert(Luan luan,Object obj) {
			if( obj == null )
				return null;
			Object[] a = new Object[Array.getLength(obj)];
			for( int i=0; i<a.length; i++ ) {
				a[i] = Array.get(obj,i);
			}
			return new LuanTable(luan,new ArrayList<Object>(Arrays.asList(a)));
		}
	};
*/
	private static RtnConverter getRtnConverter(JavaMethod m) {
		Class rtnType = m.getReturnType();
		if( rtnType == Void.TYPE )
			return RTN_NOTHING;
/*
		if( !m.isLuan() && rtnType.isArray() && !rtnType.getComponentType().isPrimitive() ) {
			return RTN_ARRAY;
		}
*/
		return RTN_SAME;
	}

	private interface ArgConverter {
		public Object convert(Object obj) throws LuanException;
	}

	private static final ArgConverter ARG_SAME = new ArgConverter() {
		@Override public Object convert(Object obj) {
			return obj;
		}
		@Override public String toString() {
			return "ARG_SAME";
		}
	};

	private static final ArgConverter ARG_DOUBLE = new ArgConverter() {
		@Override public Object convert(Object obj) {
			if( obj instanceof Double )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				return n.doubleValue();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_DOUBLE";
		}
	};

	private static final ArgConverter ARG_FLOAT = new ArgConverter() {
		@Override public Object convert(Object obj) {
			if( obj instanceof Float )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				return n.floatValue();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_FLOAT";
		}
	};

	private static final ArgConverter ARG_LONG = new ArgConverter() {
		@Override public Object convert(Object obj) {
			if( obj instanceof Long )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				long r = n.longValue();
				if( r==n.doubleValue() )
					return r;
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_LONG";
		}
	};

	private static final ArgConverter ARG_INTEGER = new ArgConverter() {
		@Override public Object convert(Object obj) {
			if( obj instanceof Integer )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				int r = n.intValue();
				if( r==n.doubleValue() )
					return r;
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_INTEGER";
		}
	};

	private static final ArgConverter ARG_SHORT = new ArgConverter() {
		@Override public Object convert(Object obj) {
			if( obj instanceof Short )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				short r = n.shortValue();
				if( r==n.doubleValue() )
					return r;
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_SHORT";
		}
	};

	private static final ArgConverter ARG_BYTE = new ArgConverter() {
		@Override public Object convert(Object obj) {
			if( obj instanceof Byte )
				return obj;
			if( obj instanceof Number ) {
				Number n = (Number)obj;
				byte r = n.byteValue();
				if( r==n.doubleValue() )
					return r;
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_BYTE";
		}
	};
/*
	private static final ArgConverter ARG_TABLE = new ArgConverter() {
		@Override public Object convert(Luan luan,Object obj) {
			LuanTable tbl = luan.toTable(obj);
			return tbl!=null ? tbl : obj;
		}
		@Override public String toString() {
			return "ARG_TABLE";
		}
	};
*/
	private static final ArgConverter ARG_MAP = new ArgConverter() {
		@Override public Object convert(Object obj) throws LuanException {
			if( obj instanceof LuanTable ) {
				LuanTable t = (LuanTable)obj;
				return t.asMap();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_MAP";
		}
	};

	private static final ArgConverter ARG_LIST = new ArgConverter() {
		@Override public Object convert(Object obj) {
			if( obj instanceof LuanTable ) {
				LuanTable t = (LuanTable)obj;
				if( t.isList() )
					return t.asList();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_LIST";
		}
	};

	private static final ArgConverter ARG_SET = new ArgConverter() {
		@Override public Object convert(Object obj) throws LuanException {
			if( obj instanceof LuanTable ) {
				LuanTable t = (LuanTable)obj;
				if( t.isSet() )
					return t.asSet();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_SET";
		}
	};

	private static final ArgConverter ARG_COLLECTION = new ArgConverter() {
		@Override public Object convert(Object obj) throws LuanException {
			if( obj instanceof LuanTable ) {
				LuanTable t = (LuanTable)obj;
				if( t.isList() )
					return t.asList();
				if( t.isSet() )
					return t.asSet();
			}
			return obj;
		}
		@Override public String toString() {
			return "ARG_COLLECTION";
		}
	};

	private static class ArgArray implements ArgConverter {
		private final Object[] a;

		ArgArray(Class cls) {
			a = (Object[])Array.newInstance(cls.getComponentType(),0);
		}

		@Override public Object convert(Object obj) {
			if( obj instanceof LuanTable ) {
				LuanTable t = (LuanTable)obj;
				if( t.isList() ) {
					try {
						return t.asList().toArray(a);
					} catch(ArrayStoreException e) {}
				}
			}
			return obj;
		}
	}

	private static boolean takesLuan(JavaMethod m) {
		Class[] paramTypes = m.getParameterTypes();
		return paramTypes.length > 0 && paramTypes[0].equals(Luan.class);
	}

	private static ArgConverter[] getArgConverters(boolean takesLuan,JavaMethod m) {
		final boolean isVarArgs = m.isVarArgs();
		Class[] paramTypes = m.getParameterTypes();
		if( takesLuan ) {
			Class[] t = new Class[paramTypes.length-1];
			System.arraycopy(paramTypes,1,t,0,t.length);
			paramTypes = t;
		}
		ArgConverter[] a = new ArgConverter[paramTypes.length];
		for( int i=0; i<a.length; i++ ) {
			Class paramType = paramTypes[i];
			if( isVarArgs && i == a.length-1 )
				paramType = paramType.getComponentType();
			a[i] = getArgConverter(paramType);
		}
		return a;
	}

	private static ArgConverter getArgConverter(Class cls) {
		if( cls == Double.TYPE || cls.equals(Double.class) )
			return ARG_DOUBLE;
		if( cls == Float.TYPE || cls.equals(Float.class) )
			return ARG_FLOAT;
		if( cls == Long.TYPE || cls.equals(Long.class) )
			return ARG_LONG;
		if( cls == Integer.TYPE || cls.equals(Integer.class) )
			return ARG_INTEGER;
		if( cls == Short.TYPE || cls.equals(Short.class) )
			return ARG_SHORT;
		if( cls == Byte.TYPE || cls.equals(Byte.class) )
			return ARG_BYTE;
//		if( cls.equals(LuanTable.class) )
//			return ARG_TABLE;
		if( cls.equals(Map.class) )
			return ARG_MAP;
		if( cls.equals(List.class) )
			return ARG_LIST;
		if( cls.equals(Set.class) )
			return ARG_SET;
		if( cls.equals(Collection.class) )
			return ARG_COLLECTION;
		if( cls.isArray() && !cls.getComponentType().isPrimitive() )
			return new ArgArray(cls);
		return ARG_SAME;
	}



	private static abstract class JavaMethod {
		abstract boolean isVarArgs();
		abstract Class[] getParameterTypes();
		abstract Object invoke(Object obj,Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException;
		abstract Class getReturnType();
		abstract String getName();
	
		static JavaMethod of(final Method m) {
			return new JavaMethod() {
				@Override boolean isVarArgs() {
					return m.isVarArgs();
				}
				@Override Class[] getParameterTypes() {
					return m.getParameterTypes();
				}
				@Override Object invoke(Object obj,Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
				{
					return m.invoke(obj,args);
				}
				@Override Class getReturnType() {
					return m.getReturnType();
				}
				@Override public String getName() {
					return m.getName();
				}
				@Override public String toString() {
					return m.toString();
				}
			};
		}
	
		static JavaMethod of(final Constructor c) {
			return new JavaMethod() {
				@Override boolean isVarArgs() {
					return c.isVarArgs();
				}
				@Override Class[] getParameterTypes() {
					return c.getParameterTypes();
				}
				@Override Object invoke(Object obj,Object... args)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException
				{
					return c.newInstance(args);
				}
				@Override Class getReturnType() {
					return c.getDeclaringClass();
				}
				@Override public String getName() {
					return c.getName();
				}
				@Override public String toString() {
					return c.toString();
				}
			};
		}
	
	}

}
