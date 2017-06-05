package cn.ac.iie.Entity;

public class RulesTable {
	private long r_id;

	private long r_update_time;

	private int up_user_id;

	private int rule_type;
	private String rule;
	private long tp_id;

	public long getR_id() {
		return this.r_id;
	}

	public void setR_id(long r_id) {
		this.r_id = r_id;
	}

	public long getR_update_time() {
		return this.r_update_time;
	}

	public void setR_update_time(long r_update_time) {
		this.r_update_time = r_update_time;
	}

	public int getUp_user_id() {
		return this.up_user_id;
	}

	public void setUp_user_id(int up_user_id) {
		this.up_user_id = up_user_id;
	}

	public int getRule_type() {
		return this.rule_type;
	}

	public void setRule_type(int rule_type) {
		this.rule_type = rule_type;
	}

	public String getRule() {
		return this.rule;
	}

	public void setRule(String rule) {
		this.rule = rule;
	}

	public long getTp_id() {
		return this.tp_id;
	}

	public void setTp_id(long tp_id) {
		this.tp_id = tp_id;
	}

	public String toString() {
		return "RulesTable "
				+ "[r_id=" 				+ this.r_id 
				+ ", r_update_time=" 	+ this.r_update_time 
				+ ", up_user_id=" 		+ this.up_user_id 
				+ ", rule_type=" 		+ this.rule_type 
				+ ", rule=" 			+ this.rule 
				+ ", tp_id=" 			+ this.tp_id
				+ "]";
	}
}
