package API.models.wizzair;

import java.util.ArrayList;
import java.util.List;

public class Payload {

    String priceType;
    int adultCount;
    int childCount;
    int infantCount;
    List<PayloadFlight> flightList = new ArrayList<>();

    public  Payload() {
        this.priceType = "regular";
        this.adultCount = 1;
        this.childCount = 0;
        this.infantCount = 0;
    }

    public void setFlightList(List<PayloadFlight> flightList) {
        this.flightList = flightList;
    }

    public void addFlight(PayloadFlight payloadFlight){
        flightList.add(payloadFlight);
    }

}
