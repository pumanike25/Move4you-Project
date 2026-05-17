package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

public class CreateCarProfileActivity extends AppCompatActivity {

    private EditText carNameEditText;
    private EditText carYearEditText;
    private EditText fuelConsumptionEditText;
    private Spinner fuelTypeSpinner;
    private Spinner co2CategorySpinner;
    private Button saveButton;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_car_profile);

        firebaseHelper = new FirebaseHelper();
        String userId = firebaseHelper.getCurrentUserId();

        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        carNameEditText = findViewById(R.id.carNameEditText);
        carYearEditText = findViewById(R.id.carYearEditText);
        fuelConsumptionEditText = findViewById(R.id.fuelConsumptionEditText);
        fuelTypeSpinner = findViewById(R.id.fuelTypeSpinner);
        co2CategorySpinner = findViewById(R.id.co2CategorySpinner);
        saveButton = findViewById(R.id.saveButton);

        ArrayAdapter<CharSequence> fuelAdapter = ArrayAdapter.createFromResource(this,
                R.array.fuel_types, android.R.layout.simple_spinner_item);
        fuelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fuelTypeSpinner.setAdapter(fuelAdapter);

        ArrayAdapter<CharSequence> co2Adapter = ArrayAdapter.createFromResource(this,
                R.array.co2_categories, android.R.layout.simple_spinner_item);
        co2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        co2CategorySpinner.setAdapter(co2Adapter);

        saveButton.setOnClickListener(v -> saveCarProfile(userId));
    }

    private void saveCarProfile(String userId) {
        String carName = carNameEditText.getText().toString().trim();
        String carYear = carYearEditText.getText().toString().trim();
        String fuelConsumptionStr = fuelConsumptionEditText.getText().toString().trim();
        String fuelType = fuelTypeSpinner.getSelectedItem().toString();

        if (carName.isEmpty() || carYear.isEmpty() || fuelConsumptionStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double fuelConsumption = Double.parseDouble(fuelConsumptionStr);
            double co2Emissions = calculateCo2Emissions(fuelConsumption, fuelType);

            Car customCar = new Car("Custom", carName, carYear,
                    fuelConsumption, co2Emissions, fuelType);

            // 1. Salvăm în colecția de profile (listă)
            firebaseHelper.saveCustomCarProfile(userId, customCar, new FirebaseHelper.FirestoreCallback<Void>() {
                @Override
                public void onCallback(Void data) {
                    // 2. Salvăm și ca mașină curentă (JSON în user)
                    firebaseHelper.saveCarData(userId, new Gson().toJson(customCar));

                    Toast.makeText(CreateCarProfileActivity.this, "Car profile saved successfully", Toast.LENGTH_SHORT).show();
                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(CreateCarProfileActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid fuel consumption", Toast.LENGTH_SHORT).show();
        }
    }

    private double calculateCo2Emissions(double fuelConsumption, String fuelType) {
        switch (fuelType) {
            case "Gasoline": return fuelConsumption * 23.2;
            case "Diesel": return fuelConsumption * 26.5;
            case "LPG": return fuelConsumption * 16.8;
            case "Hybrid": return fuelConsumption * 15.0;
            case "Electric": return 0.0;
            default: return fuelConsumption * 20.0;
        }
    }
}