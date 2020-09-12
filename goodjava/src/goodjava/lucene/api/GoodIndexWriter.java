package goodjava.lucene.api;

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.search.Query;


public interface GoodIndexWriter {
	public void close() throws IOException;
	public void commit() throws IOException;
	public void rollback() throws IOException;
	public void deleteAll() throws IOException;
	public void deleteDocuments(Query query) throws IOException;
	public void addDocument(Map<String,Object> storedFields) throws IOException;
	public void updateDocument(String keyFieldName,Map<String,Object> storedFields) throws IOException;
	public void reindexDocuments(String keyFieldName,Query query) throws IOException;
}
