package API.models.blueair;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;
import javax.annotation.Generated;
import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "arrivalHour",
        "daysOfWeek",
        "numberOfStops",
        "startHour"
})

public class ScheduleTime implements Serializable {

    @JsonProperty("startHour")
    @Expose String startHour;

    @JsonProperty("arrivalHour")
    @Expose String arrivalHour;

    @JsonProperty("daysOfWeek")
    @Expose String daysOfWeek;

    @JsonProperty("numberOfStops")
    @Expose int numberOfStops;

}
