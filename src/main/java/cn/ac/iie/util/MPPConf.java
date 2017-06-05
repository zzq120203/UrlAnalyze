package cn.ac.iie.util;

import java.util.List;

public class MPPConf {
	
	private List<String> updateFields;
	private boolean update;
	public MPPConf(List<String> updateFields, boolean update) {
		super();
		this.updateFields = updateFields;
		this.update = update;
	}
	public List<String> getUpdateFields() {
		return updateFields;
	}
	public void setUpdateFields(List<String> updateFields) {
		this.updateFields = updateFields;
	}
	public boolean isUpdate() {
		return update;
	}
	public void setUpdate(boolean update) {
		this.update = update;
	}
	
}
