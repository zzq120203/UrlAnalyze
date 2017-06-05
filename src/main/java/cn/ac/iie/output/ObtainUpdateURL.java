package cn.ac.iie.output;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.URLData;
import cn.ac.iie.obtainSrcData.RocketMQConsumer;
import cn.ac.iie.util.MPPDataLoading;

public class ObtainUpdateURL extends MPPDataLoading<URLData>{
	private static Logger logger;
	private GlobalConfig gConfig;
	private RocketMQConsumer consumer;
	private ArrayList<LinkedBlockingQueue<byte[]>> inputURLDBlockingQList;
	private static ObjectMapper mapper = new ObjectMapper();
	
	private AtomicLong InterruptedException_nr = new AtomicLong(0L);
	private AtomicLong JsonParseException_nr = new AtomicLong(0L);
	private AtomicLong JsonMappingException_nr = new AtomicLong(0L);
	private AtomicLong IOException_nr = new AtomicLong(0L);
	
	public ObtainUpdateURL(GlobalConfig gc) {
		super();
		this.gConfig = gc;
		logger = LoggerFactory.getLogger(ObtainUpdateURL.class);
		consumer = new RocketMQConsumer(gc, gc.URLconsumerNameSrv, gc.getURLTopic, gc.URLconsumerInstanceName);
		if (!isOpenMPP())
			init(gc, gConfig.mppDataCenter, gConfig.mppConnAddress, gConfig.mppUserName,
					gConfig.mppPassword, gConfig.RDBTableNameOfWl, gConfig.mppKeySpace);
	}
	
	public void start(){
		consumer.start();
		for (int i = 0; i < this.gConfig.maxTransmitThreadCount; i++) {
			PerTransmitterState pts = new PerTransmitterState();
			UpdateDB t = new UpdateDB((LinkedBlockingQueue) this.inputURLDBlockingQList.get(i % this.gConfig.maxBlockQueue));
			logger.info("-------------------------runthrow : "
					+ ((LinkedBlockingQueue) this.inputURLDBlockingQList.get(i % this.gConfig.maxBlockQueue)).size());
			Thread tTrhead = new Thread(t);
			tTrhead.start();
		}
		
	}
	
	public long getInterruptedExceptionNr() {
		return this.InterruptedException_nr.get();
	}

	public long getJsonParseExceptionNr() {
		return this.JsonParseException_nr.get();
	}

	public long getJsonMappingExceptionNr() {
		return this.JsonMappingException_nr.get();
	}

	public long getIOExceptionNr() {
		return this.IOException_nr.get();
	}

	
	
	class UpdateDB implements Runnable{
		
		private LinkedBlockingQueue<byte[]> inputMsgQueue;

		public UpdateDB(LinkedBlockingQueue<byte[]> inputMsgQueue) {
			super();
			this.inputMsgQueue = inputMsgQueue;
		}

		@Override
		public void run() {
			
//			inputMsgQueue = consumer.getSrcQueue();
			try {
				while (!gConfig.stopInfoObtainThread) {
					byte[] msg = (byte[]) this.inputMsgQueue.take();
					URLData urldata = (URLData) mapper.readValue(msg, URLData.class);
					if (urldata != null) {
						int u_is_target = urldata.getU_is_target();
						if (u_is_target == 1) {
							logger.info(urldata.toString());
							ObjectPersistence(urldata, true);
						}
					}
				}
			} catch (InterruptedException e) {
				InterruptedException_nr.incrementAndGet();
				logger.error(e.getMessage(), e);
			} catch (JsonParseException e) {
				JsonParseException_nr.incrementAndGet();
				logger.error(e.getMessage(), e);
			} catch (JsonMappingException e) {
				JsonMappingException_nr.incrementAndGet();
				logger.error(e.getMessage(), e);
			} catch (IOException e) {
				IOException_nr.incrementAndGet();
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public Map<String, Object> writeValuesAsMap(URLData e) {
		Map<String, Object> map = new HashMap();
		map.put("url_id", e.getG_id());
		map.put("ist", Integer.valueOf(e.getU_is_target()));
		return map;
	}
}
