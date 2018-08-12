package Helpers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public class DateHelper {

	public DateHelper() {
		super();
	}

	public static LocalDate convertDateToLocalDate(Date date){
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}
	
	public static Date convertLocalDateToDate(LocalDate date) {
		return Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	public static LocalDate getNextDateOfWeekDay(LocalDate localDate, String dayOfWeek){
			return localDate.with(TemporalAdjusters.next(DayOfWeek.valueOf(dayOfWeek)));
	}
	
}
