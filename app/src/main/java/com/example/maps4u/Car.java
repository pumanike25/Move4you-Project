package com.example.maps4u;

public class Car {
    private String make;
    private String model;
    private String year;
    private double fuelConsumption;
    private double co2Emissions;
    private String fuelType;

    public Car() {
    }

    public Car(String make, String model, String year, double fuelConsumption, double co2Emissions, String fuelType) {
        this.make = make;
        this.model = model;
        this.year = year;
        this.fuelConsumption = fuelConsumption;
        this.co2Emissions = co2Emissions;
        this.fuelType = fuelType;
    }

    public String getFuelType() {
        return fuelType;
    }
    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }
    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getYear() {
        return year;
    }

    public double getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(double fuelConsumption) {
        this.fuelConsumption = fuelConsumption;
    }

    public double getCo2Emissions() {
        return co2Emissions;
    }

    public void setCo2Emissions(double co2Emissions) {
        this.co2Emissions = co2Emissions;
    }
}