package luan;


public interface LuanCloneable {
	public LuanCloneable shallowClone();
	public void deepenClone(LuanCloneable clone,LuanCloner cloner);
}
