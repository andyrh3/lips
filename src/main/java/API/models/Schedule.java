package API.models;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "ScheduleUnknown",
    "ScheduleStarted",
    "ScheduleEnded",
    "Months"
})
public class Schedule implements Serializable{
		@JsonProperty("ScheduleUnknown")
		@Expose public boolean ScheduleUnknown;
		@JsonProperty("ScheduleStarted")
		@Expose public boolean ScheduleStarted;
		@JsonProperty("ScheduleEnded")
		@Expose public boolean ScheduleEnded;
		@JsonProperty("Months")
		@Expose public ArrayList<Month> Months = new ArrayList<>();
		@Expose public ArrayList<String> DaysOfweek = new ArrayList<>();
		@JsonProperty("DateCreated")
		@Expose public LocalDate DateCreated;

		public Schedule(){
			DateCreated = LocalDate.now();
		}
		
		public Month getMonth(int yearNumber, int monthNumber){
			return Months.stream().filter(m -> (m.MonthNumber==monthNumber && m.YearNumber==yearNumber)).findFirst().orElse(null);
		}

		public ArrayList<Month> getMonths() {
			return Months;
		}

		public void setMonths(ArrayList<Month> months) {
			Months = months;
		}
		
		public void addMonth(Month month) {
			Months.add(month);
		}

		public void addDayofWeek(String dayOfWeek){
			this.DaysOfweek.add(dayOfWeek);
		}
		
}
