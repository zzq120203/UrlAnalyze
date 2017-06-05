package cn.ac.iie.output;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.URLDetailEntity;
import cn.ac.iie.util.DbUtil;
import cn.ac.iie.util.MPPDataLoading;
import cn.ac.iie.util.Tools;
import redis.clients.jedis.Jedis;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticAndTransput extends MPPDataLoading<URLDetailEntity>{
	private Logger logger;
	private GlobalConfig gConfig;
	private ArrayList<LinkedBlockingQueue<URLDetailEntity>> inputURLDBlockingQList;
	private List<Integer> groupList;
	private ArrayList<Thread> staAndTranThreads;
	private ArrayList<PerTransmitterState> perThreadStates;
	private AtomicLong sta_trans_process_nr = new AtomicLong(0L);

	public StaticAndTransput(GlobalConfig gc, ArrayList<LinkedBlockingQueue<URLDetailEntity>> obtainSrcBQList) {
		logger = LoggerFactory.getLogger(StaticAndTransput.class);
		this.gConfig = gc;
		this.inputURLDBlockingQList = obtainSrcBQList;
		this.perThreadStates = new ArrayList();
		this.staAndTranThreads = new ArrayList();
		if (!isOpenMPP())
			init(gc, gConfig.mppDataCenter, gConfig.mppConnAddress, gConfig.mppUserName,
					gConfig.mppPassword, gConfig.RDBTableNameOfWl, gConfig.mppKeySpace);
	}

	public void runThrow() {
		for (int i = 0; i < this.gConfig.maxTransmitThreadCount; i++) {
			PerTransmitterState pts = new PerTransmitterState();
			this.perThreadStates.add(pts);
			Transmit t = new Transmit((LinkedBlockingQueue) this.inputURLDBlockingQList.get(i % this.gConfig.maxBlockQueue), pts);
			logger.info("-------------------------runthrow : "
					+ ((LinkedBlockingQueue) this.inputURLDBlockingQList.get(i % this.gConfig.maxBlockQueue)).size());
			Thread tTrhead = new Thread(t);
			ExceptionHandler exceptionHandler = new ExceptionHandler();
			tTrhead.setUncaughtExceptionHandler(exceptionHandler);
			this.staAndTranThreads.add(tTrhead);
			tTrhead.start();
		}
	}

	private void writeToReidsIncremental(ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> data) {
		Jedis jedis = GlobalConfig.getRpL1().getResource();
		//
		try {
			if (jedis != null) {
				for (Map.Entry<String, ConcurrentHashMap<String, Long>> en : data.entrySet()) {
					String key = en.getKey();
					for (Map.Entry<String, Long> ent : en.getValue().entrySet()) {
						jedis.hincrBy(key, ent.getKey(), ent.getValue());
					}
				}
				data.clear();
			} else {
				logger.error("jedis is not runing");
			}
		} finally {

			GlobalConfig.getRpL1().putInstance(jedis);
		}

	}

	public long getObtainNr() {
		return this.sta_trans_process_nr.get();
	}

	public static ConcurrentHashMap<String, Long> MapAdd(ConcurrentHashMap<String, Long> map, String filed,
			long value) {
		if (map == null) {
			map = new ConcurrentHashMap();
			map.put(filed, Long.valueOf(1L));
		} else {
			long newValue;
			if (map.containsKey(filed)) {
				long oldValue = ((Long) map.get(filed)).longValue();
				newValue = oldValue + 1L;
			} else {
				newValue = 1L;
			}
			map.put(filed, Long.valueOf(newValue));
		}
		return map;
	}

	class Transmit implements Runnable {
		private LinkedBlockingQueue<URLDetailEntity> inputBQueue;
		private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> period_url = new ConcurrentHashMap();
		private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> acc_url = new ConcurrentHashMap();
		String timep = null;

		public Transmit(LinkedBlockingQueue<URLDetailEntity> minganResultQueue, PerTransmitterState p) {
			this.inputBQueue = minganResultQueue;
			System.out.println("++++++++++++++++++++++:" + inputBQueue.size());
		}

		private void all_processing_period(URLDetailEntity entity) {
			this.timep = Tools.Timestamp2Date(entity.getM_publish_time() * 1000L);
			synchronized (this.period_url) {
				ConcurrentHashMap<String, Long> tmpStLong = period_url.get(timep + "_url");
				if (tmpStLong != null) {
					MapAdd(tmpStLong, "all", 1L);
				} else {
					period_url.put(timep + "_url", MapAdd(tmpStLong, "all", 1L));
				}
				if ((entity.getU_is_target() & 15) == 1) {
					// targeted计数
					tmpStLong = period_url.get(timep + "_url");
					if (tmpStLong != null) {
						MapAdd(tmpStLong, "targeted", 1L);
					} else {
						period_url.put(timep + "_url", MapAdd(tmpStLong, "targeted", 1L));
					}

					// 国家分布统计150
					if (entity.getM_dom_for() == 1) {
						tmpStLong = acc_url.get(timep + "_url_region_country");
						if (tmpStLong != null) {
							MapAdd(tmpStLong, "150", 1L);
						} else {
							acc_url.put(timep + "_url_region_country", MapAdd(tmpStLong, "150", 1L));
						}
					} else {
						tmpStLong = acc_url.get("accu_url_country");
						if (tmpStLong != null) {
							MapAdd(tmpStLong, "-1", 1L);
						} else {
							acc_url.put("accu_url_country", MapAdd(tmpStLong, "-1", 1L));
						}
					}
					// 省份分布统计
					tmpStLong = period_url.get(timep + "_url_region_province");
					if (tmpStLong != null) {
						MapAdd(tmpStLong, entity.getU_loc_province() + "", 1L);
					} else {
						period_url.put(timep + "_url_region_province",
								MapAdd(tmpStLong, entity.getU_loc_province() + "", 1L));
					}

					// 市分布统计
					tmpStLong = period_url.get(timep + "_url_region_city");
					if (tmpStLong != null) {
						MapAdd(tmpStLong, entity.getU_loc_city() + "", 1L);
					} else {
						period_url.put(timep + "_url_region_city",
								MapAdd(tmpStLong, entity.getU_loc_city() + "", 1L));
					}
					// 县分布统计
					tmpStLong = acc_url.get(timep + "_url_region_county");
					if (tmpStLong != null) {
						MapAdd(tmpStLong, entity.getU_loc_county() + "", 1L);
					} else {
						acc_url.put(timep + "_url_region_county", MapAdd(tmpStLong, entity.getU_loc_county() + "", 1L));
					}

					// 主题统计

					for (long i : entity.getT_id()) {
						tmpStLong = period_url.get(timep + "_url_zhuti");
						if (tmpStLong != null) {
							MapAdd(tmpStLong, i + "_targeted", 1L);
						} else {
							period_url.put(timep + "_url_zhuti", MapAdd(tmpStLong, i + "_targeted", 1L));
						}
					}
					// 专题统计

					for (long i : entity.getTp_id()) {
						tmpStLong = period_url.get(timep + "_url_zhuanti");
						if (tmpStLong != null) {
							MapAdd(tmpStLong, i + "_targeted", 1L);
						} else {
							period_url.put(timep + "_url_zhuanti", MapAdd(tmpStLong, i + "_targeted", 1L));
						}
					}
				}
			}
		}

		private void all_processing_acc(URLDetailEntity entity) {
			ConcurrentHashMap<String, Long> tmpStLong_acc = acc_url.get("accu_url_all");
			synchronized (acc_url) {
				// 统计在此时间段内的全量数据量
				if (tmpStLong_acc != null) {
					MapAdd(tmpStLong_acc, "all", 1L);
				} else {
					acc_url.put("accu_url_all", MapAdd(tmpStLong_acc, "all", 1L));
				}

				if ((entity.getU_is_target() & 15) == 1) {
					// targeted计数
					tmpStLong_acc = acc_url.get("accu_url_all");
					if (tmpStLong_acc != null) {
						MapAdd(tmpStLong_acc, "targeted", 1L);
					} else {
						acc_url.put("accu_url_all", MapAdd(tmpStLong_acc, "targeted", 1L));
					}

					// 国家分布统计150
					if (entity.getM_dom_for() == 1) {
						tmpStLong_acc = acc_url.get("accu_url_country");
						if (tmpStLong_acc != null) {
							MapAdd(tmpStLong_acc, "150", 1L);
						} else {
							acc_url.put("accu_url_country", MapAdd(tmpStLong_acc, "150", 1L));
						}
					} else {
						tmpStLong_acc = acc_url.get("accu_url_country");
						if (tmpStLong_acc != null) {
							MapAdd(tmpStLong_acc, "-1", 1L);
						} else {
							acc_url.put("accu_url_country", MapAdd(tmpStLong_acc, "-1", 1L));
						}
					}
					// 省份分布统计
					tmpStLong_acc = acc_url.get("accu_url_province");
					if (tmpStLong_acc != null) {
						MapAdd(tmpStLong_acc, entity.getU_loc_province() + "", 1L);
					} else {
						acc_url.put("accu_url_province", MapAdd(tmpStLong_acc, entity.getU_loc_province() + "", 1L));
					}

					// 市分布统计
					tmpStLong_acc = acc_url.get("accu_url_city");
					if (tmpStLong_acc != null) {
						MapAdd(tmpStLong_acc, entity.getU_loc_city() + "", 1L);
					} else {
						acc_url.put("accu_url_city", MapAdd(tmpStLong_acc, entity.getU_loc_city() + "", 1L));
					}
					// 县分布统计
					tmpStLong_acc = acc_url.get("accu_url_county");
					if (tmpStLong_acc != null) {
						MapAdd(tmpStLong_acc, entity.getU_loc_county() + "", 1L);
					} else {
						acc_url.put("accu_url_county", MapAdd(tmpStLong_acc, entity.getU_loc_county() + "", 1L));
					}

					// 主题统计

					for (long i : entity.getT_id()) {
						tmpStLong_acc = acc_url.get("accu_url_zhuti");
						if (tmpStLong_acc != null) {
							MapAdd(tmpStLong_acc, i + "_targeted", 1L);
						} else {
							acc_url.put("accu_url_zhuti", MapAdd(tmpStLong_acc, i + "_targeted", 1L));
						}
					}
					// 专题统计

					for (long i : entity.getTp_id()) {
						tmpStLong_acc = acc_url.get("accu_url_zhuanti");
						if (tmpStLong_acc != null) {
							MapAdd(tmpStLong_acc, i + "_targeted", 1L);
						} else {
							acc_url.put("accu_url_zhuanti", MapAdd(tmpStLong_acc, i + "_targeted", 1L));
						}
					}
				}
			}

		}

		public void run() {
			long lastPersistTimeStamp = System.currentTimeMillis();
			while (!gConfig.stopTransThread) {
				URLDetailEntity urlDetailEntity = null;
				try {
					urlDetailEntity = inputBQueue.take();
					if (urlDetailEntity != null) {
						
						ObjectPersistence(urlDetailEntity, false);
						//logger.info("mpp===========:" + urlDetailEntity.toString());
						all_processing_period(urlDetailEntity);
						all_processing_acc(urlDetailEntity);
						sta_trans_process_nr.incrementAndGet();
					}
					if (System.currentTimeMillis() - lastPersistTimeStamp > gConfig.persistInterval) {
						writeToReidsIncremental(period_url);
						writeToReidsIncremental(acc_url);
						lastPersistTimeStamp = System.currentTimeMillis();
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			logger.info("staticAndTrans process exiting!");
		}
	}

	class ExceptionHandler implements Thread.UncaughtExceptionHandler {
		ExceptionHandler() {
		}

		public void uncaughtException(Thread t, Throwable e) {
			logger.error("i catch a exception !");
			logger.error("threadId:" + t.getId());
			logger.error("threadName:" + t.getName());
			logger.error("Exception:" + e.getClass().getName() + " " + e.getMessage());
			logger.error("thread status:" + t.getState());
			e.printStackTrace();
		}
	}

	@Override
	public Map<String, Object> writeValuesAsMap(URLDetailEntity e) {
		Map<String, Object> map = new HashMap();

		map.put("uid", e.getG_id());
		map.put("uwxid", e.getU_name());
		map.put("url", e.getUrl());
		map.put("ucid", Long.valueOf(e.getU_ch_id()));
		map.put("mr", Long.valueOf(e.getM_chat_room()));
		map.put("pt", Long.valueOf(e.getM_publish_time()));
		map.put("it", Long.valueOf(System.currentTimeMillis() / 1000L));
		map.put("sip", e.getU_send_ip());
		map.put("up", Integer.valueOf(e.getU_loc_province()));
		map.put("ulcity", Integer.valueOf(e.getU_loc_city()));
		map.put("uc", Integer.valueOf(e.getU_loc_county()));
		map.put("dom", e.getDomain());
		map.put("ut", e.getUrl_title());
		map.put("ucon", e.getUrl_content());
		map.put("tid", e.getT_id());
		map.put("tpid", e.getTp_id());
		map.put("ru", e.getRules());//
		map.put("rsize", e.getRules().size());
		map.put("ish", Integer.valueOf(e.getU_is_harm()));
		map.put("isd", Integer.valueOf(e.getU_is_dispose()));
		map.put("ist", Integer.valueOf(e.getU_is_target()));
		map.put("mdf", Integer.valueOf(e.getM_dom_for()));
		map.put("mccode", Integer.valueOf(e.getM_country_code()));
//		map.put("opid", Integer.valueOf(e.getOperator_id()));
		map.put("opid", -1);
		map.put("udu", Long.valueOf(e.getU_dispose_uptime()));
		return map;
	}

}
