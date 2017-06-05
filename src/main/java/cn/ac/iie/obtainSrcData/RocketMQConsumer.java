package cn.ac.iie.obtainSrcData;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.URLInfoEntity;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageExt;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQConsumer {
	private static DefaultMQPushConsumer consumer;
	private static Logger logger;
	private ArrayList<LinkedBlockingQueue<byte[]>> srcQueue;
	private GlobalConfig gConfig;
	private AtomicLong obtain_nr = new AtomicLong(0L);
	
	public RocketMQConsumer(GlobalConfig gc, String consumerNameSrv, String MQTopicName, String consumerInstanceName) {
		init(gc, consumerNameSrv, MQTopicName, consumerInstanceName);
	}
	
	private void init(GlobalConfig gc, String consumerNameSrv, String MQTopicName, String consumerInstanceName) {
		
		this.gConfig = gc;
		logger = LoggerFactory.getLogger(RocketMQConsumer.class);
		try {
			consumer = new DefaultMQPushConsumer("urlgroup");
			srcQueue = new ArrayList<LinkedBlockingQueue<byte[]>>();
			for (int i = 0; i < gConfig.maxBlockQueue; i++) {
				LinkedBlockingQueue<byte[]> dxoBQueue = new LinkedBlockingQueue<byte[]>(
						gConfig.maxBlockingQueueCapacity);
				srcQueue.add(dxoBQueue);
			}
			consumer.setNamesrvAddr(consumerNameSrv);
			consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
			consumer.setInstanceName(
					InetAddress.getLocalHost().getHostAddress().toString() + consumerInstanceName);
			consumer.subscribe(MQTopicName, "*");
			consumer.registerMessageListener(new MessageListenerConcurrently() {
				public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
						ConsumeConcurrentlyContext context) {
					int count = 0;
					for (Message msg : msgs) {
						try {
							while (srcQueue.get(count % gConfig.maxBlockQueue).remainingCapacity() <= 0) {
								Thread.sleep(10);
							}
							srcQueue.get(count % gConfig.maxBlockQueue).put(msg.getBody());
							count ++;
							obtain_nr.incrementAndGet();
						} catch (InterruptedException e) {
							logger.info(e.getMessage(), e);
						}
					}
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
			});
		} catch (MQClientException ex) {
			logger.info(ex.getMessage(), ex);
		} catch (UnknownHostException e) {
			logger.info(e.getMessage(), e);
		}
		logger.debug("construct Consumer topic=" + MQTopicName);
	}
	
	public long getObtainNr() {
		return this.obtain_nr.get();
	}

	public ArrayList<LinkedBlockingQueue<byte[]>> getSrcQueue() {
		return this.srcQueue;
	}

	public void start() {
		try {
			consumer.start();
		} catch (MQClientException ex) {
			logger.info(ex.getMessage());
		}
	}

	public void stop() {
		consumer.shutdown();
	}
}
