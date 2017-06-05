package cn.ac.iie.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.URLInfoEntity;
import redis.clients.jedis.Jedis;

public class STDbUtil {
	private Logger logger = LoggerFactory.getLogger(STDbUtil.class);
	
	private static Map<String, String> urlymMap;
	
	private static STDbUtil stdbUtil;
	
	private STDbUtil() {
		urlymMap = new HashMap<String, String>();
	}
	
	public static STDbUtil getStdbUtil() {
		if (stdbUtil == null || urlymMap == null)
			stdbUtil = new STDbUtil();
		return stdbUtil;
	}

	public URLInfoEntity setLocFromRedis(URLInfoEntity entity) {
		Jedis jedis = null;
		String domain = entity.getDomain();
		int len = domain.split("\\.").length;
		if (len > 3)
			len = 3;
		try {
			jedis = GlobalConfig.getRpL1().getResource();
			if (jedis != null) {
				for (int i = 2; i <= len; i++) {
					String temp = getDomain(domain, i);
					String loctemp = jedis.hget("url_domain", temp);
					if (loctemp != null && loctemp.length() > 0) {
						entity.setU_loc_province(Integer.parseInt(loctemp.split(";")[0]));
						entity.setU_loc_city(Integer.parseInt(loctemp.split(";")[1]));
						entity.setU_loc_county(Integer.parseInt(loctemp.split(";")[2]));
						entity.setM_dom_for(1);
						entity.setDomain(temp);
						return entity;
					}
				}
				entity.setU_loc_province(-1);
				entity.setU_loc_city(-1);
				entity.setU_loc_county(-1);
				entity.setM_dom_for(-1);
			} else {
				logger.error("jedis is not exist");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
				GlobalConfig.getRpL1().putInstance(jedis);
		}
		return entity;
	}
	
	public URLInfoEntity setLoc(URLInfoEntity entity) {
		entity.setU_loc_province(-1);
		entity.setU_loc_city(-1);
		entity.setU_loc_county(-1);
		entity.setM_dom_for(-1);
		String domain = entity.getDomain();
		int len = domain.split("\\.").length;
		if (len > 3)
			len = 3;
		for (int i = 2; i <= len; i++) {
			String temp = getDomain(domain, i);
			String loctemp = urlymMap.get(temp);
			if (loctemp != null && loctemp.length() > 0) {
				entity.setU_loc_province(Integer.parseInt(loctemp.split(";")[0]));
				entity.setU_loc_city(Integer.parseInt(loctemp.split(";")[1]));
				entity.setU_loc_county(Integer.parseInt(loctemp.split(";")[2]));
				entity.setM_dom_for(1);
				entity.setDomain(temp);
				return entity;
			} else {
				loctemp = getUrlLoctoRedis(temp);
				entity.setU_loc_province(Integer.parseInt(loctemp.split(";")[0]));
				entity.setU_loc_city(Integer.parseInt(loctemp.split(";")[1]));
				entity.setU_loc_county(Integer.parseInt(loctemp.split(";")[2]));
				entity.setM_dom_for(1);
				entity.setDomain(temp);
				return entity;
			}
		}
		return entity;
	}
	
	private String getDomain(String domain, int ii) {
		StringBuffer str = new StringBuffer();
		try {
			for (int i = 0; i < ii; i++){
				str.append(domain.split("\\.")[domain.split("\\.").length - ii + i]).append(".");
			}
		} catch (ArrayIndexOutOfBoundsException e){
			logger.error(e.getMessage(),e);
			logger.error("domain:" + domain + "; len:" + domain.split("\\.").length + "; l:" + ii);
			return domain;
		}
		return str.deleteCharAt(str.length() - 1).toString();
	}

	
	public String getUrlLoctoRedis(String domain) {
		String ym = "";
		String u_loc_province = "";
		String u_loc_city = "";
		String u_loc_county = "";
		Connection dbConn = null;
		PreparedStatement pstmt =null;
		ResultSet rs = null;
		Jedis jedis = null;
		
		String retStr = "-1;-1;-1";
		
		try {
			jedis = GlobalConfig.getRpL1().getResource();
			dbConn = STDbUtilConnection.getConnection();
			String sql = "select YM,SHENGID,SHIID,XIANID from SAMPLEBASE.ICP_GN_BAXX_SLXX_VIEW where YM = ?;";
			pstmt = dbConn.prepareStatement(sql);
			pstmt.setString(1, domain);
			pstmt.execute();
			rs = pstmt.executeQuery();
			if (rs.next()) {
				ym = rs.getString("YM");
				u_loc_province = rs.getString("SHENGID");
				u_loc_city = rs.getString("SHIID");
				u_loc_county = rs.getString("XIANID");
				
				String u_loc_province_id = jedis.hget(u_loc_province, "id");
				if (u_loc_province_id == null || u_loc_province_id.length() <= 0)
					u_loc_province_id = "-1";
				String u_loc_city_id = jedis.hget(u_loc_province,  u_loc_city);
				if (u_loc_city_id == null || u_loc_city_id.length() <= 0)
					u_loc_city_id = "-1";
				String u_loc_county_id = jedis.hget(u_loc_province, u_loc_city + u_loc_county);
				if (u_loc_county_id == null || u_loc_county_id.length() <= 0)
					u_loc_county_id = "-1";
				logger.info("url_domain" + "," + ym + "," + 
						u_loc_province_id 
						+ ";" + u_loc_city_id 
						+ ";" + u_loc_county_id);
				urlymMap.put(ym, u_loc_province_id 
						+ ";" + u_loc_city_id 
						+ ";" + u_loc_county_id);
//				jedis.hset("url_domain", ym, 
//						u_loc_province_id 
//						+ ";" + u_loc_city_id 
//						+ ";" + u_loc_county_id);
				retStr = u_loc_province_id 
						+ ";" + u_loc_city_id 
						+ ";" + u_loc_county_id;
			}
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (pstmt != null)
					pstmt.close();
				GlobalConfig.getRpL1().putInstance(jedis);
			} catch (SQLException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return retStr;
	}
	
	/**
		ID			3307398
		DWMC		陈乐焱
		ZTID		3527502
		DWXZ		个人
		ZT_BAXH		粤ICP备08034628号
		WZID		3605782
		WZMC		www.teamzonetech.com
		WZFZR		陈乐焱
		SITE_URL	www.teamzonetech.com
		YM			teamzonetech.com
		WZ_BAXH		粤ICP备08034628号-1
		SHSJ		2008-07-10
		NRLX		null
		ZJLX		null
		ZJHM		null
		SHENGID		广东省
		SHIID		广州市
		XIANID		天河区
		XXDZ		广东省广州市天河区
		YMID		1442310
		VISITED		null
		CLASSIFY	null
		CREATETIME	2016-12-21 17:59:59.0
		UPDATETIME	2017-01-09 10:48:52.0
		NEWSHENGID	广东省
		NEWSHIID	广州市
		NEWXIANID	天河区
	 */
	public static void main(String[] args) {
//		try {
//			Class.forName("com.oscar.Driver");
//			Connection dbConn = DriverManager.getConnection("jdbc:oscar://10.136.128.41:1521/orcl", "ICP_TMP_USER", "ICP_TMP_USER");
//
//			String sql = "select * from SAMPLEBASE.ICP_GN_BAXX_SLXX_VIEW where XIANID = ?";
//			PreparedStatement pstmt = dbConn.prepareStatement(sql);
////			pstmt.setInt(1, 330000);
//			pstmt.execute();
//			ResultSet rs = pstmt.executeQuery();
//			while (rs.next()){
////				System.out.println(rs.getString("SHENGID"));
//				System.out.println(rs.getString("SHIID"));
//				System.out.println(rs.getString("XIANID"));
//			}
//			rs.close();
//			pstmt.close();
//		} catch (SQLException ex) {
//			logger.error(ex.getMessage());
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String domain = "www.sad.123456cp.com";
//		int len = domain.split("\\.").length;
//		for (int i = 2; i <= len; i++) {
//			
//			String temp = getDomain(domain,i);
//			System.out.println(temp);
//		}
	}
	
}
