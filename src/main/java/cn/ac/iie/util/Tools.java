package cn.ac.iie.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

public class Tools {
	public static String Timestamp2Date(long time) {
		Timestamp ts = new Timestamp(time);
		Timestamp ts2 = new Timestamp(System.currentTimeMillis());
		String tsStr = "";
		DateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
		try {
			tsStr = sdf.format(ts);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tsStr;
	}

	public static void main(String[] arg) {
		ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> period_url = new ConcurrentHashMap();
		String timep = "";
		timep = Timestamp2Date(System.currentTimeMillis());
		ConcurrentHashMap<String, Long> tmpStLong = (ConcurrentHashMap) period_url.get(timep + "_all");
	}
}
