package LIPSin.models;

import com.google.gson.annotations.Expose;
import java.util.Date;
import java.util.LinkedHashMap;

public class TpOpSeason {
	@Expose private String name;
	@Expose private String season;
	//Use a unique key here to identify airline destination origins that operate the same days!!! Example: TOM-AYT-0101001
	@Expose private LinkedHashMap<String, TpOpRoute> tpOpRoutes = new LinkedHashMap<>();
	@Expose private Date startDate;
	@Expose private Date endDate;

	public TpOpSeason(String name, String season, Date startDate, Date endDate) {
		super();
		this.name = name;
		this.season = season;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public String getSeason() {
		return season;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public LinkedHashMap<String, TpOpRoute> getTpOpRoutes() {
		return tpOpRoutes;
	}
	public void addTpOpRoute(String key, TpOpRoute tpOpRoute){
		tpOpRoutes.put(key, tpOpRoute);
    }
	public Date getStartDate() {
		return startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
}
