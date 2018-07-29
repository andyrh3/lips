package LIPS.models;

import java.util.LinkedHashMap;
import com.google.gson.annotations.Expose;

public class OpAirport {
	
	@Expose private String code;
    @Expose private String direction;
    @Expose private LinkedHashMap<String, OpAirport> origins = new LinkedHashMap<>();
	@Expose private LinkedHashMap<String, OpDay> opDays = new LinkedHashMap<>();

	public OpAirport(String code, String direction) {
		super();
		this.code = code;
        this.direction = direction;
	}

	public String getCode() {
		return code;
	}

	public LinkedHashMap<String, OpAirport> getAirports() {
		return origins;
	}

	public void addAirport(OpAirport airport){
		this.origins.put(airport.getCode(), airport);
	}

	public LinkedHashMap<String, OpDay> getOpDays() {
		return opDays;
	}

	public OpDay getOpDay(String weekDay){
		return opDays.getOrDefault(weekDay,null);
	}

    public void setOpDays(LinkedHashMap<String, OpDay> opDays) {
        this.opDays = opDays;
    }

    public void putOpDay(String weekDayShortName, String weekDayLongName, int ordinal){
		opDays.put(weekDayShortName, new OpDay(weekDayLongName, ordinal));
	}

	public LinkedHashMap<String, OpAirport> getOrigins() {
		return origins;
	}

	public void addOpDay(String weekDayShortName, OpDay opDay){
		opDays.put(weekDayShortName, opDay);
	}
	
}
