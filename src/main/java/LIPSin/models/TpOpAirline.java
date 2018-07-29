package LIPSin.models;

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.ArrayList;

public class TpOpAirline implements Serializable{

	private static final long serialVersionUID = 1L;

	@Expose private String name;
	@Expose private String code;
	@Expose private ArrayList<TpOpRoute> tpOpRoutes = new ArrayList<>();

	public TpOpAirline(String name, String code) {
		super();
		this.name = name;
		this.code = code;
	}

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

	public ArrayList<TpOpRoute> getTpOpRoutes() {
		return tpOpRoutes;
	}

	public void setTpOpRoutes(ArrayList<TpOpRoute> tpOpRoutes) {
		this.tpOpRoutes = tpOpRoutes;
	}

	public void addTpOpRoute(TpOpRoute tpOpRoute) {
		this.tpOpRoutes.add(tpOpRoute);
	}

}
