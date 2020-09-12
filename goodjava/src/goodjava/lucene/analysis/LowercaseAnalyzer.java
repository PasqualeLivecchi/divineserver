package goodjava.lucene.analysis;

import java.io.Reader;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;


public final class LowercaseAnalyzer extends Analyzer {
	private final Version matchVersion;

	public LowercaseAnalyzer(Version matchVersion) {
		this.matchVersion = matchVersion;
	}

	protected TokenStreamComponents createComponents( String fieldName, Reader reader ) {
		Tokenizer source = new KeywordTokenizer(reader);
		TokenStream filter = new LowerCaseFilter(matchVersion,source);
		return new TokenStreamComponents(source,filter);
	}
}
