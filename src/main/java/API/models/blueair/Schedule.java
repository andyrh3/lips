package API.models.blueair;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "key",
        "value"
})

public class Schedule {

    @JsonProperty("SchedulesDate")
    @Expose ScheduleDate key;

    @JsonProperty("SchedulesTimes")
    @Expose List<ScheduleTime> value = new ArrayList<>();
}
