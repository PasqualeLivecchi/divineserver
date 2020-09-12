package goodjava.lucene.api;

import java.util.Map;
import org.apache.lucene.analysis.Analyzer;


public interface GoodIndexWriterConfig {
	public boolean isIndexed(String fieldName);
	public Analyzer getAnalyzer(String fieldName);
	public MoreFieldInfo getMoreFieldInfo(Map<String,Object> storedFields);
}
