package cn.ac.iie.check;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Process.LogicSyntaxTreeProcess;
import cn.ac.iie.obtainSrcData.CustomizedMQConsumer;
import cn.ac.iie.obtainSrcData.ObtainURLDetail;
import cn.ac.iie.obtainSrcData.ObtainURLInfo;
import cn.ac.iie.output.StaticAndTransput;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

public class CheckStates implements Runnable {
	private static Logger logger;
	private GlobalConfig gConfig;
	private ObtainURLInfo obUrlInfo;
	private ObtainURLDetail obUrlDetail;
	private LogicSyntaxTreeProcess logicProcess;
	private StaticAndTransput staicAndTrans;

	public CheckStates(GlobalConfig gc, ObtainURLInfo obUrlInfo, ObtainURLDetail obUrlDetail,
			LogicSyntaxTreeProcess logicProcess, StaticAndTransput staicAndTrans) {
		logger = LoggerFactory.getLogger(CheckStates.class);
		this.gConfig = gc;
		this.obUrlInfo = obUrlInfo;
		this.obUrlDetail = obUrlDetail;
		this.logicProcess = logicProcess;
		this.staicAndTrans = staicAndTrans;
	}

	public void run() {
		//long lastTs = System.currentTimeMillis();
		//Jedis jedis = GlobalConfig.getRpL1().getResource();
		while (!gConfig.stopCheckStatesThread) {
			try {
				logger.info("consumer_obtain_nr			process		total			url	nr	: " + CustomizedMQConsumer.consumer_obtain_nr);
				logger.info("filter_by_msg_content_nr	process		total			url	nr	: " + CustomizedMQConsumer.filter_by_msg_content_nr);
				logger.info("ObtainInfo					process		BloomFilter		url	nr	: " + ObtainURLInfo.getBloomFilter_nr());
				logger.info("ObtainInfo					process		total			url	nr	: " + ObtainURLInfo.getTotalNr());
				logger.info("ObtainInfo					process		webfilter		url	nr	: " + ObtainURLInfo.getFilter_Nr());
				logger.info("ObtainInfo					process		remain			url	nr	: " + ObtainURLInfo.getObtainNr());
				logger.info("ObtainDetail				process		total			url	nr	: " + obUrlDetail.getObtainNr());
				logger.info("LogicProcess				process		total			url	nr	: " + logicProcess.getObtainNr());
				logger.info("StaTranProcess				process		total			url	nr	: " + staicAndTrans.getObtainNr());
				
				logger.info("UpdateDB					process		total    		url	nr	: " + obUrlInfo.getInterruptedExceptionNr());

				Thread.sleep(10000L);
			} catch (InterruptedException exception) {
				logger.error(exception.getMessage(), exception);
			}
		}
	}
}
