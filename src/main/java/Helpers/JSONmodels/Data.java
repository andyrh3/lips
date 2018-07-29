
package Helpers.JSONmodels;

import com.fasterxml.jackson.annotation.*;
import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
    "countries",
    "checksum",
    "timestamp",
    "info"
})
public class Data {

    @JsonProperty("countries")
    private List<Country> countries = new ArrayList();

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("info")
    private String info;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap();

    @JsonProperty("countries")
    public List<Country> getCountries() {
        return countries;
    }

    @JsonProperty("countries")
    public void setCountries(List<Country> countries) {
        this.countries = countries;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    @JsonProperty("timestamp")
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty("info")
    public String getInfo() {
        return info;
    }

    @JsonProperty("info")
    public void setInfo(String info) {
        this.info = info;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
