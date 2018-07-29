package LIPS.models;

import java.time.DayOfWeek;
import java.util.LinkedList;

import com.google.gson.annotations.Expose;

public class OpDay {
	@Expose private String name;
	@Expose private int ordinal;
	@Expose private LinkedList<OpRange> opRanges = new LinkedList<>();

	OpDay(String name, int ordinal) {
		super();
		this.name = name;
		this.ordinal = ordinal;
	}

	public OpDay(DayOfWeek dayOfWeek){
        super();
        this.name = dayOfWeek.name();
        this.ordinal = dayOfWeek.ordinal();
    }

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public int getOrdinal() {
		return ordinal;
	}

	public LinkedList<OpRange> getOpRanges() {
		return opRanges;
	}

	public void addOpRange(OpRange opRange) {
		this.opRanges.add(opRange);
	}


}
