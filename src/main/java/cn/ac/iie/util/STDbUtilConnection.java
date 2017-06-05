package cn.ac.iie.util;

import cn.ac.iie.Entity.GlobalConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 数据库连接
 *
 */

public class STDbUtilConnection {
	private static Connection conn = null;
	private static Logger logger;

	private static GlobalConfig gConfig;
	
	public STDbUtilConnection(GlobalConfig gc) {
		logger = LoggerFactory.getLogger(STDbUtilConnection.class);
		gConfig = gc;
	}

	private static void init() {
		try {
			Class.forName(gConfig.stdbDriver);
			conn = DriverManager.getConnection(gConfig.stdbUrl, gConfig.stdbUser, gConfig.stdbPwd);
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static Connection getConnection() {
		try {
			if (conn != null && !conn.isClosed()) {
				return conn;
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
		init();
		return conn;
	}
	
	public void close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

}
