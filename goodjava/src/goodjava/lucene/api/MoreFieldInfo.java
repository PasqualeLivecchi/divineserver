package goodjava.lucene.api;

import java.util.Map;


public final class MoreFieldInfo {
	public final Map<String,Object> unstoredFields;
	public final Map<String,Float> boosts;

	public MoreFieldInfo(Map<String,Object> unstoredFields,Map<String,Float> boosts) {
		this.unstoredFields = unstoredFields;
		this.boosts = boosts;
	}
}
