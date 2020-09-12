package goodjava.lucene.api;

import java.io.IOException;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;


public abstract class GoodCollector extends Collector {
	private int docBase;

	public boolean acceptsDocsOutOfOrder() {
		return true;
	}

	public void setScorer(Scorer scorer) {}

	public void setNextReader(AtomicReaderContext context) {
		this.docBase = context.docBase;
	}

	public void collect(int doc) throws IOException {
		collectDoc(docBase+doc);
	}

	public abstract void collectDoc(int doc) throws IOException;
}
