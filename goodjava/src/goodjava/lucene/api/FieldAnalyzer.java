package goodjava.lucene.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.core.KeywordAnalyzer;


public final class FieldAnalyzer extends AnalyzerWrapper {
	private static final Analyzer defaultAnalyzer = new KeywordAnalyzer();
	private final Map<String,Analyzer> fieldAnalyzers = new ConcurrentHashMap<String,Analyzer>();

	public void add(String fieldName,Analyzer analyzer) {
		fieldAnalyzers.put(fieldName,analyzer);
	}

	public boolean isAdded(String fieldName) {
		return fieldAnalyzers.containsKey(fieldName);
	}

	protected Analyzer getWrappedAnalyzer(String fieldName) {
		Analyzer analyzer = fieldAnalyzers.get(fieldName);
/*
		if( analyzer == null )
			throw new RuntimeException("no analyzer for field: "+fieldName);
		return analyzer;
*/
		return analyzer!=null ? analyzer : defaultAnalyzer;
	}
}
