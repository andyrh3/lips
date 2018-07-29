package LIPS.models;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;

import com.google.gson.annotations.Expose;

public class OpRange implements Serializable{
	
	private static final long serialVersionUID = 857873823916468934L;
	
	@Expose private Date startDate;
	@Expose private Date endDate;
	@Expose private int mainstreamAlloc;
	@Expose private int seatonlyAlloc;
	private int mainstreamLoad = 0;
	private int seatonlyLoad = 0;
	@Expose private boolean isGap = false;
	@Expose private LinkedHashMap<String, OpAirline> airlines = new LinkedHashMap<String, OpAirline>();

	public OpRange(){}
	
	public OpRange(Date startDate, Date endDate){
		this.startDate = startDate;
		this.endDate = endDate;
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
	public long getMainstreamAlloc() {
		return mainstreamAlloc;
	}
	public void setMainstreamAlloc(int mainstreamAlloc) {
		this.mainstreamAlloc = mainstreamAlloc;
	}
	public long getSeatonlyAlloc() {
		return seatonlyAlloc;
	}
	public void setSeatonlyAlloc(int seatonlyAlloc) {
		this.seatonlyAlloc = seatonlyAlloc;
	}
	
	public int getMainstreamLoad() {
		return mainstreamLoad;
	}

	public void setMainstreamLoad(int mainstreamLoad) {
		this.mainstreamLoad = mainstreamLoad;
	}

	public int getSeatonlyLoad() {
		return seatonlyLoad;
	}

	public void setSeatonlyLoad(int seatonlyLoad) {
		this.seatonlyLoad = seatonlyLoad;
	}

	public boolean isGap() {
		return isGap;
	}
	public void setGap(boolean isGap) {
		this.isGap = isGap;
	}
	public LinkedHashMap<String, OpAirline> getAirlines() {
		return airlines;
	}
	public OpAirline getOpAirline(String name){
		return this.airlines.get(name);
	}
	public void setAirlines(LinkedHashMap<String, OpAirline> airlines) {
		this.airlines = airlines;
	}
	public void clearAirlines(){
		this.airlines.clear();
	}
	
	public void addAirline(OpAirline opAirline){
		this.airlines.put(opAirline.getCode(), opAirline);
	}
	
}
