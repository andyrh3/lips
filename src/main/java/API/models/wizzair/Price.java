package API.models.wizzair;

import API.models.blueair.ScheduleDate;
import API.models.blueair.ScheduleTime;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.gson.annotations.Expose;

import javax.annotation.Generated;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "amount",
        "currencyCode"
})

public class Price implements Serializable {

    @JsonProperty("amount")
    @Expose BigDecimal amount;

    @JsonProperty("currencyCode")
    @Expose String currencyCode;
}
