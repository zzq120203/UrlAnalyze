package cn.ac.iie.Entity;

import java.util.ArrayList;

public class TopicTable {
	private long tp_id;
	private String tp_name;
	private long tp_update_time;
	private int up_user_id;
	private long tp_t_id;
	private ArrayList<Long> r_id_list;

	public TopicTable() {
		this.r_id_list = new ArrayList();
	}

	public long getTp_id() {
		return this.tp_id;
	}

	public void setTp_id(long tp_id) {
		this.tp_id = tp_id;
	}

	public String getTp_name() {
		return this.tp_name;
	}

	public void setTp_name(String tp_name) {
		this.tp_name = tp_name;
	}

	public long getTp_update_time() {
		return this.tp_update_time;
	}

	public void setTp_update_time(long tp_update_time) {
		this.tp_update_time = tp_update_time;
	}

	public int getUp_user_id() {
		return this.up_user_id;
	}

	public void setUp_user_id(int up_user_id) {
		this.up_user_id = up_user_id;
	}

	public long getTp_t_id() {
		return this.tp_t_id;
	}

	public void setTp_t_id(long tp_t_id) {
		this.tp_t_id = tp_t_id;
	}

	public ArrayList<Long> getR_id_list() {
		return this.r_id_list;
	}

	public void setR_id_list(ArrayList<Long> r_id_list) {
		this.r_id_list = r_id_list;
	}

	public String toString() {
		return "TopicTable "
				+  "[tp_id=" 			+ this.tp_id 
				+ ", tp_name=" 			+ this.tp_name 
				+ ", tp_update_time=" 	+ this.tp_update_time 
				+ ", up_user_id=" 		+ this.up_user_id 
				+ ", tp_t_id=" 			+ this.tp_t_id 
				+ ", r_id_list=" 		+ this.r_id_list 
				+ "]";
	}

	public TopicTable(ArrayList<Long> r_id_list) {
		this.r_id_list = r_id_list;
	}
}
