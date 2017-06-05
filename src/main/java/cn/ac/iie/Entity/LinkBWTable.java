package cn.ac.iie.Entity;

public class LinkBWTable {
	private long bw_list_id;

	private String bw_url;

	private int bw_type;
	private int u_user_id;
	private long url_insert_time;

	public long getBw_list_id() {
		return this.bw_list_id;
	}

	public void setBw_list_id(long bw_list_id) {
		this.bw_list_id = bw_list_id;
	}

	public String getBw_url() {
		return this.bw_url;
	}

	public void setBw_url(String bw_url) {
		this.bw_url = bw_url;
	}

	public int getBw_type() {
		return this.bw_type;
	}

	public void setBw_type(int bw_type) {
		this.bw_type = bw_type;
	}

	public int getU_user_id() {
		return this.u_user_id;
	}

	public void setU_user_id(int u_user_id) {
		this.u_user_id = u_user_id;
	}

	public long getUrl_insert_time() {
		return this.url_insert_time;
	}

	public void setUrl_insert_time(long url_insert_time) {
		this.url_insert_time = url_insert_time;
	}
}
