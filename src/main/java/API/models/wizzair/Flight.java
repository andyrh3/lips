package API.models.wizzair;

import Helpers.DateHelper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;
import net.sf.cglib.core.Local;

import javax.annotation.Generated;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "departureDate",
        "departureStation",
        "arrivalStation",
        "price",
        "priceType",
        "departureDates",
        "classOfService",
        "hasMacFlight",
})

public class Flight implements Serializable {

    @JsonProperty("departureDate")
    @Expose Date departureDate;

    @JsonProperty("departureStation")
    @Expose String departureStation;

    @JsonProperty("arrivalStation")
    @Expose String arrivalStation;

    @JsonProperty("price")
    @Expose Price price;

    @JsonProperty("priceType")
    @Expose String priceType;

    @JsonProperty("departureDates")
    @Expose List<Date> departureDates;

    @JsonProperty("classOfService")
    @Expose String classOfService;

    @JsonProperty("hasMacFlight")
    @Expose boolean hasMacFlight;

    public LocalDate getDepartureDateAsLocalDate() {
        return DateHelper.convertDateToLocalDate(departureDate);
    }
}