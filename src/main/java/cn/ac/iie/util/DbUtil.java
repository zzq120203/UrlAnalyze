package cn.ac.iie.util;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.LinkBWTable;
import cn.ac.iie.Entity.RulesTable;
import cn.ac.iie.Entity.ThemeTable;
import cn.ac.iie.Entity.TopicTable;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbUtil {
	private static Logger logger = LoggerFactory.getLogger(DbUtil.class);

	public static List<ThemeTable> getThemeTable() {
		List<ThemeTable> result = new ArrayList();
		try {
			Connection dbConn = DbUtilConnection.getConnection();
			Statement stmt = dbConn.createStatement();

			String sql = "select * from t_theme ;";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				ThemeTable oneTheme = new ThemeTable();
				oneTheme.setT_id(rs.getLong("t_id"));
				oneTheme.setT_name(rs.getString("t_name"));
				oneTheme.setT_type(rs.getInt("t_type"));
//				oneTheme.setT_update_time(rs.getLong("t_update_time"));
				oneTheme.setT_update_time(rs.getTimestamp("t_update_time").getTime());
				oneTheme.setUp_user_id(rs.getInt("t_up_user_id"));
				oneTheme.setT_topic_list((ArrayList) rs.getArray("t_topic_list"));
				result.add(oneTheme);
			}
			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		}

		return result;
	}

	public static List<TopicTable> getTopicTable() {
		List<TopicTable> result = new ArrayList();
		try {
			Connection dbConn = DbUtilConnection.getConnection();
			Statement stmt = dbConn.createStatement();

			String sql = "select * from t_topic;";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				TopicTable oneTopic = new TopicTable();
				oneTopic.setTp_t_id(rs.getInt("tp_id"));
				oneTopic.setTp_name(rs.getString("tp_name"));
				oneTopic.setTp_update_time(rs.getLong("tp_update_time"));
				oneTopic.setUp_user_id(rs.getInt("up_user_id"));
				oneTopic.setTp_t_id(rs.getInt("tp_t_id"));
				oneTopic.setR_id_list((ArrayList) rs.getArray("r_id_list"));
				result.add(oneTopic);
			}
			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		}

		return result;
	}

	public static ConcurrentHashMap<String, LinkBWTable> getDomain2BwTable() {
		ConcurrentHashMap<String, LinkBWTable> domain2TableMap = new ConcurrentHashMap();
		Connection dbConnection = DbUtilConnection.getConnection();
		try {
			Statement statement = dbConnection.createStatement();
			String sql = "select * from t_link_white_black";
			ResultSet rSet = statement.executeQuery(sql);
			while (rSet.next()) {
				LinkBWTable bwTable = new LinkBWTable();
				bwTable.setBw_list_id(rSet.getLong("bw_list_id"));
				bwTable.setBw_url(rSet.getString("bw_url"));
				bwTable.setBw_type(rSet.getInt("bw_type"));
				bwTable.setU_user_id(rSet.getInt("u_user_id"));
				bwTable.setUrl_insert_time(rSet.getTimestamp("url_insert_time").getTime());
				domain2TableMap.put(bwTable.getBw_url(), bwTable);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}

		return domain2TableMap;
	}

	public static ConcurrentHashMap<Long, TopicTable> getTopic2ObjectMap() {
		ConcurrentHashMap<Long, TopicTable> topicid_to_object_map = new ConcurrentHashMap();
		try {
			Connection dbConn = DbUtilConnection.getConnection();
			Statement stmt = dbConn.createStatement();

			String sql = "select * from t_topic";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				TopicTable oneTopic = new TopicTable();
				oneTopic.setTp_id(rs.getLong("tp_id"));
				oneTopic.setTp_name(rs.getString("tp_name"));
				oneTopic.setTp_update_time(rs.getTimestamp("tp_update_time").getTime());
				oneTopic.setUp_user_id(rs.getInt("up_user_id"));
				oneTopic.setTp_t_id(rs.getLong("tp_t_id"));
				String id_list_tmp = rs.getString("r_id_list");
				if (id_list_tmp != null) {
					String[] arrayOfString;
					int j = (arrayOfString = id_list_tmp.split(",")).length;
					for (int i = 0; i < j; i++) {
						String tmp = arrayOfString[i];

						if ((tmp != null) && (!tmp.equals("")))
							oneTopic.getR_id_list().add(Long.valueOf(Long.parseLong(tmp)));
					}
				}
				topicid_to_object_map.put(Long.valueOf(oneTopic.getTp_id()), oneTopic);
			}
			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		}

		return topicid_to_object_map;
	}

	public static List<RulesTable> getRulesTable() {
		List<RulesTable> result = new ArrayList();
		try {
			Connection dbConn = DbUtilConnection.getConnection();
			Statement stmt = dbConn.createStatement();

			String sql = "select * from t_rule where rule_type=3 and R_STATUS = 1;";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				RulesTable oneRule = new RulesTable();
				oneRule.setR_id(rs.getInt("r_id"));
//				oneRule.setR_update_time(rs.getLong("r_update_time"));
				oneRule.setR_update_time(rs.getTimestamp("r_update_time").getTime());
				oneRule.setUp_user_id(rs.getInt("up_user_id"));
				oneRule.setRule(rs.getString("rule"));
				oneRule.setRule_type(rs.getInt("rule_type"));
				oneRule.setTp_id(rs.getLong("tp_id"));
				result.add(oneRule);
			}
			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		}

		return result;
	}

	public static ConcurrentHashMap<String, RulesTable> getRulesTableMap() {
		ConcurrentHashMap<String, RulesTable> rule_content_object_map = new ConcurrentHashMap();
		try {
			Connection dbConn = DbUtilConnection.getConnection();
			Statement stmt = dbConn.createStatement();

			String sql = "select * from t_rule where rule_type=3 and R_STATUS = 1";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				RulesTable oneRule = new RulesTable();
				oneRule.setR_id(rs.getInt("r_id"));
//				oneRule.setR_update_time(rs.getLong("r_update_time"));
				oneRule.setR_update_time(rs.getTimestamp("r_update_time").getTime());
				oneRule.setUp_user_id(rs.getInt("up_user_id"));
				oneRule.setRule(rs.getString("rule"));
				oneRule.setRule_type(rs.getInt("rule_type"));
				oneRule.setTp_id(rs.getLong("tp_id"));
				rule_content_object_map.put(oneRule.getRule(), oneRule);
			}

			rs.close();
			stmt.close();
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		}

		return rule_content_object_map;
	}
	
	public static void updateLoc() {
		Connection dbConn = null;
		Statement stmt =null;
		ResultSet rs = null;
		Jedis jedis = null;
		String u_loc_province = "";
		String u_loc_city = "";
		String u_loc_county = "";
		int u_loc_province_id;
		int u_loc_city_id;
		int u_loc_county_id;
		try {
			dbConn = DbUtilConnection.getConnection();
			stmt = dbConn.createStatement();
			String sql = "select * from t_province_city";
			rs = stmt.executeQuery(sql);
			jedis = new Jedis("10.144.32.29",6379);
			if (jedis != null) {
				while (rs.next()) {
					u_loc_province = rs.getString("provincename");
					u_loc_province_id = rs.getInt("provinceid");
					u_loc_city = rs.getString("cityname");
					u_loc_city_id = rs.getInt("cityid");
					u_loc_county = rs.getString("countyname");
					u_loc_county_id = rs.getInt("countyid");
					System.out.println(u_loc_province + ":" + u_loc_province_id + ";" 
							+ u_loc_city + ":" + u_loc_city_id + ";" 
							+ u_loc_county + ":" + u_loc_county_id);
					jedis.hset(u_loc_province, "id", u_loc_province_id + "");
					jedis.hset(u_loc_province, u_loc_city, u_loc_city_id + "");
					jedis.hset(u_loc_province, u_loc_city + u_loc_county, u_loc_county_id + "");
				}
			} else {
				logger.error("jedis is not exist");
			}
		} catch (SQLException ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (jedis != null)
					GlobalConfig.getRpL1().putInstance(jedis);
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
	
}
