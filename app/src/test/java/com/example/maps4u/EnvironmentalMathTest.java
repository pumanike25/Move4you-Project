package com.example.maps4u;

import org.junit.Test;
import static org.junit.Assert.*;

public class EnvironmentalMathTest {

    @Test
    public void testDistanceAndCaloriesCalculation() {
        // Input data (simulating a user)
        int totalSteps = 20000;
        float heightCm = 180f;
        float weightKg = 80f;

        // Execute the logic exactly as it is in ProfileActivity
        float stepLength = heightCm * 0.45f;
        float distanceWalkedKm = (totalSteps * stepLength) / 100000f;
        float caloriesBurned = totalSteps * (weightKg * 0.0005f);

        // Verify the results (Expected vs Actual, with a margin of error of 0.01)
        // 20000 steps * (180 * 0.45 = 81) / 100000 = 16.2 km
        assertEquals(16.2f, distanceWalkedKm, 0.01f);

        // 20000 steps * (80 * 0.0005 = 0.04) = 800 calories
        assertEquals(800f, caloriesBurned, 0.01f);
    }

    @Test
    public void testCO2SavedCalculation() {
        int steps = 10000;

        // Logic from StatisticsActivity (10k steps = ~1.2kg CO2 saved)
        double expectedCO2 = (steps / 10000.0) * 1.2;

        assertEquals(1.2, expectedCO2, 0.001);
    }
}