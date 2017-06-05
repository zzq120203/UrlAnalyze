package cn.ac.iie.Entity;

import java.util.List;

public class URLDetailEntity {
	private String g_id;			//外链Id，主键
	private String url;				//外链url
	private String u_name;			//用户名
	private long u_ch_id;			//发送者唯一id
	private long m_chat_room;		//群id
	private long m_publish_time;	//外链发布时间
	private long m_insert_time;		//入库时间
	private String u_send_ip;		//发布者ip
	private String m_content; 		//消息内容
	private int u_loc_province;		//省归属地编号
	private int u_loc_city;			//市归属地编号
	private int u_loc_county;		//县归属地编号
	private String domain;			//url解析的域名
	private String url_title;		//外链标题
	private String url_content;		//外链内容
	private List<Long> t_id;		//外链主题
	private List<Long> tp_id;		//外链专题
	private List<Long> rules;		//命中规则
	private int u_is_harm;			//是否有害
	private int u_is_dispose;		//是否处置
	private int u_is_target;		//是否命中
	private int m_dom_for;			//标记此条消息是境内还是境外，境内为1，境外为-1
	private int m_country_code;		//国家地域编码
	private int operator_id;		//操作员id
	private long u_dispose_uptime;	//处置时间

	public String getU_name() {
		return u_name;
	}

	public void setU_name(String u_name) {
		this.u_name = u_name;
	}

	public String toString() {
		return "URLDetailEntity "
				+ "[g_id=" 			+ this.g_id 
				+ ", url=" 				+ this.url 
				+ ", u_ch_id=" 			+ this.u_ch_id 
				+ ", m_chat_room=" 		+ this.m_chat_room 
				+ ", m_publish_time=" 	+ this.m_publish_time 
				+ ", m_insert_time=" 	+ this.m_insert_time
				+ ", u_send_ip=" 		+ this.u_send_ip 
				+ ", m_content=" 		+ this.m_content 
				+ ", u_loc_province=" 	+ this.u_loc_province 
				+ ", u_loc_city=" 		+ this.u_loc_city 
				+ ", u_loc_county=" 	+ this.u_loc_county 
				+ ", domain=" 			+ this.domain 
				+ ", url_title="		+ this.url_title 
				+ ", url_content="		+ this.url_content 
				+ ", t_id=" 			+ this.t_id 
				+ ", tp_id=" 			+ this.tp_id 
				+ ", rules=" 			+ this.rules
				+ ", u_is_harm=" 		+ this.u_is_harm 
				+ ", u_is_dispose=" 	+ this.u_is_dispose 
				+ ", u_is_target=" 		+ this.u_is_target 
				+ ", m_dom_for=" 		+ this.m_dom_for 
				+ ", m_country_code=" 	+ this.m_country_code 
				+ ", operator_id=" 		+ this.operator_id 
				+ ", u_dispose_uptime=" + this.u_dispose_uptime 
				+ ", u_name=" 			+ this.u_name
				+ "]";
	}

	public String getG_id() {
		return this.g_id;
	}

	public void setG_id(String g_id) {
		this.g_id = g_id;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public long getU_ch_id() {
		return this.u_ch_id;
	}

	public void setU_ch_id(long u_ch_id) {
		this.u_ch_id = u_ch_id;
	}

	public long getM_chat_room() {
		return this.m_chat_room;
	}

	public void setM_chat_room(long m_chat_room) {
		this.m_chat_room = m_chat_room;
	}

	public long getM_publish_time() {
		return this.m_publish_time;
	}

	public void setM_publish_time(long m_publish_time) {
		this.m_publish_time = m_publish_time;
	}

	public long getM_insert_time() {
		return this.m_insert_time;
	}

	public void setM_insert_time(long m_insert_time) {
		this.m_insert_time = m_insert_time;
	}

	public String getU_send_ip() {
		return this.u_send_ip;
	}

	public void setU_send_ip(String u_send_ip) {
		this.u_send_ip = u_send_ip;
	}

	public String getM_content() {
		return m_content;
	}

	public void setM_content(String m_content) {
		this.m_content = m_content;
	}

	public int getU_loc_province() {
		return this.u_loc_province;
	}

	public void setU_loc_province(int u_loc_province) {
		this.u_loc_province = u_loc_province;
	}

	public int getU_loc_city() {
		return u_loc_city;
	}

	public void setU_loc_city(int u_loc_city) {
		this.u_loc_city = u_loc_city;
	}

	public int getU_loc_county() {
		return this.u_loc_county;
	}

	public void setU_loc_county(int u_loc_county) {
		this.u_loc_county = u_loc_county;
	}

	public String getDomain() {
		return this.domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public int getM_dom_for() {
		return this.m_dom_for;
	}

	public void setM_dom_for(int m_dom_for) {
		this.m_dom_for = m_dom_for;
	}

	public int getM_country_code() {
		return this.m_country_code;
	}

	public void setM_country_code(int m_country_code) {
		this.m_country_code = m_country_code;
	}

	public String getUrl_title() {
		return this.url_title;
	}

	public void setUrl_title(String url_title) {
		this.url_title = url_title;
	}

	public String getUrl_content() {
		return this.url_content;
	}

	public void setUrl_content(String url_content) {
		this.url_content = url_content;
	}

	public List<Long> getT_id() {
		return this.t_id;
	}

	public void setT_id(List<Long> t_id) {
		this.t_id = t_id;
	}

	public List<Long> getTp_id() {
		return this.tp_id;
	}

	public void setTp_id(List<Long> tp_id) {
		this.tp_id = tp_id;
	}

	public List<Long> getRules() {
		return this.rules;
	}

	public void setRules(List<Long> rules) {
		this.rules = rules;
	}

	public int getU_is_harm() {
		return this.u_is_harm;
	}

	public void setU_is_harm(int u_is_harm) {
		this.u_is_harm = u_is_harm;
	}

	public int getU_is_dispose() {
		return this.u_is_dispose;
	}

	public void setU_is_dispose(int u_is_dispose) {
		this.u_is_dispose = u_is_dispose;
	}

	public int getU_is_target() {
		return this.u_is_target;
	}

	public void setU_is_target(int u_is_target) {
		this.u_is_target = u_is_target;
	}

	public int getOperator_id() {
		return operator_id;
	}

	public void setOperator_id(int operator_id) {
		this.operator_id = operator_id;
	}

	public long getU_dispose_uptime() {
		return u_dispose_uptime;
	}

	public void setU_dispose_uptime(long u_dispose_uptime) {
		this.u_dispose_uptime = u_dispose_uptime;
	}
	
}
