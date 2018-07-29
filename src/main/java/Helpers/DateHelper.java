package Helpers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
	
}
