package API.models.blueair;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;
import javax.annotation.Generated;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "name",
        "start",
        "end"
})

public class ScheduleDate {

    @JsonProperty("Name")
    @Expose String name;

    @JsonProperty("Start")
    @Expose LocalDate start;

    @JsonProperty("End")
    @Expose LocalDate end;

}
