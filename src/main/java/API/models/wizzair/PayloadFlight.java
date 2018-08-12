package API.models.wizzair;

public class PayloadFlight {

    String departureStation;
    String arrivalStation;
    String from;
    String to;

    public PayloadFlight(String departureStation, String arrivalStation, String from, String to) {
        this.departureStation = departureStation;
        this.arrivalStation = arrivalStation;
        this.from = from;
        this.to = to;
    }
}
