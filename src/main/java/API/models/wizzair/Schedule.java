package API.models.wizzair;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;
import jdk.nashorn.internal.ir.annotations.Ignore;

import javax.annotation.Generated;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "outboundFlights",
        "returnFlights"
})

public class Schedule implements Serializable {

    @JsonProperty("returnFlights")
    @Expose List<Flight> returnFlights = new ArrayList<>();

    @JsonProperty("outboundFlights")
    @Expose List<Flight> outboundFlights = new ArrayList<>();

    @JsonProperty("dates")
    @Ignore List<Date> dates = new ArrayList<>();

    public List<Flight> getOutboundFlights() {
        return outboundFlights;
    }
}
