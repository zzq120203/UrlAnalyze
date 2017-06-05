/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.ac.iie.util;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.MMConf;
import cn.ac.iie.Entity.RedisPool;


public class LoadConfig {

    private final GlobalConfig gConfig;
    private static Logger logger;

    public LoadConfig(String configFilePathString, GlobalConfig gc) {
    	 logger = LoggerFactory.getLogger(LoadConfig.class);
        gConfig = gc;
        gConfig.configFilePath = configFilePathString;
        loadConfigFile();
    }

    public LoadConfig(GlobalConfig gc) {
    	 logger = LoggerFactory.getLogger(LoadConfig.class);
        this.gConfig = gc;
    }

    public final void loadConfigFile() {
        String filePath = this.gConfig.configFilePath;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
            Document doc = dbBuilder.parse(filePath);

            this.gConfig.consumerNameSrv = doc.getElementsByTagName("consumerNameSrv").item(0).getFirstChild().getNodeValue();
            this.gConfig.MQTopicName = doc.getElementsByTagName("MQTopicName").item(0).getFirstChild().getNodeValue();
            this.gConfig.consumerInstanceName = doc.getElementsByTagName("consumerInstanceName").item(0).getFirstChild().getNodeValue();
            
            String bq = doc.getElementsByTagName("MaxBlockQueue").item(0).getFirstChild().getNodeValue();
            this.gConfig.maxBlockQueue = Integer.parseInt(bq);
          
            String bqCapacity = doc.getElementsByTagName("MaxBlockingQueueCapacity").item(0).getFirstChild().getNodeValue();
            this.gConfig.maxBlockingQueueCapacity = Integer.parseInt(bqCapacity);
            String lqc = doc.getElementsByTagName("MaxLogicProcThreadCount").item(0).getFirstChild().getNodeValue();
            this.gConfig.maxLogicProcThreadCount = Integer.parseInt(lqc);
            String thc = doc.getElementsByTagName("MaxTransmitThreadCount").item(0).getFirstChild().getNodeValue();
            this.gConfig.maxTransmitThreadCount = Integer.parseInt(thc);
            String persistInterval = doc.getElementsByTagName("persistInterval").item(0).getFirstChild().getNodeValue();
            this.gConfig.persistInterval = Integer.parseInt(persistInterval);
            this.gConfig.redisConfigStr = doc.getElementsByTagName("redisConfigStr").item(0).getFirstChild().getNodeValue();
            
            init(gConfig, gConfig.redisConfigStr);
            this.gConfig.InfoFilePath = doc.getElementsByTagName("InfoFilePath").item(0).getFirstChild().getNodeValue();
            this.gConfig.DetailFilePath = doc.getElementsByTagName("DetailFilePath").item(0).getFirstChild().getNodeValue();
            this.gConfig.rdbUrl = doc.getElementsByTagName("RdbUrl").item(0).getFirstChild().getNodeValue();
            this.gConfig.rdbDriver = doc.getElementsByTagName("RdbDriver").item(0).getFirstChild().getNodeValue();
            this.gConfig.rdbPwd = doc.getElementsByTagName("RdbPwd").item(0).getFirstChild().getNodeValue();
            this.gConfig.rdbUser = doc.getElementsByTagName("RdbUser").item(0).getFirstChild().getNodeValue();
            this.gConfig.RDBTableNameOfWl = doc.getElementsByTagName("RDBTableNameOfWl").item(0).getFirstChild().getNodeValue();

            this.gConfig.mppDataCenter = doc.getElementsByTagName("MppDataCenter").item(0).getFirstChild().getNodeValue();
            this.gConfig.mppKeySpace = doc.getElementsByTagName("MppKeySpace").item(0).getFirstChild().getNodeValue();
            this.gConfig.mppConnAddress = doc.getElementsByTagName("MppConnAddress").item(0).getFirstChild().getNodeValue();
            String mppPort = doc.getElementsByTagName("MppPort").item(0).getFirstChild().getNodeValue();
            this.gConfig.mppPort = Integer.parseInt(mppPort);
            this.gConfig.mppUserName = doc.getElementsByTagName("MppUserName").item(0).getFirstChild().getNodeValue();
            this.gConfig.mppPassword = doc.getElementsByTagName("MppPassword").item(0).getFirstChild().getNodeValue();
            this.gConfig.oriMessMQAddress = doc.getElementsByTagName("OriMessMQAddress").item(0).getFirstChild().getNodeValue();
            String oriMessMQPort = doc.getElementsByTagName("OriMessMQPort").item(0).getFirstChild().getNodeValue();
            this.gConfig.oriMessMQPort = Integer.parseInt(oriMessMQPort);
            this.gConfig.oriMessConsumerGroup = doc.getElementsByTagName("OriMessConsumerGroup").item(0).getFirstChild().getNodeValue();
            this.gConfig.oriMessTopic = doc.getElementsByTagName("OriMessTopic").item(0).getFirstChild().getNodeValue();

            this.gConfig.URLconsumerNameSrv = doc.getElementsByTagName("URLConsumerNameSrv").item(0).getFirstChild().getNodeValue();
            this.gConfig.putURLTopic = doc.getElementsByTagName("putURLTopic").item(0).getFirstChild().getNodeValue();
            this.gConfig.getURLTopic = doc.getElementsByTagName("getURLTopic").item(0).getFirstChild().getNodeValue();
            this.gConfig.URLconsumerInstanceName = doc.getElementsByTagName("URLConsumerInstanceName").item(0).getFirstChild().getNodeValue();

            this.gConfig.isRocketmq = Boolean.parseBoolean(doc.getElementsByTagName("isRocketmq").item(0).getFirstChild().getNodeValue());
            this.gConfig.BFexpectedInsertions = Integer.parseInt(doc.getElementsByTagName("BFexpectedInsertions").item(0).getFirstChild().getNodeValue());

            this.gConfig.stopUrlData = Boolean.parseBoolean(doc.getElementsByTagName("stopUrlData").item(0).getFirstChild().getNodeValue());
            this.gConfig.stopUpUrlData = Boolean.parseBoolean(doc.getElementsByTagName("stopUpUrlData").item(0).getFirstChild().getNodeValue());
            
            this.gConfig.stopYMFromST = Boolean.parseBoolean(doc.getElementsByTagName("stopYMFromST").item(0).getFirstChild().getNodeValue());

            this.gConfig.WhiteList = new HashSet<String>(Arrays.asList(doc.getElementsByTagName("WhiteList").item(0).getFirstChild().getNodeValue().split(";")));
            
            this.gConfig.stdbUrl = doc.getElementsByTagName("stdbUrl").item(0).getFirstChild().getNodeValue();
            this.gConfig.stdbDriver = doc.getElementsByTagName("stdbDriver").item(0).getFirstChild().getNodeValue();
            this.gConfig.stdbPwd = doc.getElementsByTagName("stdbPwd").item(0).getFirstChild().getNodeValue();
            this.gConfig.stdbUser = doc.getElementsByTagName("stdbUser").item(0).getFirstChild().getNodeValue();
            
            logger.info("Init redis pool: mode=" + gConfig.mmconf.getRedisMode() + ", urls=" + gConfig.redisConfigStr);
    		
            logger.info("load configure file success!");
            logger.info("init mongodb connection pool ok.");
        } catch (Exception ex) {
            logger.error(ex.getMessage(),ex);
        }
    }
    
    private void init(GlobalConfig gConfig) throws Exception {
		switch (gConfig.mmconf.getRedisMode()) {
		case SENTINEL:
			gConfig.setRpL1(new RedisPool(gConfig.mmconf, "mymaster"));
			break;
		case STANDALONE:
			gConfig.setRpL1(new RedisPool(gConfig.mmconf, "nomaster"));
			break;
		default:
			break;
		}
	}
    
    private int init_by_sentinel(GlobalConfig gConfig, String urls) throws Exception {
    	MMConf conf = gConfig.mmconf;
    	
		if (conf.getRedisMode() != MMConf.RedisMode.SENTINEL) {
			return -1;
		}
		// iterate the sentinel set, get master IP:port, save to sentinel set
		if (conf.getSentinels() == null) {
			if (urls == null) {
				throw new Exception("Invalid URL(null) or sentinels.");
			}
			HashSet<String> sens = new HashSet<String>();
			String[] s = urls.split(";");
			
			for (int i = 0; i < s.length; i++) {
				sens.add(s[i]);
			}
			conf.setSentinels(sens);
		}
		System.out.println("startttttttttttttttttttttttttttttttttttt");
		init(gConfig);
		
		return 0;
	}
	
	private int init_by_standalone(GlobalConfig gConfig, String urls) throws Exception {
		MMConf conf = gConfig.mmconf;
		
		if (conf.getRedisMode() != MMConf.RedisMode.STANDALONE) {
			return -1;
		}
		// get IP:port, save it to HaP
		if (urls == null) {
			throw new Exception("Invalid URL: null");
		}
		String[] s = urls.split(":");
		if (s != null && s.length == 2) {
			try {
				HostAndPort hap = new HostAndPort(s[0], 
						Integer.parseInt(s[1]));
				conf.setHap(hap);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		init(gConfig);
		
		return 0;
	}
    
    public int init(GlobalConfig gConfig, String urls) throws Exception {
		if (urls == null) {
			throw new Exception("The url can not be null.");
		}
		if (urls.startsWith("STL://")) {
			urls = urls.substring(6);
			gConfig.mmconf.setRedisMode(MMConf.RedisMode.SENTINEL);
		} else if (urls.startsWith("STA://")) {
			urls = urls.substring(6);
			gConfig.mmconf.setRedisMode(MMConf.RedisMode.STANDALONE);
		}
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~" + urls);
		switch (gConfig.mmconf.getRedisMode()) {
		case SENTINEL:
			init_by_sentinel(gConfig, urls);
			break;
		case STANDALONE:
			init_by_standalone(gConfig, urls);
			break;
		case CLUSTER:
			System.out.println("MMS do NOT support CLUSTER mode now, " +
					"use STL/STA instead.");
			break;
		default:
			break;
		}
		
		return 0;
	}

    public final void loadConfigFromDatabase() {
    	Jedis j = gConfig.getRpL1().getResource();
    	try {
    		if (j != null) {
			
    			Set<String> keys = j.keys("*");
    			Iterator<String> it = keys.iterator();
    			while(it.hasNext())
    			{
    				String key = it.next();
    				HashMap url_bw_list = (HashMap)j.hgetAll(key);
    				gConfig.url_bw_list.put(key, url_bw_list);
    			}
    		}
    	} catch (Exception e) {
    		logger.error(e.getMessage());
    	} finally {
    		GlobalConfig.getRpL1().putInstance(j);
    	}
    	GlobalConfig.getRpL1().quit();
        logger.info("load configure from database success!");
    }

}
