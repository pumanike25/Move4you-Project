package com.example.maps4u;

public class BiometricData {
    private float height;
    private float weight;
    private int age;
    private String gender;
    private int stepCount;
    private int dailyStepGoal;
    private int monthlyMoneyGoal = 50;

    public BiometricData() {
        this.dailyStepGoal = 10000;
        this.stepCount = 0;
    }

    public BiometricData(float height, float weight, int age, String gender, int stepCount) {
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.gender = gender;
        this.stepCount = stepCount;
        this.dailyStepGoal = 10000;
    }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = weight; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public int getStepCount() { return stepCount; }
    public void setStepCount(int stepCount) { this.stepCount = stepCount; }

    public int getDailyStepGoal() { return dailyStepGoal; }
    public void setDailyStepGoal(int dailyStepGoal) { this.dailyStepGoal = dailyStepGoal; }
    public int getMonthlyMoneyGoal() { return monthlyMoneyGoal; }
    public void setMonthlyMoneyGoal(int monthlyMoneyGoal) { this.monthlyMoneyGoal = monthlyMoneyGoal; }
}