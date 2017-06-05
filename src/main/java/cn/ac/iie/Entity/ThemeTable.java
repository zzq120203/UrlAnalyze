package cn.ac.iie.Entity;

import java.util.ArrayList;

public class ThemeTable {
	private long t_id;
	private String t_name;
	private int t_type;
	private long t_update_time;
	private int up_user_id;
	private ArrayList<Long> t_topic_list;

	public long getT_id() {
		return this.t_id;
	}

	public void setT_id(long t_id) {
		this.t_id = t_id;
	}

	public String getT_name() {
		return this.t_name;
	}

	public void setT_name(String t_name) {
		this.t_name = t_name;
	}

	public int getT_type() {
		return this.t_type;
	}

	public void setT_type(int t_type) {
		this.t_type = t_type;
	}

	public long getT_update_time() {
		return this.t_update_time;
	}

	public void setT_update_time(long t_update_time) {
		this.t_update_time = t_update_time;
	}

	public int getUp_user_id() {
		return this.up_user_id;
	}

	public void setUp_user_id(int up_user_id) {
		this.up_user_id = up_user_id;
	}

	public ArrayList<Long> getT_topic_list() {
		return this.t_topic_list;
	}

	public void setT_topic_list(ArrayList<Long> t_topic_list) {
		this.t_topic_list = t_topic_list;
	}
}
