package cn.ac.iie.obtainSrcData;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.URLDetailEntity;
import cn.ac.iie.Entity.URLInfoEntity;
import cn.ac.iie.di.datadock.rdata.exchange.client.core.session.receive.REAbstractReceiveMessageHandler;
import cn.ac.iie.di.datadock.rdata.exchange.client.exception.REConnectionException;
import cn.ac.iie.di.datadock.rdata.exchange.client.v1.ConsumePosition;
import cn.ac.iie.di.datadock.rdata.exchange.client.v1.REMessageExt;
import cn.ac.iie.di.datadock.rdata.exchange.client.v1.connection.REConnection;
import cn.ac.iie.di.datadock.rdata.exchange.client.v1.session.FormattedHandler;
import cn.ac.iie.di.datadock.rdata.exchange.client.v1.session.REReceiveSession;
import cn.ac.iie.di.datadock.rdata.exchange.client.v1.session.REReceiveSessionBuilder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomizedMQConsumer {

	private final static Logger logger = LoggerFactory.getLogger(CustomizedMQConsumer.class);
	private REReceiveSession receiver = null;
	private static ArrayList<LinkedBlockingQueue<URLInfoEntity>> srcQueue;
	private GlobalConfig gConfig;
	public static AtomicLong filter_by_msg_content_nr = new AtomicLong(0L);
	public static AtomicLong consumer_obtain_nr = new AtomicLong(0L);
	
	public CustomizedMQConsumer(GlobalConfig gc,String mqAddr, int mqPort, String consumerGroup, String topic)
			throws REConnectionException {
		this.gConfig = gc;
		srcQueue = new ArrayList<LinkedBlockingQueue<URLInfoEntity>>();
		for (int i = 0; i < gConfig.maxBlockQueue; i++) {
			LinkedBlockingQueue<URLInfoEntity> dxoBQueue = new LinkedBlockingQueue<URLInfoEntity>(
					gConfig.maxBlockingQueueCapacity);
			srcQueue.add(dxoBQueue);
		}
		REConnection conn = new REConnection(mqAddr + ":" + mqPort);
		REReceiveSessionBuilder builder = (REReceiveSessionBuilder) conn.getReceiveSessionBuilder(topic);
		builder.setGroupName(consumerGroup);
		builder.setConsumPosition(ConsumePosition.CONSUME_FROM_FIRST_OFFSET);
		builder.setConsumeThreadNum(10);
		builder.setFailureHandler(new REAbstractReceiveMessageHandler<byte[]>() {
			@Override
			public boolean handle(byte[] message) {
				logger.error(new String(message));
				return true;
			}
		});
		builder.setHandler(new MessageConsumingHandler(gc, nameToType,
				nameToGetMethod, nameToSetMethod));

		receiver = (REReceiveSession) builder.build();
	}
	public ArrayList<LinkedBlockingQueue<URLInfoEntity>> getSrcQueue() {
		   return srcQueue; 
	}
	public void startConsumer() {
		try {
			receiver.start();
		} catch (REConnectionException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void stopConsumer() {
		try {
			receiver.shutdown();
			logger.info("shuting down consumer!");
		} catch (REConnectionException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private Map<String, Class<?>> nameToType = new HashMap<String, Class<?>>();
	private Map<String, Method> nameToGetMethod = new HashMap<String, Method>();
	private Map<String, Method> nameToSetMethod = new HashMap<String, Method>();


	public static class MessageConsumingHandler extends FormattedHandler{
		Map<String, Class<?>> nameToType = null;
		Map<String, Method> nameToGetMethod = null;
		Map<String, Method> nameToSetMethod = null;
		private GlobalConfig gConfig;
		
		public MessageConsumingHandler(GlobalConfig gc, Map<String, Class<?>> nameToType,
				Map<String, Method> nameToGetMethod, Map<String, Method> nameToSetMethod) {
			this.gConfig = gc;
			this.nameToType = nameToType;
			this.nameToGetMethod = nameToGetMethod;
			this.nameToSetMethod = nameToSetMethod;
		}
		public long getFilterNr()
		{
			return filter_by_msg_content_nr.get();
		}
		@Override
		public boolean handle(REMessageExt messageExt) {
			Iterator<REMessageExt.Record> itr = messageExt.getRecordIterator();
			URLInfoEntity uEntity = new URLInfoEntity();
			int count = 0;
			while (itr.hasNext()) {
				try {
					REMessageExt.Record rec = itr.next();
					if(rec.getString("url") != null ){
						uEntity.setG_id(rec.getString("g_id"));
						uEntity.setU_name(rec.getString("u_name"));
						uEntity.setUrl(rec.getString("url"));
						uEntity.setDomain(rec.getString("domain"));
						uEntity.setM_chat_room(rec.getLong("m_chat_room"));
						uEntity.setU_ch_id(rec.getLong("u_ch_id"));
						uEntity.setM_publish_time(rec.getLong("m_publish_time"));
						uEntity.setU_send_ip(rec.getString("u_send_ip"));
						uEntity.setU_loc_county(rec.getInt("u_loc_county"));
						uEntity.setU_loc_province(rec.getInt("u_loc_province"));
						uEntity.setM_dom_for(rec.getInt("m_dom_for"));
						uEntity.setM_country_code(rec.getInt("m_country_code"));
						uEntity.setM_content(rec.getString("m_content"));
						//logger.info(" uEntity~~~~~~~~~~~" + uEntity.toString());
						consumer_obtain_nr.incrementAndGet();
						if (contentFilter(uEntity.getM_content())) {
							while (srcQueue.get(count % gConfig.maxBlockQueue).remainingCapacity() <= 0) {
//								logger.debug("lllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
								Thread.sleep(10);
							}
							srcQueue.get(count % gConfig.maxBlockQueue).put(uEntity);
						} else
							filter_by_msg_content_nr.incrementAndGet();
						count++;
					}
				}catch (Exception e) {
					logger.error(e.getMessage(), e);
				} 
			}
			return true;
		}
		private boolean contentFilter(String lineTxt) {
			for	(String swl : gConfig.WhiteList)
				if (lineTxt.contains(swl))
					return false;
			return true;
		}
	}

}
