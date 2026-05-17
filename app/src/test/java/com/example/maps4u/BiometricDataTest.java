package com.example.maps4u;

import org.junit.Test;
import static org.junit.Assert.*;

public class BiometricDataTest {

    @Test
    public void testBiometricDataConstructorAndGetters() {
        BiometricData data = new BiometricData();
        data.setHeight(175.5f);
        data.setWeight(70.2f);
        data.setAge(25);
        data.setGender("Male");
        data.setDailyStepGoal(12000);

        assertEquals(175.5f, data.getHeight(), 0.01f);
        assertEquals(70.2f, data.getWeight(), 0.01f);
        assertEquals(25, data.getAge());
        assertEquals("Male", data.getGender());
        assertEquals(12000, data.getDailyStepGoal());
    }

    @Test
    public void testFallbackForEmptyOrZeroData() {
        BiometricData emptyData = new BiometricData();
        emptyData.setHeight(0);
        emptyData.setGender("");

        assertEquals(0f, emptyData.getHeight(), 0.0f);
        assertEquals("", emptyData.getGender());

        emptyData.setMonthlyMoneyGoal(100);
        assertEquals("The object must be able to update the money goal", 100, emptyData.getMonthlyMoneyGoal());
    }
}