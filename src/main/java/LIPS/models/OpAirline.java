package LIPS.models;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import API.models.Airport;
import com.google.gson.annotations.Expose;

public class OpAirline implements Serializable{

	private static final long serialVersionUID = 1L;
	
	@Expose private String name;
	@Expose private String code;
	@Expose private String url;
	@Expose private LinkedList<OpRange> opRanges = new LinkedList<>();
	private static DateTimeFormatter dateFormatter;

	@Expose private LinkedHashMap<String, Airport> airportMapping = new LinkedHashMap<>();
	
	public OpAirline(String name, String code, String url) {
		super();
		this.name = name;
		this.code = code;
		this.url = url;
	}

	public URI buildApiQueryURL(ArrayList<NameValuePair> params){
		URIBuilder builder;
		try {
			builder = new URIBuilder(this.url).setParameters(params);
			return builder.build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}	
	
	public LinkedList<OpRange> getOpRanges() {
		return opRanges;
	}

	public void setOpRanges(LinkedList<OpRange> opRanges) {
		this.opRanges = opRanges;
	}

	public void addOpRange(OpRange opRange) {
		this.opRanges.add(opRange);
	}

	public LinkedHashMap<String, Airport> getAirportMapping() {
		return airportMapping;
	}

	public void setAirportMapping(LinkedHashMap<String, Airport> airportMapping) {
		this.airportMapping = airportMapping;
	}
	
	public void addAirportMapping(String code, Airport airport){
		this.airportMapping.put(code, airport);
	}
	
	public void setDateFormatter(String dateFormat){
		this.dateFormatter = DateTimeFormatter.ofPattern(dateFormat);
	}

	public DateTimeFormatter getDateFormatter(){
		return this.dateFormatter;
	}

}
