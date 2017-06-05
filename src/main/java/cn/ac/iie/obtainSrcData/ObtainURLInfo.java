package cn.ac.iie.obtainSrcData;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.URLData;
import cn.ac.iie.Entity.URLInfoEntity;
import cn.ac.iie.di.datadock.rdata.exchange.client.exception.REConnectionException;
import cn.ac.iie.output.PerTransmitterState;
import cn.ac.iie.util.DbUtil;
import cn.ac.iie.util.STDbUtil;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObtainURLInfo{
	private static Logger logger;
	private GlobalConfig gConfig;
	private RocketMQConsumer consumer;
	private RocketMQProducer producer;
	private CustomizedMQConsumer cConsumer;
	private static ObjectMapper mapper = new ObjectMapper();
	private ArrayList<LinkedBlockingQueue<URLInfoEntity>> inputMsgQueueEntity;

	private static Set<String> set = Collections.synchronizedSet(new HashSet());
	
	private static AtomicLong obtain_nr = new AtomicLong(0L);
	private static AtomicLong webfilter_nr = new AtomicLong(0L);
	private static AtomicLong total_nr = new AtomicLong(0L);
	private static AtomicLong InterruptedException_nr = new AtomicLong(0L);
	private static AtomicLong JsonParseException_nr = new AtomicLong(0L);
	private static AtomicLong JsonMappingException_nr = new AtomicLong(0L);
	private static AtomicLong IOException_nr = new AtomicLong(0L);
	private static AtomicLong BloomFilter_nr = new AtomicLong(0L);
	
	private STDbUtil stDbUtil;
	
	private BloomFilter<String> bfOui;

	public ObtainURLInfo(GlobalConfig globalConfig) {
		this.gConfig = globalConfig;
		logger = LoggerFactory.getLogger(ObtainURLInfo.class);
		this.producer = new RocketMQProducer(gConfig);
		try {
			this.cConsumer = new CustomizedMQConsumer(gConfig ,gConfig.oriMessMQAddress,
					gConfig.oriMessMQPort,gConfig.oriMessConsumerGroup,gConfig.oriMessTopic);
		} catch (REConnectionException e) {
			logger.error(e.getMessage(), e);
		}
		
		cConsumer.startConsumer();
		inputMsgQueueEntity = cConsumer.getSrcQueue();
		
		set = DbUtil.getDomain2BwTable().keySet();
		
		bfOui = BloomFilter.create(new Funnel<String>() {
			private static final long serialVersionUID = 1L;
			@Override
			public void funnel(String from, PrimitiveSink into) {
				into.putString(from, Charsets.UTF_8);
			}
		}, gConfig.BFexpectedInsertions, 0.000001d);
		
		stDbUtil = STDbUtil.getStdbUtil();
	}
	
	public void runThrow() {
		for (int i = 0; i < this.gConfig.maxTransmitThreadCount; i++) {
			InfoThread t = new InfoThread( 
					(LinkedBlockingQueue) this.inputMsgQueueEntity.get(i % this.gConfig.maxBlockQueue));
			Thread tTrhead = new Thread(t);
			tTrhead.start();
			logger.info("-------------------------ObtainURLInforunthrow : "
					+ ((LinkedBlockingQueue) this.inputMsgQueueEntity.get(i % this.gConfig.maxBlockQueue)).size()
					+ "TrheadID: " + tTrhead.getId());
		}
	}
	
	public void stopConsumer() {
		this.consumer.stop();
	}

	public static long getObtainNr() {
		return obtain_nr.get();
	}

	public static long getFilter_Nr() {
		return webfilter_nr.get();
	}

	public static long getTotalNr() {
		return total_nr.get();
	}

	public static long getInterruptedExceptionNr() {
		return InterruptedException_nr.get();
	}

	public static long getJsonParseExceptionNr() {
		return JsonParseException_nr.get();
	}

	public static long getJsonMappingExceptionNr() {
		return JsonMappingException_nr.get();
	}

	public static long getIOExceptionNr() {
		return IOException_nr.get();
	}
	
	public static long getBloomFilter_nr() {
		return BloomFilter_nr.get();
	}

	public String generate_dn(String url) {
		String dnString = null;
		try {
			url = url.replaceAll("\\^", "");
			URI u = new URI(url);
			dnString = u.getHost();
		} catch (URISyntaxException e1) {
			if (dnString == null) {
				int index = url.indexOf("://");
				if (index != -1)
					dnString = url.substring(index + 3);
				index = dnString.indexOf("/");
				if (index != -1)
					dnString = dnString.substring(0, index);
			}
		}
		return dnString;
	}
	
	private boolean mightContain(String deal_id){

        if(deal_id == null || deal_id.length() == 0){
            logger.warn("deal_id is null");
            return true;
        }

        boolean exists = bfOui.mightContain(deal_id);
        if(!exists){
        	bfOui.put(deal_id);
        }
        return exists;
    }
	

	class InfoThread implements Runnable {

		private LinkedBlockingQueue<URLInfoEntity> inputMsgQueueEntity;
		
		public InfoThread(
				LinkedBlockingQueue<URLInfoEntity> inputMsgQueueEntity) {
			this.inputMsgQueueEntity = inputMsgQueueEntity;
		}

		public void run() {
			long lastPersistTimeStamp = System.currentTimeMillis();
			
			while (!gConfig.stopInfoObtainThread) {
				try {
					URLInfoEntity urlie;
					total_nr.incrementAndGet();
					urlie =  this.inputMsgQueueEntity.take();
					if (urlie != null) {
						if (!gConfig.stopYMFromST) {
							long sta = System.currentTimeMillis();
							URLInfoEntity temp = stDbUtil.setLoc(urlie);
							logger.debug("select ST time : " + (System.currentTimeMillis() - sta));
							urlie.setU_loc_province(temp.getU_loc_province());
							urlie.setU_loc_city(temp.getU_loc_city());
							urlie.setU_loc_county(temp.getU_loc_county());
							urlie.setM_dom_for(temp.getM_dom_for());
							urlie.setDomain(temp.getDomain());
						}
						logger.info("total url: " + urlie.getUrl() + "; gid:" + urlie.getG_id()
							+ " ;url-pr" + urlie.getU_loc_province() + " ;url-ci" + urlie.getU_loc_city()+ " ;url-co" + urlie.getU_loc_county());
						if (!mightContain(urlie.getUrl())) {
							String domain = urlie.getDomain();
							if(null != set && null != domain){
								if (null != set  && set.contains(domain)) {
									webfilter_nr.incrementAndGet();
								} else {
//									if (urlie.getG_id() == null || "null".equals(urlie.getG_id())){
//										urlie.setG_id(UUID.randomUUID() + "");
//									} else {
//										//logger.debug("ssssssssssssssssssss");
//									}
									String toWrite = mapper.writeValueAsString(urlie);
									File file = new File(gConfig.InfoFilePath + "/" + urlie.getG_id() + ".json");
		
									if (!file.exists()) {
										file.createNewFile();
									}
									FileWriter fw = new FileWriter(file, true);
									BufferedWriter bw = new BufferedWriter(fw);
									bw.write(toWrite);
									obtain_nr.incrementAndGet();
									bw.flush();
									bw.close();
								}
							}
						} else {
							//insertUserDB(urlie)
							BloomFilter_nr.incrementAndGet();
						}
						if (!gConfig.stopUrlData) {
							URLData urlData = new URLData(urlie);
							logger.info("zzzzzzzzzzzzzzzzzz: " + urlData.toString());
							producer.sendMessage(urlData.toString().getBytes());
						}
					}
					if (System.currentTimeMillis() - lastPersistTimeStamp > gConfig.persistInterval * 1000L) {
						set = DbUtil.getDomain2BwTable().keySet();
						lastPersistTimeStamp = System.currentTimeMillis();
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
			logger.info("Obtain Thread exitting...");
		}
	}
}
