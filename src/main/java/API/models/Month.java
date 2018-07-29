package API.models;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.ArrayList;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "MonthNumber",
    "YearNumber",
    "FlightDates"
})
public class Month implements Serializable{
	@JsonProperty("FlightDates")
    @Expose public ArrayList<Integer> FlightDates = new ArrayList<>();
	@JsonProperty("YearNumber")
	@Expose public int YearNumber;
	@JsonProperty("MonthNumber")
	@Expose public int MonthNumber;
	@JsonIgnore
	@Expose public String Gap = "false";

	public Month() { }

	public Month(int yearNumber, int monthNumber) {
		YearNumber = yearNumber;
		MonthNumber = monthNumber;
	}
}
