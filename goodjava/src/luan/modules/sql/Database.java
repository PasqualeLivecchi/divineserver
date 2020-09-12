package luan.modules.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import goodjava.logging.Logger;
import goodjava.logging.LoggerFactory;
import luan.Luan;
import luan.LuanTable;
import luan.LuanException;
import luan.modules.Utils;


public final class Database {
	private static final Logger logger = LoggerFactory.getLogger(Database.class);

	public final Connection con;
	private final Map<String,PreparedStatement> pstmts = new HashMap<String,PreparedStatement>();
	private int fetchSize = 0;

	public Database(Connection con) {
		this.con = con;
	}

	public Database(LuanTable spec)
		throws LuanException, ClassNotFoundException, SQLException
	{
		spec = new LuanTable(spec);
		String cls = Utils.removeRequiredString(spec,"class");
		Class.forName(cls);
		String url = Utils.removeRequiredString(spec,"url");
		Properties props = new Properties();
		props.putAll(spec.asMap());
		this.con = DriverManager.getConnection(url,props);
		spec.remove("user");
		spec.remove("password");
		set2(spec);
	}

	public void set(LuanTable options) throws LuanException, SQLException {
		set2(new LuanTable(options));
	}

	private void set2(LuanTable options) throws LuanException, SQLException {
		Boolean autoCommit = Utils.removeBoolean(options,"auto_commit");
		if( autoCommit != null )
			con.setAutoCommit(autoCommit);
		Integer n = Utils.removeInteger(options,"fetch_size");
		if( n != null )
			fetchSize = n;
		Utils.checkEmpty(options);
	}

	private void fix(Statement stmt) throws SQLException {
		if( fetchSize > 0 )
			stmt.setFetchSize(fetchSize);
	}

	private PreparedStatement prepareStatement(String sql,Object[] args) throws SQLException {
		PreparedStatement pstmt = pstmts.get(sql);
		if( pstmt==null ) {
			pstmt = con.prepareStatement(sql);
			fix(pstmt);
			pstmts.put(sql,pstmt);
		}
		for( int i=0; i<args.length; i++ ) {
			pstmt.setObject(i+1,args[i]);
		}
		return pstmt;
	}

	public ResultSet query(String sql,Object... args) throws SQLException {
		if( args.length == 0 ) {
			Statement stmt = con.createStatement();
			fix(stmt);
			return stmt.executeQuery(sql);
		} else {
			PreparedStatement pstmt = prepareStatement(sql,args);
			return pstmt.executeQuery();
		}
	}

	public int update(String sql,Object... args) throws SQLException {
		if( args.length == 0 ) {
			Statement stmt = con.createStatement();
			fix(stmt);
			int n = stmt.executeUpdate(sql);
			stmt.close();
			return n;
		} else {
			PreparedStatement pstmt = prepareStatement(sql,args);
			return pstmt.executeUpdate();
		}
	}

}
