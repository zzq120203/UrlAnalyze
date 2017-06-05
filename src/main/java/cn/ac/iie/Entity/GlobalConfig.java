package cn.ac.iie.Entity;

import java.util.HashMap;
import java.util.HashSet;

public class GlobalConfig {
	public boolean stopInfoObtainThread;
	public boolean stopDetailObtainThread;
	public boolean stopLogicProcessThread;
	public boolean stopTransThread;
	public boolean stopPushResultThread;
	public boolean stopTransputClientThread;
	public boolean stopTransputHistoryHotsThread;
	public boolean stopCheckStatesThread;
	public String configFilePath;
	public String RDBTableNameOfWl;
	public int maxTransmitThreadCount;
	public int maxBlockQueue;
	public int maxBlockingQueueCapacity;
	public int maxLogicProcThreadCount;
	public String consumerNameSrv;
	public String consumerInstanceName;
	public String MQTopicName;
	public String redisConfigStr;
	public MMConf mmconf;
	private static RedisPool rpL1;
	public long persistInterval;
	public HashMap<String, HashMap<String, String>> url_bw_list;
	public String InfoFilePath;
	public String DetailFilePath;
	public String rdbUrl;
	public String rdbDriver;
	public String rdbPwd;
	public String rdbUser;
	public String mppDataCenter;
	public String mppKeySpace;
	public String mppConnAddress;
	public int mppPort;
	public String mppUserName;
	public String mppPassword;

	public String oriMessMQAddress;
	public int oriMessMQPort;
	public String oriMessConsumerGroup;
	public String oriMessTopic;
	
	public String URLconsumerNameSrv;
	public String putURLTopic;
	public String getURLTopic;
	public String URLconsumerInstanceName;
	
	public boolean isRocketmq;
	
	public int BFexpectedInsertions;
	public boolean stopUrlData;
	public boolean stopUpUrlData;
	
	public HashSet<String> WhiteList;
	
	public String stdbDriver;
	public String stdbUrl;
	public String stdbUser;
	public String stdbPwd;
	public boolean stopYMFromST;

	public GlobalConfig() {
		this.mmconf = new MMConf();
		this.stopInfoObtainThread = false;
		this.stopDetailObtainThread = false;
		this.stopLogicProcessThread = false;
		this.stopCheckStatesThread = false;
	}

	public static RedisPool getRpL1() {
		return rpL1;
	}

	public static void setRpL1(RedisPool rpL) {
		rpL1 = rpL;
	}

	@Override
	public String toString() {
		return "GlobalConfig ["																+ "\n"
				+ "stopInfoObtainThread=" 				+ stopInfoObtainThread              + "\n"
				+ "stopDetailObtainThread=" 			+ stopDetailObtainThread            + "\n"
				+ "stopLogicProcessThread=" 			+ stopLogicProcessThread            + "\n"
				+ "stopTransThread=" 					+ stopTransThread                   + "\n"
				+ "stopPushResultThread=" 				+ stopPushResultThread              + "\n"
				+ "stopTransputClientThread=" 			+ stopTransputClientThread          + "\n"
				+ "stopTransputHistoryHotsThread=" 		+ stopTransputHistoryHotsThread     + "\n"
				+ "stopCheckStatesThread=" 				+ stopCheckStatesThread             + "\n"
				+ "configFilePath=" 					+ configFilePath                    + "\n"
				+ "RDBTableNameOfWl=" 					+ RDBTableNameOfWl                  + "\n"
				+ "maxTransmitThreadCount=" 			+ maxTransmitThreadCount            + "\n"
				+ "maxBlockQueue=" 						+ maxBlockQueue                     + "\n"
				+ "maxBlockingQueueCapacity=" 			+ maxBlockingQueueCapacity          + "\n"
				+ "maxLogicProcThreadCount=" 			+ maxLogicProcThreadCount           + "\n"
				+ "consumerNameSrv=" 					+ consumerNameSrv                   + "\n"
				+ "consumerInstanceName=" 				+ consumerInstanceName              + "\n"
				+ "MQTopicName=" 						+ MQTopicName                       + "\n"
				+ "redisConfigStr=" 					+ redisConfigStr                    + "\n"
				+ "mmconf=" 							+ mmconf                            + "\n"
				+ "persistInterval=" 					+ persistInterval                   + "\n"
				+ "url_bw_list=" 						+ url_bw_list          				+ "\n"
				+ "InfoFilePath=" 						+ InfoFilePath                      + "\n"
				+ "DetailFilePath=" 					+ DetailFilePath                    + "\n"
				+ "rdbUrl=" 							+ rdbUrl                            + "\n"
				+ "rdbDriver=" 							+ rdbDriver                         + "\n"
				+ "rdbPwd=" 							+ rdbPwd                            + "\n"
				+ "rdbUser=" 							+ rdbUser                           + "\n"
				+ "mppDataCenter=" 						+ mppDataCenter                     + "\n"
				+ "mppKeySpace=" 						+ mppKeySpace                       + "\n"
				+ "mppConnAddress=" 					+ mppConnAddress                    + "\n"
				+ "mppPort=" 							+ mppPort                           + "\n"
				+ "mppUserName="  						+ mppUserName                       + "\n"
				+ "mppPassword=" 						+ mppPassword                       + "\n"
				+ "oriMessMQAddress=" 					+ oriMessMQAddress                  + "\n"
				+ "oriMessMQPort=" 						+ oriMessMQPort                     + "\n"
				+ "oriMessConsumerGroup=" 				+ oriMessConsumerGroup              + "\n"
				+ "oriMessTopic=" 						+ oriMessTopic                      + "\n"
				+ "URLconsumerNameSrv=" 				+ URLconsumerNameSrv                + "\n"
				+ "putURLTopic=" 						+ putURLTopic                       + "\n"
				+ "getURLTopic=" 						+ getURLTopic                       + "\n"
				+ "URLconsumerInstanceName=" 			+ URLconsumerInstanceName           + "\n"
				+ "isRocketmq=" 						+ isRocketmq                        + "\n"
				+ "BFexpectedInsertions=" 				+ BFexpectedInsertions              + "\n"
				+ "stopUrlData=" 						+ stopUrlData                       + "\n"
				+ "WhiteList=" 							+ WhiteList              			+ "\n"
				+ "stdbDriver=" 						+ stdbDriver              			+ "\n"
				+ "stdbUrl=" 							+ stdbUrl              				+ "\n"
				+ "stdbUser=" 							+ stdbUser              			+ "\n"
				+ "stdbPwd=" 							+ stdbPwd              				+ "\n"
				+ "]";
	}
	
	
}
