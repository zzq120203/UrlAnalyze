package cn.ac.iie.Entity;

import cn.ac.iie.util.DesUtils;

public class URLData {
	private String g_id;
	private String url;
	private String u_ch_id;			//发送者唯一id
	private String m_chat_room;		//群id
	private long m_publish_time;	//发送时间
	private int u_is_target;		//是否命中
	
	public URLData() {
	}
	
	public URLData(String g_id
			, String url
			, String u_ch_id
			, String m_chat_room
			, long m_publish_time
			, int u_is_target
		) {
		super();
		this.g_id = g_id;
		this.url = url;
		this.u_ch_id = u_ch_id;
		this.m_chat_room = m_chat_room;
		this.m_publish_time = m_publish_time;
		this.u_is_target = u_is_target;
	}
	
	public URLData(URLInfoEntity urlie) {
		super();
		this.g_id = urlie.getG_id();
		this.url = urlie.getUrl();
		this.u_ch_id = DesUtils.instance().encrypt(urlie.getU_ch_id() + "");
		this.m_chat_room = DesUtils.instance().encrypt(urlie.getM_chat_room() + "");
		this.m_publish_time = urlie.getM_publish_time();
		this.u_is_target = 0;
	}
	
	public String getG_id() {
		return g_id;
	}

	public void setG_id(String g_id) {
		this.g_id = g_id;
	}

	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getU_ch_id() {
		return u_ch_id;
	}

	public void setU_ch_id(String u_ch_id) {
		this.u_ch_id = u_ch_id;
	}

	public String getM_chat_room() {
		return m_chat_room;
	}

	public void setM_chat_room(String m_chat_room) {
		this.m_chat_room = m_chat_room;
	}

	public long getM_publish_time() {
		return m_publish_time;
	}

	public void setM_publish_time(long m_publish_time) {
		this.m_publish_time = m_publish_time;
	}

	public int getU_is_target() {
		return u_is_target;
	}

	public void setU_is_target(int u_is_target) {
		this.u_is_target = u_is_target;
	}

	@Override
	public String toString() {
		return "{"
				+ "\"url_id\":" 				+ "\"" + g_id + "\""
				+ ", \"url\":" 				+ "\"" + url + "\""
				+ ", \"u_ch_id\":" 			+ "\"" + u_ch_id + "\""
				+ ", \"m_chat_room\":" 		+ "\"" + m_chat_room + "\""
				+ ", \"m_publish_time\":" 	+ m_publish_time
				+ ", \"u_is_target\":" 		+ u_is_target
				+ "}";
	}
}
