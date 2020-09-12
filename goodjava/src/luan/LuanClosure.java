package luan;

import luan.impl.Pointer;


public abstract class LuanClosure extends LuanFunction {
	public Pointer[] upValues;
	public boolean javaOk;
	public final String sourceName;

	public LuanClosure(Luan luan,Pointer[] upValues,boolean javaOk,String sourceName) throws LuanException {
		super(luan);
		this.upValues = upValues;
		this.javaOk = javaOk;
		this.sourceName = sourceName;
	}

	@Override protected void completeClone(LuanFunction dc,LuanCloner cloner) {
		LuanClosure clone = (LuanClosure)dc;
		clone.upValues = (Pointer[])cloner.clone(upValues);
		super.completeClone(dc,cloner);
	}

	@Override public final Object call(Object... args) throws LuanException {
		Luan luan = luan();
		luan.push(this);
		try {
			return doCall(luan,args);
		} catch(StackOverflowError e) {
			throw new LuanException( "stack overflow", e );
		} finally {
			luan.pop();
		}	
	}

	@Override public String toString() {
		return super.toString()+"="+sourceName;
	}

	public abstract Object doCall(Luan luan,Object[] args) throws LuanException;
}
