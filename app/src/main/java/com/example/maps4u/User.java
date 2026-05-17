package com.example.maps4u;

public class User {
    private String username;
    private String email;
    private String role;
    private String imageUrl;
    private String carData;
    private String uid;

    public User() {}

    public User(String username, String email, String role, String imageUrl) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.imageUrl = imageUrl;
        this.carData = "";
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCarData() { return carData; }
    public void setCarData(String carData) { this.carData = carData; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
}