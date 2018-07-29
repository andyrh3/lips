package LIPS.models;

import java.util.Date;
import java.util.LinkedHashMap;
import com.google.gson.annotations.Expose;

public class OpSeason {
	@Expose private String name;
	@Expose private String season;
	@Expose private LinkedHashMap<String, OpAirport> destinations = new LinkedHashMap<>();
	@Expose private Date startDate;
	@Expose private Date endDate;
	
	public OpSeason(String name, String season, Date startDate, Date endDate) {
		super();
		this.name = name;
		this.season = season;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public LinkedHashMap<String, OpAirport> getDestinations() {
		return destinations;
	}
	public void addAirport(OpAirport airport){
		this.destinations.put(airport.getCode(), airport);
	}
	public Date getStartDate() {
		return startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
}
