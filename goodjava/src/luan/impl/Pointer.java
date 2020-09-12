package luan.impl;

import luan.LuanCloneable;
import luan.LuanCloner;


public final class Pointer implements LuanCloneable {
	public Object o;

	public Pointer() {}

	public Pointer(Object o) {
		this.o = o;
	}

	@Override public Pointer shallowClone() {
		return new Pointer();
	}

	@Override public void deepenClone(LuanCloneable clone,LuanCloner cloner) {
		((Pointer)clone).o = cloner.get(o);
	}
}
