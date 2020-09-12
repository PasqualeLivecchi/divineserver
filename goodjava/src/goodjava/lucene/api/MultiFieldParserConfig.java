package goodjava.lucene.api;

import java.util.Map;
import java.util.Collections;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import goodjava.lucene.queryparser.MultiFieldParser;
import goodjava.lucene.queryparser.FieldParser;
import goodjava.lucene.queryparser.StringFieldParser;


public class MultiFieldParserConfig implements GoodIndexWriterConfig {
	private final MultiFieldParser mfp;

	public MultiFieldParserConfig(MultiFieldParser mfp) {
		this.mfp = mfp;
	}

	public final boolean isIndexed(String fieldName) {
		return mfp.fields.containsKey(fieldName);
	}

	public final Analyzer getAnalyzer(String fieldName) {
		FieldParser fp = mfp.fields.get(fieldName);
		if( !(fp instanceof StringFieldParser) )
			return null;
		StringFieldParser sfp = (StringFieldParser)fp;
		Analyzer analyzer = sfp.analyzer;
		return analyzer instanceof KeywordAnalyzer ? null : analyzer;
	}

	private static final MoreFieldInfo noMoreFieldInfo = new MoreFieldInfo(Collections.emptyMap(),Collections.emptyMap());

	public MoreFieldInfo getMoreFieldInfo(Map<String,Object> storedFields) {
		return noMoreFieldInfo;
	}
}
