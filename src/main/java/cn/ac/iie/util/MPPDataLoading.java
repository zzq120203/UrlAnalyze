package cn.ac.iie.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ac.iie.Entity.GlobalConfig;

public abstract class MPPDataLoading<E> {
	private static Logger logger;
	private static boolean isOpenMPP = false;
	private static CassandraConnector connector;
	private static List<Map<String, Object>> list = null;
	private static int batchSize = 1;

	public void init(GlobalConfig gc, String dataCenter, String connAddress, String userName, String password, String tableName,
			String keySpace) {
		
		logger = LoggerFactory.getLogger(MPPDataLoading.class);
		connector = new CassandraConnector(keySpace, dataCenter, connAddress, userName, password,
				tableName, "", new ArrayList<String>(){{this.add("uid");}}, 30000, true);
		isOpenMPP = true;
		connector.setExists("");
		list = new ArrayList<Map<String, Object>>();
	}
	
	public void ObjectPersistence(E entity, boolean isUpdate) throws IOException {
		synchronized (list) {
			Map<String, Object> m = writeValuesAsMap(entity);
			MPPConf mc = new MPPConf(new ArrayList<String>(m.keySet()), isUpdate);
			list.add(m);
			if (list.size() >= batchSize) {
				logger.info("listsize-------------------------" + list.size());
				connector.pushData(list, mc);
				list.clear();
			}
		}
	}
	
	public boolean isOpenMPP() {return isOpenMPP;}
	
	public abstract Map<String, Object> writeValuesAsMap(E e);

}
