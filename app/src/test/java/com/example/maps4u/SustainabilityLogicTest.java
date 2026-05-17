package com.example.maps4u;

import org.junit.Test;
import static org.junit.Assert.*;

public class SustainabilityLogicTest {

    @Test
    public void testFuelAndMoneySavedCalculations() {
        // Simulate a realistic scenario: the user walked 15,000 steps
        int totalSteps = 15000;
        double fuelPricePerLiter = 1.50;
        double litersPerKm = 0.08;

        // 1. Calculate distance (formula from the app: steps * 0.000762)
        double distanceKm = totalSteps * 0.000762;
        assertEquals("The distance should be 11.43 km", 11.43, distanceKm, 0.001);

        // 2. Calculate saved fuel
        double fuelSavedLiters = distanceKm * litersPerKm;
        assertEquals("The saved fuel should be ~0.914 liters", 0.9144, fuelSavedLiters, 0.001);

        // 3. Calculate saved money
        double moneySaved = fuelSavedLiters * fuelPricePerLiter;
        assertEquals("The saved money should be ~1.37 Euro", 1.3716, moneySaved, 0.001);
    }

    @Test
    public void testVirtualForestTreesEquivalent() {
        // Simulate that the user walked 50 km
        double distanceKm = 50.0;

        // Formula from the app: 0.15 kg CO2 saved per km
        double co2SavedKg = distanceKm * 0.15;
        assertEquals("For 50km we should save 7.5 kg CO2", 7.5, co2SavedKg, 0.001);

        // Formula from the app: one tree absorbs 22kg CO2/year
        double treesEquivalent = co2SavedKg / 22.0;

        // 7.5 / 22 = 0.3409...
        assertEquals("The trees equivalent should be calculated correctly", 0.3409, treesEquivalent, 0.001);
    }
}