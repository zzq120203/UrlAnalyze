package cn.ac.iie.Entity;

public class URLInfoEntity {
	private String g_id;
	private String url;
	private String domain;
	private long m_chat_room;		//群ID
	private long u_ch_id;			//用户ID
	private long m_publish_time;	//发送时间
	private String u_send_ip;
	private String m_content;
	private int u_loc_county;
	private int u_loc_city;
	private int u_loc_province;
	private int m_dom_for;
	private int m_country_code;
	private String u_name;

	public URLInfoEntity() {
	}

	public URLInfoEntity(String g_id, String url, String domain, long m_chat_room, long u_ch_id, long m_publish_time,
			String u_send_ip,String m_content, int u_loc_county, int u_loc_city, int u_loc_province, String u_name) {
		this.g_id = g_id;
		this.url = url;
		this.m_chat_room = m_chat_room;
		this.u_ch_id = u_ch_id;
		this.m_publish_time = m_publish_time;
		this.m_content = m_content;
		this.u_send_ip = u_send_ip;
		this.u_loc_county = u_loc_county;
		this.u_loc_city = u_loc_city;
		this.u_loc_province = u_loc_province;
		this.domain = domain;
		this.u_name = u_name;
	}

	public String getG_id() {
		return g_id;
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

	public long getM_chat_room() {
		return this.m_chat_room;
	}

	public void setM_chat_room(long m_chat_room) {
		this.m_chat_room = m_chat_room;
	}

	public long getU_ch_id() {
		return this.u_ch_id;
	}

	public void setU_ch_id(long u_ch_id) {
		this.u_ch_id = u_ch_id;
	}

	public long getM_publish_time() {
		return this.m_publish_time;
	}

	public void setM_publish_time(long m_publish_time) {
		this.m_publish_time = m_publish_time;
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
	
	public int getU_loc_county() {
		return this.u_loc_county;
	}

	public void setU_loc_county(int u_loc_county) {
		this.u_loc_county = u_loc_county;
	}

	public int getU_loc_province() {
		return this.u_loc_province;
	}

	public void setU_loc_province(int u_loc_province) {
		this.u_loc_province = u_loc_province;
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
	
	public String getU_name() {
		return u_name;
	}

	public void setU_name(String u_name) {
		this.u_name = u_name;
	}

	public int getU_loc_city() {
		return u_loc_city;
	}

	public void setU_loc_city(int u_loc_city) {
		this.u_loc_city = u_loc_city;
	}
	
	@Override
	public String toString() {
		return "URLInfoEntity"
				+ " [g_id=" 			+ g_id 
				+ ", url=" 				+ url 
				+ ", domain=" 			+ domain 
				+ ", m_chat_room=" 		+ m_chat_room 
				+ ", u_ch_id=" 			+ u_ch_id 
				+ ", m_publish_time=" 	+ m_publish_time 
				+ ", u_send_ip=" 		+ u_send_ip 
				+ ", m_content=" 		+ m_content 
				+ ", u_loc_province=" 	+ u_loc_province 
				+ ", u_loc_city=" 		+ u_loc_city 
				+ ", u_loc_county=" 	+ u_loc_county 
				+ ", m_dom_for=" 		+ m_dom_for 
				+ ", m_country_code=" 	+ m_country_code 
				+ ", u_name=" 			+ u_name 
				+ "]";
	}
	
	
	
}
