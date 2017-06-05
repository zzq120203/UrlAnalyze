package cn.ac.iie.obtainSrcData;

import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.client.producer.SendStatus;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.remoting.exception.RemotingException;

import cn.ac.iie.Entity.GlobalConfig;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class RocketMQProducer {
	private static Logger logger = LogManager.getLogger(RocketMQProducer.class);
	private static RocketMQProducer instance = null;
	private DefaultMQProducer producer = null;
	private String producerGroupname = "MetaProducerGroup";
	private String namesrvAddr = null;
	private String topic = "bad-topic";

	public RocketMQProducer(GlobalConfig gc) {
		if (instance == null) {
			topic = gc.putURLTopic;
			namesrvAddr = gc.URLconsumerNameSrv;
			producer = getDefaultMQProducer(namesrvAddr);
			instance = this;
		}
	}

	private DefaultMQProducer getDefaultMQProducer(String namesrvAddr) {
		producer = new DefaultMQProducer(producerGroupname);
		producer.setNamesrvAddr(namesrvAddr);
		producer.setSendMsgTimeout(5 * 1000);
		//producer.setClientIP("host ip");
		producer.setInstanceName(producerGroupname + "-oldms");
		/*
		 * 消息体最大不超过16M
		 */
		producer.setMaxMessageSize(16 * 1024 * 1024);
		try {
			producer.start();
		} catch (MQClientException e) {
			logger.error(e, e);
		}
		logger.info("Topic '" + topic + "' has been published.");

		return producer;
	}

	public static RocketMQProducer getInstance() {
		return instance;
	}

	public String getTopic() {
		return topic;
	}

	public boolean sendMessage(byte[] pData) {
		long bg = System.currentTimeMillis();
		SendResult sendResult = null;
		Message msg = new Message(topic, pData);

		try {
			sendResult = producer.send(msg);
		} catch (MQClientException e) {
			logger.error(e, e);
		} catch (RemotingException e) {
			logger.error(e, e);
		} catch (MQBrokerException e) {
			logger.error(e, e);
		} catch (InterruptedException e) {
			logger.error(e, e);
		}

		if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
			if (sendResult == null) {
				logger.error("Send msg failed: null sendResult.");
			} else {
				logger.error("Send msg failed: " + sendResult.getSendStatus());
			}
			return false;
		} else {
			logger.trace("send to rocketmq use " + (System.currentTimeMillis() - bg) +
					" ms for " + topic + " len=" + pData.length);
			return true;
		}
	}
	
	public void shutdown() {
		producer.shutdown();
	}
}
