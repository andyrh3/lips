package API.models;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class Airport implements Serializable{
	
	private static final long serialVersionUID = 1L;

	@Expose private String name;
	@Expose private String code;
	@Expose private String mapping;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMapping() {
		return mapping;
	}
	public void setMapping(String mapping) {
		this.mapping = mapping;
	}
	public Airport(String name, String code, String mapping) {
		super();
		this.name = name;
		this.code = code;
		this.mapping = mapping; //code or airline specific ref
	}

}
