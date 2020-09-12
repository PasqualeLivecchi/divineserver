package luan.modules.lucene;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import luan.Luan;
import luan.LuanTable;
import luan.LuanFunction;
import luan.LuanException;
import luan.modules.Utils;
import luan.modules.parsers.LuanToString;
import luan.modules.parsers.LuanParser;
import goodjava.parser.ParseException;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;


final class PostgresBackup {
	private static final Logger logger = LoggerFactory.getLogger(PostgresBackup.class);

	final boolean wasCreated;
	private final String url;
	private final Properties props = new Properties();
	private final Connection con;
	private final PreparedStatement insertStmt;
	private final PreparedStatement updateStmt;
	private final PreparedStatement deleteStmt;
	private int trans = 0;
	private final LuanToString luanToString = new LuanToString();

	PostgresBackup(Luan luan,LuanTable spec)
		throws ClassNotFoundException, SQLException, LuanException
	{
		spec = new LuanTable(spec);
/*
		Class.forName("org.postgresql.Driver");
		url = "jdbc:postgresql://localhost:5432/luan";
		props.setProperty("user","postgres");
		props.setProperty("password","");
*/
		String cls = "org.postgresql.Driver";
		if( !Utils.removeRequiredString(spec,"class").equals(cls) )
			throw new LuanException( "parameter 'class' must be '"+cls+"'" );
		Class.forName(cls);
		url = Utils.removeRequiredString(spec,"url");
		props.setProperty( "user", Utils.removeRequiredString(spec,"user") );
		props.setProperty( "password", Utils.removeRequiredString(spec,"password") );
		Utils.checkEmpty(spec);

		con = newConnection();

		Statement stmt = con.createStatement();
		boolean hasTable = stmt.executeQuery(
			"select * from information_schema.tables where table_name='lucene'"
		).next();
		if( !hasTable ) {
			stmt.executeUpdate(
				"create table lucene ("
				+"	id integer not null primary key,"
				+"	data text not null"
				+")"
			);
		}
		stmt.close();
		wasCreated = !hasTable;

		insertStmt = con.prepareStatement(
			"insert into lucene (id,data) values (?,?)"
		);
		updateStmt = con.prepareStatement(
			"update lucene set data=? where id=?"
		);
		deleteStmt = con.prepareStatement(
			"delete from lucene where id=?"
		);

		luanToString.strict = true;
		luanToString.numberTypes = true;
	}

	Connection newConnection() throws SQLException {
		return DriverManager.getConnection(url,props);
	}

	void close() throws SQLException {
		insertStmt.close();
		updateStmt.close();
		deleteStmt.close();
		con.close();
	}

	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	void add(LuanTable doc) throws LuanException, SQLException {
		Long id = (Long)doc.get("id");
		String data = luanToString.toString(doc);
		insertStmt.setLong(1,id);
		insertStmt.setString(2,data);
		insertStmt.executeUpdate();
	}

	void update(LuanTable doc) throws LuanException, SQLException {
		Long id = (Long)doc.get("id");
		String data = luanToString.toString(doc);
		updateStmt.setString(1,data);
		updateStmt.setLong(2,id);
		int n = updateStmt.executeUpdate();
		if( n==0 ) {
			logger.error("update not found for id="+id+", trying add");
			add(doc);
		} else if( n!=1 )
			throw new RuntimeException();
	}

	void deleteAll() throws SQLException {
		Statement stmt = con.createStatement();
		stmt.executeUpdate("delete from lucene");
		stmt.close();
	}

	void delete(long id) throws SQLException, LuanException {
		deleteStmt.setLong(1,id);
		int n = deleteStmt.executeUpdate();
		if( n==0 )
			throw new LuanException("delete not found for id="+id);
	}

	void begin() throws SQLException {
		if( trans++ == 0 )
			con.setAutoCommit(false);
	}

	void commit() throws SQLException, LuanException {
		if( trans <= 0 )
			throw new LuanException("commit not in transaction");
		if( --trans == 0 )
			con.setAutoCommit(true);
	}

	void rollback() throws SQLException, LuanException {
		if( --trans != 0 )
			throw new LuanException("rollback failed trans="+trans);
		con.rollback();
		con.setAutoCommit(true);
	}

	void restoreLucene(LuceneIndex li,LuanFunction completer)
		throws LuanException, IOException, SQLException, ParseException
	{
		Luan luan = new Luan();
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("select data from lucene");
		while( rs.next() ) {
			String data = rs.getString("data");
			LuanTable doc = (LuanTable)LuanParser.parse(luan,data);
			li.restore(completer,doc);
		}
		stmt.close();
	}

	long maxId()
		throws LuanException, IOException, SQLException
	{
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("select max(id) as m from lucene");
		rs.next();
		long m = rs.getLong("m");
		stmt.close();
		return m;
	}

	final class Checker {
		private final Connection con;
		private final PreparedStatement pstmt;
		private final Luan luan = new Luan();

		Checker() throws SQLException {
			con = newConnection();
			con.setReadOnly(true);
			pstmt = con.prepareStatement(
				"select data from lucene where id=?"
			);
		}

		void close() throws SQLException {
			pstmt.close();
			con.close();
		}

		List<Long> getIds() throws SQLException {
			List<Long> ids = new ArrayList<Long>();
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("select id from lucene order by id");
			while( rs.next() ) {
				long id = rs.getLong("id");
				ids.add(id);
			}
			stmt.close();
			return ids;
		}

		LuanTable getDoc(long id) throws SQLException, ParseException {
			pstmt.setLong(1,id);
			ResultSet rs = pstmt.executeQuery();
			if( !rs.next() )
				return null;
			String data = rs.getString("data");
			LuanTable doc = (LuanTable)LuanParser.parse(luan,data);
			return doc;
		}
	}

	Checker newChecker() throws SQLException {
		return new Checker();
	}

}
