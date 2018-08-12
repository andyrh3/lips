package API.models.blueair;

import java.io.Serializable;
import java.util.ArrayList;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "Schedules"
})

public class basicSchedule implements Serializable {

    @JsonProperty("Schedules")
    @Expose public ArrayList<Schedule> Schedules = new ArrayList<>();
}
