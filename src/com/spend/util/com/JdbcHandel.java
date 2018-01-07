package com.spend.util.com;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.ResultSetMetaData;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JdbcHandel {

	private static Logger logger = LogManager.getLogger();

	private String user;
	private String pass;
	private String url;
	private String sDBDriver;

	private Connection conn = null;// 连接对象
	private ResultSet rs = null;// 结果集对象
	private Statement sm = null;

	/**
	 * 构造函数获得数据库用户名和密码
	 * 
	 * @param user
	 * @param pass
	 * @param url
	 */
	public JdbcHandel(String user, String pass, String url, String sDBDriver) {
		this.user = user;
		this.pass = pass;
		this.url = url;
		this.sDBDriver = sDBDriver;
	}

	/**
	 * 连接数据库
	 * 
	 * @return Connection
	 */
	public Connection createConnection() {
		// String sDBDriver = "oracle.jdbc.driver.OracleDriver";
		try {
			Class.forName(sDBDriver).newInstance();
			conn = DriverManager.getConnection(url, user, pass);
		} catch (Exception e) {
			System.out.println("数据库连接失败");
			e.printStackTrace();
		}
		return conn;
	}

	/**
	 * 关闭数据库
	 * 
	 * @param Connection
	 */
	public void closeConnection(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (Exception e) {
			System.out.println("数据库关闭失败");
			e.printStackTrace();
		}
	}

	/**
	 * 插入数据
	 * 
	 * @param insert
	 *            插入语句
	 * @return
	 */
	public int insert(String sql) {
		conn = createConnection();
		int re = 0;
		try {
			conn.setAutoCommit(false);// 事物开始
			sm = conn.createStatement();
			re = sm.executeUpdate(sql);
			if (re < 0) { // 插入失败
				conn.rollback(); // 回滚
				sm.close();
				closeConnection(conn);
				return re;
			}
			conn.commit(); // 插入正常
			sm.close();
			closeConnection(conn);
			return re;
		} catch (Exception e) {
			e.printStackTrace();
		}
		closeConnection(conn);
		return 0;
	}

	public int insert(List<String> listSql) {

		int size = listSql.size();
		logger.debug("insert------size: " + size);
		if (size == 0) {
			return 0;
		}
		if (size > 5000) {
			int flag = 1;
			List<String> listSqlN = new LinkedList<String>();
			for (int i = 0; i < size; i++) {
				listSqlN.add(listSql.get(i));
				if (((i + 1) % 5000) == 0) {
					int exec = insert(listSqlN);
					listSqlN = new LinkedList<String>();
					if (exec != 1) {
						flag = 0;
					}
				}
			}
			int exec = insert(listSqlN);
			if (exec != 1) {
				flag = 0;
			}
			return flag;
		} else {
			conn = createConnection();
			int re = 0;
			try {
				conn.setAutoCommit(false);// 事物开始
				sm = conn.createStatement();

				for (int i = 0; i < listSql.size(); i++) {
					String sql = listSql.get(i);
					re = sm.executeUpdate(sql);
				}

				if (re < 0) { // 插入失败
					conn.rollback(); // 回滚
					sm.close();
					closeConnection(conn);
					return re;
				}
				conn.commit(); // 插入正常
				sm.close();
				closeConnection(conn);
				return re;
			} catch (Exception e) {
				e.printStackTrace();
			}
			closeConnection(conn);
		
		}
		logger.info("insert finish! ---------- conmmit!!!!!!");
		return 0;
	}

	/**
	 * 查询语句 返回结果集
	 * 
	 * @param select
	 * @return
	 */
	public ResultSet selectSql(String sql) {
		conn = createConnection();
		try {
			sm = conn.createStatement();
			rs = sm.executeQuery(sql);
			return rs;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

//	/**
//	 * 根据command命令 返回结果
//	 * 
//	 * @param command
//	 * @return
//	 */
//	public ResultSet commandSql(String sql) {
//		try {
//			conn = createConnection();
//			conn.setAutoCommit(false);
//			PreparedStatement s = conn.prepareStatement(sql);
//			s.addBatch();
//			s.executeBatch();
//		} catch (SQLException e1) {
//			e1.printStackTrace();
//		}
//		return null;
//	}
	
	/**
	 * 根据结果集输出
	 * 
	 * @param rs
	 */
	public void printRs(ResultSet rs) {
		int columnsCount = 0;
		boolean f = false;
		try {
			if (!rs.next()) {
				return;
			}
			ResultSetMetaData rsmd = rs.getMetaData();
			columnsCount = rsmd.getColumnCount();// 数据集的列数
			for (int i = 0; i < columnsCount; i++) {
				System.out.print(rsmd.getColumnLabel(i + 1) + "/t"); // 输出列名
			}
			System.out.println();

			while (!f) {
				for (int i = 1; i <= columnsCount; i++) {
					System.out.print(rs.getString(i) + "/t");
				}
				System.out.println();
				if (!rs.next()) {
					f = true;
				}
			}
			rs.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		closeConnection(conn);
	}

	// public static void main(String[] args) {
	//
	// // JDBC jDBC = new JDBC("dqb", "dqb",
	// // "jdbc:oracle:thin:@172.16.29.184:1521/ora11g");
	// // String insert = "insert into t_department values('D005','外交部')";
	// // System.out.println(jDBC.insert(insert));// 插入成功
	//
	// JdbcHandel jDBC = new JdbcHandel("nbpf", "nbpf",
	// "jdbc:oracle:thin:@172.16.13.86:1521/ora11g");
	// String sql = "SELECT * FROM STAT_WA_DATASTAT_0001";
	// jDBC.printRs(jDBC.selectSql(sql));
	// System.out.println("----------------------------------------");
	//
	// sql = "SELECT * FROM STAT_WA_DATASTAT_0002";
	// jDBC.printRs(jDBC.selectSql(sql));
	// System.out.println("----------------------------------------");
	//
	// sql = "SELECT * FROM STAT_WA_DATASTAT_0003";
	// jDBC.printRs(jDBC.selectSql(sql));
	// System.out.println("----------------------------------------");
	//
	// sql = "SELECT * FROM STAT_WA_DATASTAT_0016";
	// jDBC.printRs(jDBC.selectSql(sql));
	//
	// }

}