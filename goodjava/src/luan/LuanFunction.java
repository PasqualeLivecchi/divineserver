package luan;


public abstract class LuanFunction implements LuanCloneable, Cloneable {
	private Luan luan;
	private LuanCloner cloner;
	private boolean clone;

	public LuanFunction(Luan luan) {
		if( luan==null )  throw new NullPointerException();
		this.luan = luan;
		this.clone = true;
	}

	public LuanFunction(boolean clone) {
		this.clone = clone;
	}

	// for LuanJavaFunction
	void dontClone() {
		luan = null;
		clone = false;
	}

	@Override public LuanFunction shallowClone() {
		if( !clone )
			return this;
		check();
		try {
			return (LuanFunction)clone();
		} catch(CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	private void check() {
		if( cloner != null ) {
			completeClone(this,cloner);
			cloner = null;
		}
	}

	@Override public void deepenClone(LuanCloneable dc,LuanCloner cloner) {
		if( !clone )
			return;
		LuanFunction clone = (LuanFunction)dc;
		switch( cloner.type ) {
		case COMPLETE:
			completeClone(clone,cloner);
			return;
		case INCREMENTAL:
			clone.cloner = cloner;
			return;
		}
	}

	protected void completeClone(LuanFunction clone,LuanCloner cloner) {
		clone.luan = (Luan)cloner.clone(luan);
	}

	public Luan luan() {
		check();
		return luan;
	}

	public abstract Object call(Object... args) throws LuanException;

	public static final Object[] NOTHING = new Object[0];

	@Override public String toString() {
		return "function: " + Integer.toHexString(hashCode());
	}

}
