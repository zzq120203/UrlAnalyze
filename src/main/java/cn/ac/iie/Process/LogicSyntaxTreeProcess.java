package cn.ac.iie.Process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.ac.iie.Entity.GlobalConfig;
import cn.ac.iie.Entity.RulesTable;
import cn.ac.iie.Entity.TopicTable;
import cn.ac.iie.Entity.URLDetailEntity;
import cn.ac.iie.util.DbUtil;

public class LogicSyntaxTreeProcess {

	private static Logger logger;
	private GlobalConfig gConfig;
	private ArrayList<LinkedBlockingQueue<URLDetailEntity>> inputURLDBlockingQList;
	private ArrayList<LinkedBlockingQueue<URLDetailEntity>> outputprocessedURLDBlockingQList;
	private ArrayList<Thread> procThreads;
	private ArrayList<PerLogicProcesThreadState> perThreadStates;
	private ConcurrentHashMap<String, RulesTable> rule_content2ObjMap;
	private ConcurrentHashMap<Long, TopicTable> topic_id2ObjMap;
	private ConcurrentHashMap<String, LogicSyntaxTree> ruleTrees;
	private AtomicLong logic_process_nr = new AtomicLong(0L);
	private static final Map<String, Integer> operatorPriority = new HashMap<String, Integer>() {
		private static final long serialVersionUID = 1L;

		{
			put("|", 1);
			put("&", 2);
			put("!", 3);
			put("(", 4);
		}
	};

	public LogicSyntaxTreeProcess(GlobalConfig gc, ArrayList<LinkedBlockingQueue<URLDetailEntity>> obtainSrcBQList) {

		logger = LoggerFactory.getLogger(LogicSyntaxTree.class);
		gConfig = gc;
		inputURLDBlockingQList = obtainSrcBQList;
		outputprocessedURLDBlockingQList = new ArrayList<LinkedBlockingQueue<URLDetailEntity>>();
		perThreadStates = new ArrayList<PerLogicProcesThreadState>();
		procThreads = new ArrayList<Thread>();
	}

	public ArrayList<LinkedBlockingQueue<URLDetailEntity>> getOutputURLDBlockingQList() {
		return outputprocessedURLDBlockingQList;
	}

	public void MakeTree() {
		rule_content2ObjMap = DbUtil.getRulesTableMap();
//		logger.info("rule_content2ObjMap: " + rule_content2ObjMap.size());
		topic_id2ObjMap = DbUtil.getTopic2ObjectMap();
//		logger.info("topic_id2ObjMap: " + topic_id2ObjMap.size());
		ruleTrees = new ConcurrentHashMap<String, LogicSyntaxTree>();
		for (Map.Entry<String, RulesTable> entry : rule_content2ObjMap.entrySet()) {
			try {
				LogicSyntaxTree oneTree = parse(entry.getKey());
				ruleTrees.put(entry.getKey(), oneTree);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		logger.info("ruleTrees: " + ruleTrees.size());
	}

	public boolean initalize() {
		MakeTree();
		try {
			// initalize output blocking queues list
			for (int i = 0; i < gConfig.maxBlockQueue; i++) {
				LinkedBlockingQueue<URLDetailEntity> toStaticBQueue = new LinkedBlockingQueue<URLDetailEntity>(
						gConfig.maxBlockingQueueCapacity);
				outputprocessedURLDBlockingQList.add(toStaticBQueue);
			}
			// run logic tree threads
			for (int i = 0; i < gConfig.maxLogicProcThreadCount; i++) {
				PerLogicProcesThreadState ptState = new PerLogicProcesThreadState();
				perThreadStates.add(ptState);
				ProThread sThread = new ProThread(inputURLDBlockingQList.get(i % gConfig.maxBlockQueue),
						outputprocessedURLDBlockingQList.get(i % gConfig.maxBlockQueue), ptState);
				Thread proThread = new Thread(sThread);
				procThreads.add(proThread);
				proThread.start();
			}
			logger.debug("initalize logic tree threads success!");
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return true;
	}

	public long getObtainNr() {
		return logic_process_nr.get();
	}

	// Multiple thread
	class ProThread implements Runnable {

		private BlockingQueue<URLDetailEntity> srcBlockQ;
		private BlockingQueue<URLDetailEntity> outputBlockQ;
		private PerLogicProcesThreadState ptState;

		public ProThread(BlockingQueue<URLDetailEntity> bq, BlockingQueue<URLDetailEntity> outputbq,
				PerLogicProcesThreadState pts) {
			logger.trace("Create logic process thread!");
			this.srcBlockQ = bq;
			this.outputBlockQ = outputbq;
			this.ptState = pts;
		}

		// @Override
		public void run() {
			long lastPersistTimeStamp = System.currentTimeMillis();
			while (!gConfig.stopLogicProcessThread) {
				try {
					URLDetailEntity detailEntity = srcBlockQ.take();
					for (Map.Entry<String, LogicSyntaxTree> entry : ruleTrees.entrySet()) {
						boolean targeted = entry.getValue().containsVerify(detailEntity.getUrl_content());
						if (targeted) {
							detailEntity.setU_is_target(1);
							long rule_id = rule_content2ObjMap.get(entry.getKey()).getR_id();
							long tp_id = rule_content2ObjMap.get(entry.getKey()).getTp_id();
							long zh_id = topic_id2ObjMap.get(tp_id).getTp_t_id();
							detailEntity.getRules().add(rule_id);
							detailEntity.getTp_id().add(tp_id);
							detailEntity.getT_id().add(zh_id);
							logger.info("targeted.................." + detailEntity.getRules());
						}
					}
					logic_process_nr.incrementAndGet();
					outputBlockQ.put(detailEntity);
					if (System.currentTimeMillis() - lastPersistTimeStamp > gConfig.persistInterval * 1000L) {
						MakeTree();
						lastPersistTimeStamp = System.currentTimeMillis();
					}
					ptState.processNum++;
				} catch (Exception e) {
					logger.warn(e.getMessage());
				}
			}
			logger.info("logic process Thread exiting...");
		}
	}

	private static List<String> reversePolishTransformation(String str){
		str=str.replace("AND", "&");
		str=str.replace("OR", "|");
		str=str.replace("NOT", "!");
		Stack<String> stack=new Stack<String>();
		List<String> list=new ArrayList<String>();
		
		char [] charArr=str.toCharArray();
		int pos=0;
		int i=0;
		for(i=0;i<charArr.length;++i){
			String charStr=charArr[i]+"";
			if(operatorPriority.containsKey(charStr) || ")".equals(charStr)){
				if(i>pos){
					list.add(str.substring(pos, i).trim());
				}
				if("(".equals(charStr)||")".equals(charStr)){
					if("(".equals(charStr)){
						stack.push("(");
					}else{
						while(!stack.peek().equals("(")){
							list.add(stack.pop());
						}
						stack.pop();
					}
				}else{
					if(stack.empty()||operatorPriority.get(stack.peek())<=operatorPriority.get(charStr)){
						stack.push(charStr);
					}else{
						while(!stack.empty()){
							if(operatorPriority.get(stack.peek())>=operatorPriority.get(charStr)&&
									!stack.peek().equals("(")){
								list.add(stack.pop());
							}else if(stack.peek().equals("(")){
								stack.push(charStr);
								break;
							}else{
								break;
							}
						}
					}
				}
				pos=i+1;
			}
		}
		
		if(i>pos){
			list.add(str.substring(pos, i).trim());
		}
		
		while(stack.empty()==false)
			list.add(stack.pop());
		
		return list;
	}

	/**
	 * 将逆波兰式转为语法树
	 * @param list
	 * @return
	 */
	private static LogicSyntaxTree reversePolishToSyntaxTree(List<String> list){
		Stack<Object> resStack=new Stack<Object>();
		
		for(int i=0;i<list.size();++i){
			if(operatorPriority.containsKey(list.get(i))){//逻辑运算符
				if(list.get(i).equals("!")){
					LogicOperatorNode node=new LogicOperatorNode();
					node.opt=LOGOPT.NOT;
					if(resStack.peek() instanceof String){
						LogicOperatorNode leaf=new LogicOperatorNode();
						leaf.opt=LOGOPT.LEAF;
						leaf.str=(String)resStack.pop();
						node.leftOperand=leaf;
					}else{
						node.leftOperand=(LogicOperatorNode)resStack.pop();
					}
					resStack.push(node);
				}else{
					LogicOperatorNode node=new LogicOperatorNode();
					if(list.get(i).equals("&")){
						node.opt=LOGOPT.AND;
					}else{// operator '|'
						node.opt=LOGOPT.OR;
					}
					
					//the first operand will be assigned as the right child
					if(resStack.peek() instanceof String){
						LogicOperatorNode leaf=new LogicOperatorNode();
						leaf.opt=LOGOPT.LEAF;
						leaf.str=(String)resStack.pop();
						node.ritghOperand=leaf;
					}else{
						node.ritghOperand=(LogicOperatorNode)resStack.pop();
					}

					//the second operand will be assigned as the left child
					if(resStack.peek() instanceof String){
						LogicOperatorNode leaf=new LogicOperatorNode();
						leaf.opt=LOGOPT.LEAF;
						leaf.str=(String)resStack.pop();
						node.leftOperand=leaf;
					}else{
						node.leftOperand=(LogicOperatorNode)resStack.pop();
					}
					resStack.push(node);
				}
			}else{//操作数
				resStack.push(list.get(i));
			}
		}
		LogicSyntaxTree lsTree=new LogicSyntaxTree();
		if(resStack.peek() instanceof String){
			lsTree.root=new LogicOperatorNode();;
			lsTree.root.opt=LOGOPT.LEAF;
			lsTree.root.str=(String)resStack.pop();
		}else{
			lsTree.root=(LogicOperatorNode)resStack.pop();
		}
		return lsTree;
	}

	/**
	 * 将中缀逻辑表达式解析成语法树
	 * 
	 * @param str
	 *            中缀逻辑表达式
	 * @return
	 * @throws Exception
	 */
	public static LogicSyntaxTree parse(String str) throws Exception {// (
		int lbCount = leftBracketCount(str);
		int rbCount = rightBracketCount(str);

		if (lbCount != rbCount) {
			throw new Exception("The number of left bracket is not equal to that of right bracket");
		}
		List<String> list = reversePolishTransformation(str);

		return reversePolishToSyntaxTree(list);
	}

	private static int leftBracketCount(String str) {
		int pos = -1;
		int count = 0;
		while ((pos + 1 < str.length()) && (pos = str.indexOf("(", pos + 1)) > -1) {
			count++;
		}
		return count;
	}

	private static int rightBracketCount(String str) {
		int pos = -1;
		int count = 0;
		while ((pos + 1 < str.length()) && (pos = str.indexOf(")", pos + 1)) > -1) {
			count++;
		}
		return count;
	}

//	private boolean recursive(LogicOperatorNode node,
//			List<String> list) {
//		if (node.opt == LOGOPT.LEAF) {
//			for (String st : list) {
//				if (st.indexOf(node.str) >= 0)
//					return true;
//			}
//			return false;
//		} else if (node.opt == LOGOPT.AND) {
//			return recursive(node.leftOperand, list) &&
//					recursiveContainsVerify(node.ritghOperand, list);
//		} else if (node.opt == LOGOPT.OR) {
//			return recursiveContainsVerify(node.leftOperand, list) ||
//					recursiveContainsVerify(node.ritghOperand, list);
//		} else {// root.opt==LOGOPT.NOT
//			return !recursiveContainsVerify(node.leftOperand, list);
//		}
//	}
//
//	private boolean recursiveContainsVerify(LogicOperatorNode node, String str) {
//		if (node.opt == LOGOPT.LEAF) {
//			if (str.indexOf(node.str) >= 0)
//				return true;
//			return false;
//		} else if (node.opt == LOGOPT.AND) {
//			return recursiveContainsVerify(node.leftOperand, str) &&
//					recursiveContainsVerify(node.ritghOperand, str);
//		} else if (node.opt == LOGOPT.OR) {
//			return recursiveContainsVerify(node.leftOperand, str) ||
//					recursiveContainsVerify(node.ritghOperand, str);
//		} else {// root.opt==LOGOPT.NOT
//			return !recursiveContainsVerify(node.leftOperand, str);
//		}
//	}
//	
//	public boolean containsVerify(String str) throws Exception {
//		if (this.root == null) {
//			throw new Exception("node is null");
//		}
//		return recursiveContainsVerify(this.root, str);
//	}
//	
//	/**
//	*
//	* @param list
//	* 各字段内容
//	* @return
//	* @throws Exception
//	*/
//	public boolean containsVerify(List<String> list) throws Exception {
//		if (this.root == null) {
//			throw new Exception("node is null");
//		}
//		return recursiveContainsVerify(this.root, list);
//	}
//
//	private boolean recursiveExactlyMatch(LogicOperatorNode node,
//			List<String> list) {
//		if (node.opt == LOGOPT.LEAF) {
//			for (String st : list) {
//				if (st.equals(node.str))
//					return true;
//			}
//			return false;
//		} else if (node.opt == LOGOPT.AND) {
//			return recursiveExactlyMatch(node.leftOperand, list) &&
//					recursiveExactlyMatch(node.ritghOperand, list);
//		} else if (node.opt == LOGOPT.OR) {
//			return recursiveExactlyMatch(node.leftOperand, list) ||
//					recursiveExactlyMatch(node.ritghOperand, list);
//		} else {// root.opt==LOGOPT.NOT
//			return !recursiveExactlyMatch(node.leftOperand, list);
//		}
//	}
//
//	/**
//	* @param list
//	* @return
//	* @throws Exception
//	*/
//	public boolean exactlyMatch(List<String> list) throws Exception {
//		if (this.root == null) {
//			throw new Exception("root is null");
//		}
//		return recursiveExactlyMatch(this.root, list);
//	}
//
	public static void main(String args[]) throws Exception {
		String str = "港大&泛民&拉票";
//		String str1 = "我是AND好人";
//		String str2 = "(我是|你是)&!好人";
		List<String> list = new ArrayList<String>() {
			{
				add("长按识别二维码 进入公众号回复【投票】参与投票 或者直接回复选手编号投票 详细了解参与方法 努力投票中 参与选手 255 累计投票 12960 访问量 00天00时00分00秒000 公告 报名就送儿童电子手表一块 活动规则 1、评选时间：2017年5月24日——5月29日23:55 2、报名方式：进入“赣榆看点”微信公众号，点击自定义菜单“我要报名”，或给公众号回复“我要报名”，打开页面，点击“我要报名”，填写资料报名，一个人只有一次报名机会。 3、投票方式：进入“赣榆看点”“拾点夜听”微信公众号，点击自定义菜单“我要投票”，或给公众号微信回复“我要投票”，打开页面，搜索选手姓名或编号，为其报名拉票。或者给公众号回复选手编号如“99”，按提示进行投票。以下两个公众号每天都可为选手投一票 4、拉票秘籍：进入“赣榆看点”公众号，点击自定义菜单“拉票图片”保存图片群发好友、分享朋友圈，或点击“投票入口”进入，找到相对应的运动员点击进入，点击右上方三个点分享朋友圈。 5、注意事项：一个微信可以对5个宝贝每天投1票。 6、详情请咨询微信：3227652312 2017-5-24 0:00:05 至 2017-5-29 23:55:50 活动介绍 投票攻略 一个微信号每天可为五名选手各投一票 以下两个公众号每天都可为选手投一票 投票规则 传播学校知名度，分享快乐童年，禁止恶意刷票，一经发现取消比赛资格！ 活动奖品 报名奖：报名即送儿童电子手表一块 参与奖奖（满20票）: 儿童精美故事书2本 最具人气四等奖（21-40名）：磁力儿童写字黑板一块 最具人气三等奖（8-20名）：儿童益智多功能电子琴一台 最具人气二等奖（2-7名）:咪咪兔儿童定位电话手表一块 最具人气一等奖（1名）：步步高点读机一台 获奖规则 按照最终得票数，由高到底排名。 照片要求 参赛者需提供1张真实照片，最多3张，要求清晰。  领奖地点：黑林金蕾幼儿园（黑林镇兴隆集村） 领奖时间：2015年5月30日上午9点 咨询微信：3227652312 奖品展示 金蕾幼儿园简介 叔叔阿姨快来看看我们的视频 黑林镇金蕾幼儿园位于连云港市赣榆区黑林镇兴隆集村，是一所民办幼儿园，于2014年创建。全园占地面积3180多平方米，建筑面积2640平方米, 户外活动场地 1827平方米，其中绿化面积735平方米。在园幼儿320余人，教职工27人，园内环境优美，拥有完善合理的教育设施和富于童趣的园所布局。坚持“关注孩子，关注家长，让孩子每天都快乐”的办园宗旨，不断改善办园条件，提高办园水平，于2016年10月创建为连云港省优质幼儿园。 我们的校园 江苏优质幼儿园评估现场 金蕾幼儿园的六大特色 1.开放幼儿园“天眼”全网络监控，您可以随时随地通过手机掌握孩子在学校的动态。 2.我园有安全的校车接送幼儿，通过幼儿宝教育平台，随时监控宝贝的入园、离园时间及体温动向。 3.我园有专业的幼小衔接课程体系，由原华杰小学部资深教师徐老师现任我园园长，带队整理了一套系统且符合幼儿发展的学习课程体系。 4.启动“国学经典促成长”的课程，打造新社会的国学萌宝。 5.科学营养配餐，为了培养幼儿自主能力，我们实行自主用餐（后厨自制点心）。 6.按照《3-6周岁发展指南》，依托课程游戏化精神，锻炼幼儿的攀、爬、跳以及平衡能力，我园进行了系统的改建，添置了各类体育活动需要的大型玩具和体育器械，其中大型组合器械一套，供30名以上幼儿玩耍。 图书一角 游戏中的我们 我们的园内 我们的节日 黑林镇金蕾幼儿园是一所具有影响力的省示范园，拥有高素质、待人热情真诚，有强烈的求知欲和进取心，能歌善舞的教师队伍。幼师尊重理解、信任幼儿，深受幼儿的喜爱和尊敬。 展望未来、催人奋进，我们将进一步深化健康发展园本课程的研究，全面推进素质教育，努力把幼儿园办成一所幼儿喜欢、家长满意、领导放心、社会信赖的品牌园。 搜索 166号  张睿 排名：212    票数：13 参赛宣言：宝贝最棒，加油 为TA投票 我要报名点击拉票 点击查看更多 投票攻略 一个微信号每天可为五名选手各投一票 以下两个公众号每天都可为选手投一票 投票规则 传播学校知名度，分享快乐童年，禁止恶意刷票，一经发现取消比赛资格！ 活动奖品 报名奖：报名即送儿童电子手表一块 参与奖奖（满20票）: 儿童精美故事书2本 最具人气四等奖（21-40名）：磁力儿童写字黑板一块 最具人气三等奖（8-20名）：儿童益智多功能电子琴一台 最具人气二等奖（2-7名）:咪咪兔儿童定位电话手表一块 最具人气一等奖（1名）：步步高点读机一台 获奖规则 按照最终得票数，由高到底排名。 照片要求 参赛者需提供1张真实照片，最多3张，要求清晰。  领奖地点：黑林金蕾幼儿园（黑林镇兴隆集村） 领奖时间：2015年5月30日上午9点 咨询微信：3227652312 奖品展示 金蕾幼儿园简介 叔叔阿姨快来看看我们的视频 黑林镇金蕾幼儿园位于连云港市赣榆区黑林镇兴隆集村，是一所民办幼儿园，于2014年创建。全园占地面积3180多平方米，建筑面积2640平方米, 户外活动场地 1827平方米，其中绿化面积735平方米。在园幼儿320余人，教职工27人，园内环境优美，拥有完善合理的教育设施和富于童趣的园所布局。坚持“关注孩子，关注家长，让孩子每天都快乐”的办园宗旨，不断改善办园条件，提高办园水平，于2016年10月创建为连云港省优质幼儿园。 我们的校园 江苏优质幼儿园评估现场 金蕾幼儿园的六大特色 1.开放幼儿园“天眼”全网络监控，您可以随时随地通过手机掌握孩子在学校的动态。 2.我园有安全的校车接送幼儿，通过幼儿宝教育平台，随时监控宝贝的入园、离园时间及体温动向。 3.我园有专业的幼小衔接课程体系，由原华杰小学部资深教师徐老师现任我园园长，带队整理了一套系统且符合幼儿发展的学习课程体系。 4.启动“国学经典促成长”的课程，打造新社会的国学萌宝。 5.科学营养配餐，为了培养幼儿自主能力，我们实行自主用餐（后厨自制点心）。 6.按照《3-6周岁发展指南》，依托课程游戏化精神，锻炼幼儿的攀、爬、跳以及平衡能力，我园进行了系统的改建，添置了各类体育活动需要的大型玩具和体育器械，其中大型组合器械一套，供30名以上幼儿玩耍。 图书一角 游戏中的我们 我们的园内 我们的节日 黑林镇金蕾幼儿园是一所具有影响力的省示范园，拥有高素质、待人热情真诚，有强烈的求知欲和进取心，能歌善舞的教师队伍。幼师尊重理解、信任幼儿，深受幼儿的喜爱和尊敬。 展望未来、催人奋进，我们将进一步深化健康发展园本课程的研究，全面推");
				add("好人");
			}
			
	};
	String str2 = "港大&泛民&拉票长按识别二维码 进入公众号回复【投票】参与投票 或者直接回复选手编号投港大票 详细了解参与方法 努力投票中 参与选手 255 累计投票 12960 泛民访问量 00天00时00分00秒000 公告 报名就送儿童电子手表一块 活动规则 1、评选时间：2017年5月24日——5月29日23:55 2、报名方式：进入“赣榆看点”微信公众号，点击自定义菜单“我要报名”，或给公众号回复“我要报名”，打开页面，点击“我要报名”，填写资料报名，一个人只有一次报名机会。 3、投票方式：进入“赣榆看点”“拾点夜听”微信公众号，点击自定义菜单“我要投票”，或给公众号微信回复“我要投票”，打开页面，搜索选手姓名或编号，为其报名拉票。或者给公众号回复选手编号如“99”，按提示进行投票。以下两个公众号每天都可为选手投一票 4、拉票秘籍：进入“赣榆看点”公众号，点击自定义菜单“拉票图片”保存图片群发好友、分享朋友圈，或点击“投票入口”进入，找到相对应的运动员点击进入，点击右上方三个点分享朋友圈。 5、注意事项：一个微信可以对5个宝贝每天投1票。 6、详情请咨询微信：3227652312 2017-5-24 0:00:05 至 2017-5-29 23:55:50 活动介绍 投票攻略 一个微信号每天可为五名选手各投一票 以下两个公众号每天都可为选手投一票 投票规则 传播学校知名度，分享快乐童年，禁止恶意刷票，一经发现取消比赛资格！ 活动奖品 报名奖：报名即送儿童电子手表一块 参与奖奖（满20票）: 儿童精美故事书2本 最具人气四等奖（21-40名）：磁力儿童写字黑板一块 最具人气三等奖（8-20名）：儿童益智多功能电子琴一台 最具人气二等奖（2-7名）:咪咪兔儿童定位电话手表一块 最具人气一等奖（1名）：步步高点读机一台 获奖规则 按照最终得票数，由高到底排名。 照片要求 参赛者拉票需提供1张真实照片，最多3张，要求清晰。  领奖地点：黑林金蕾幼儿园（黑林镇兴隆集村） 领奖时间：2015年5月30日上午9点 咨询微信：3227652312 奖品展示 金蕾幼儿园简介 叔叔阿姨快来看看我们的视频 黑林镇金蕾幼儿园位于连云港市赣榆区黑林镇兴隆集村，是一所民办幼儿园，于2014年创建。全园占地面积3180多平方米，建筑面积2640平方米, 户外活动场地 1827平方米，其中绿化面积735平方米。在园幼儿320余人，教职工27人，园内环境优美，拥有完善合理的教育设施和富于童趣的园所布局。坚持“关注孩子，关注家长，让孩子每天都快乐”的办园宗旨，不断改善办园条件，提高办园水平，于2016年10月创建为连云港省优质幼儿园。 我们的校园 江苏优质幼儿园评估现场 金蕾幼儿园的六大特色 1.开放幼儿园“天眼”全网络监控，您可以随时随地通过手机掌握孩子在学校的动态。 2.我园有安全的校车接送幼儿，通过幼儿宝教育平台，随时监控宝贝的入园、离园时间及体温动向。 3.我园有专业的幼小衔接课程体系，由原华杰小学部资深教师徐老师现任我园园长，带队整理了一套系统且符合幼儿发展的学习课程体系。 4.启动“国学经典促成长”的课程，打造新社会的国学萌宝。 5.科学营养配餐，为了培养幼儿自主能力，我们实行自主用餐（后厨自制点心）。 6.按照《3-6周岁发展指南》，依托课程游戏化精神，锻炼幼儿的攀、爬、跳以及平衡能力，我园进行了系统的改建，添置了各类体育活动需要的大型玩具和体育器械，其中大型组合器械一套，供30名以上幼儿玩耍。 图书一角 游戏中的我们 我们的园内 我们的节日 黑林镇金蕾幼儿园是一所具有影响力的省示范园，拥有高素质、待人热情真诚，有强烈的求知欲和进取心，能歌善舞的教师队伍。幼师尊重理解、信任幼儿，深受幼儿的喜爱和尊敬。 展望未来、催人奋进，我们将进一步深化健康发展园本课程的研究，全面推进素质教育，努力把幼儿园办成一所幼儿喜欢、家长满意、领导放心、社会信赖的品牌园。 搜索 166号  张睿 排名：212    票数：13 参赛宣言：宝贝最棒，加油 为TA投票 我要报名点击拉票 点击查看更多 投票攻略 一个微信号每天可为五名选手各投一票 以下两个公众号每天都可为选手投一票 投票规则 传播学校知名度，分享快乐童年，禁止恶意刷票，一经发现取消比赛资格！ 活动奖品 报名奖：报名即送儿童电子手表一块 参与奖奖（满20票）: 儿童精美故事书2本 最具人气四等奖（21-40名）：磁力儿童写字黑板一块 最具人气三等奖（8-20名）：儿童益智多功能电子琴一台 最具人气二等奖（2-7名）:咪咪兔儿童定位电话手表一块 最具人气一等奖（1名）：步步高点读机一台 获奖规则 按照最终得票数，由高到底排名。 照片要求 参赛者需提供1张真实照片，最多3张，要求清晰。  领奖地点：黑林金蕾幼儿园（黑林镇兴隆集村） 领奖时间：2015年5月30日上午9点 咨询微信：3227652312 奖品展示 金蕾幼儿园简介 叔叔阿姨快来看看我们的视频 黑林镇金蕾幼儿园位于连云港市赣榆区黑林镇兴隆集村，是一所民办幼儿园，于2014年创建。全园占地面积3180多平方米，建筑面积2640平方米, 户外活动场地 1827平方米，其中绿化面积735平方米。在园幼儿320余人，教职工27人，园内环境优美，拥有完善合理的教育设施和富于童趣的园所布局。坚持“关注孩子，关注家长，让孩子每天都快乐”的办园宗旨，不断改善办园条件，提高办园水平，于2016年10月创建为连云港省优质幼儿园。 我们的校园 江苏优质幼儿园评估现场 金蕾幼儿园的六大特色 1.开放幼儿园“天眼”全网络监控，您可以随时随地通过手机掌握孩子在学校的动态。 2.我园有安全的校车接送幼儿，通过幼儿宝教育平台，随时监控宝贝的入园、离园时间及体温动向。 3.我园有专业的幼小衔接课程体系，由原华杰小学部资深教师徐老师现任我园园长，带队整理了一套系统且符合幼儿发展的学习课程体系。 4.启动“国学经典促成长”的课程，打造新社会的国学萌宝。 5.科学营养配餐，为了培养幼儿自主能力，我们实行自主用餐（后厨自制点心）。 6.按照《3-6周岁发展指南》，依托课程游戏化精神，锻炼幼儿的攀、爬、跳以及平衡能力，我园进行了系统的改建，添置了各类体育活动需要的大型玩具和体育器械，其中大型组合器械一套，供30名以上幼儿玩耍。 图书一角 游戏中的我们 我们的园内 我们的节日 黑林镇金蕾幼儿园是一所具有影响力的省示范园，拥有高素质、待人热情真诚，有强烈的求知欲和进取心，能歌善舞的教师队伍。幼师尊重理解、信任幼儿，深受幼儿的喜爱和尊敬。 展望未来、催人奋进，我们将进一步深化健康发展园本课程的研究，全面推";
	String str3 = "进入公众号回复【投票】参港大与投票";	
	LogicSyntaxTree tree = parse(str);
//	LogicSyntaxTree tree1 = parse(str1);
//	LogicSyntaxTree tree2 = parse(str2);
	
	
//	System.out.println(tree.containsVerify(str3));
//	System.out.println(tree.exactlyMatch(str2));
	
//	System.out.println(tree1.containsVerify(list));
	//System.out.println(tree1.exactlyMatch(list));
	
//	System.out.println(tree2.containsVerify(list));
	//System.out.println(tree2.exactlyMatch(list));
	}
}
