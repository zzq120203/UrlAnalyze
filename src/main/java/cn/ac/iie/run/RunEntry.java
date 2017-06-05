package cn.ac.iie.run;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Process.LogicSyntaxTreeProcess;
import cn.ac.iie.check.CheckStates;
import cn.ac.iie.obtainSrcData.ObtainURLDetail;
import cn.ac.iie.obtainSrcData.ObtainURLInfo;
import cn.ac.iie.output.StaticAndTransput;
import cn.ac.iie.output.ObtainUpdateURL;
import cn.ac.iie.util.DbUtilConnection;
import cn.ac.iie.util.LoadConfig;
import cn.ac.iie.util.STDbUtilConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunEntry {
	private static GlobalConfig gConfig = new GlobalConfig();
	private static Logger logger;

	public static void main(String[] args) {
		
		logger = LoggerFactory.getLogger(RunEntry.class);
		
		new LoadConfig("./Configure.xml", gConfig);
		logger.info(gConfig.toString());
		
		new DbUtilConnection(gConfig);
		new STDbUtilConnection(gConfig);
		//DbUtil.updateLoc();
		// 获取消息隊列数据 && 推送URL
		ObtainURLInfo obtainUrlInfo = new ObtainURLInfo(gConfig);
		obtainUrlInfo.runThrow();

		// 获取爬虫后数据
		ObtainURLDetail obtainUrlDetail = new ObtainURLDetail(gConfig);
		Thread obtainDetailThread = new Thread(obtainUrlDetail);
		obtainDetailThread.start();

		// 進行命中分析
		LogicSyntaxTreeProcess logicTree = new LogicSyntaxTreeProcess(gConfig, obtainUrlDetail.getOutputURLDBlockingQList());
		logicTree.initalize();

		// 統計、入庫
		StaticAndTransput staAndTran = new StaticAndTransput(gConfig, logicTree.getOutputURLDBlockingQList());
		staAndTran.runThrow();

		//获取URL、更新库
		if (!gConfig.stopUpUrlData) {
			ObtainUpdateURL ouu = new ObtainUpdateURL(gConfig);
			ouu.start();
		}
		// 日志輸出
		CheckStates checkStates = new CheckStates(gConfig, obtainUrlInfo, obtainUrlDetail, logicTree, staAndTran);
		Thread checkThread = new Thread(checkStates);
		checkThread.start();
		
	}
}
