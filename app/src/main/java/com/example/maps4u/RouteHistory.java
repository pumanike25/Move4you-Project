package com.example.maps4u;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class RouteHistory {
    private String origin;
    private String destination;
    private String transportMode;
    @ServerTimestamp
    private Date timestamp;

    public RouteHistory() {}

    public RouteHistory(String origin, String destination, String transportMode) {
        this.origin = origin;
        this.destination = destination;
        this.transportMode = transportMode;
    }

    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getTransportMode() { return transportMode; }
    public Date getTimestamp() { return timestamp; }
}