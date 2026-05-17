package com.example.maps4u;

public class FavoriteLocation {
    private String id;
    private String customName;
    private String address;
    private double latitude;
    private double longitude;

    public FavoriteLocation(String id, String customName, String address, double latitude, double longitude) {
        this.id = id;
        this.customName = customName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getCustomName() { return customName; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}