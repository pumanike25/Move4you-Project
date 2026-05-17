package com.example.maps4u;

public class DiscoverPlace {
    private String id;
    private String name;
    private String address;
    private String photoUrl;
    private double rating;
    private double lat;
    private double lng;
    private float distanceInMeters;

    public DiscoverPlace(String id, String name, String address, String photoUrl, double rating, double lat, double lng, float distanceInMeters) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.photoUrl = photoUrl;
        this.rating = rating;
        this.lat = lat;
        this.lng = lng;
        this.distanceInMeters = distanceInMeters;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhotoUrl() { return photoUrl; }
    public double getRating() { return rating; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public float getDistanceInMeters() { return distanceInMeters; }
}