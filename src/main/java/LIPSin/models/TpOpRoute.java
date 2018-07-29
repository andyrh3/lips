package LIPSin.models;

import com.google.gson.annotations.Expose;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class TpOpRoute {
	@Expose private String airline;
	@Expose private String destination;
	@Expose private ArrayList<String> origins = new ArrayList<>();
	@Expose private int[] opDayBits = {0,0,0,0,0,0,0};
	@Expose private Date startDate;
	@Expose private Date endDate;

	ArrayList<String> weekdayShortNamesOrder = new ArrayList<>(Arrays.asList("SUN","MON","TUE","WED","THU","FRI","SAT"));

	public TpOpRoute(String destination) {
		super();
		this.destination = destination;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public ArrayList<String> getOrigins() {
		return origins;
	}

	public void addOrigin(String origin) {
		this.origins.add(origin);
	}

	public void addOrigins(ArrayList<String> origins) {
		this.origins.addAll(origins);
	}
	public int[] getOpDaysBits() {
		return opDayBits;
	}

	public void setOpDaysBits(int[] opDaysBits) {
		this.opDayBits = opDaysBits;
	}

	public void setOpDaysBit(String opDayShortName) {
		this.opDayBits[ weekdayShortNamesOrder.indexOf(opDayShortName)] = 1;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getAirline() {
		return airline;
	}

	public void setAirline(String airline) {
		this.airline = airline;
	}
}
