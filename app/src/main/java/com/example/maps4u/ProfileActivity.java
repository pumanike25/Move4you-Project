package com.example.maps4u;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private LinearLayout userDataView, walkingStatsView, publicProfileView;
    private TextView tvHeight, tvWeight, tvAge, tvGender, tvTotalSteps, tvDistanceWalked, tvCaloriesBurned, tvHealthBenefits, tvMoneyGoal;
    private Button btnEditBiometrics, btnSavePublicSettings;
    private SwitchCompat switchPrivacy;
    private LinearLayout trophiesCheckboxContainer;

    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private BiometricData biometricData;
    private int dailyStepGoal = 10000;

    private List<CheckBox> trophyCheckBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();

        if (currentUserId == null) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupTabLayout();

        loadUserData();
        loadBiometricData();

        loadPublicProfileSettings();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        userDataView = findViewById(R.id.userDataView);
        walkingStatsView = findViewById(R.id.walkingStatsView);
        publicProfileView = findViewById(R.id.publicProfileView); // Noul tab

        tvHeight = findViewById(R.id.tvHeight);
        tvWeight = findViewById(R.id.tvWeight);
        tvAge = findViewById(R.id.tvAge);
        tvGender = findViewById(R.id.tvGender);

        tvTotalSteps = findViewById(R.id.tvTotalSteps);
        tvDistanceWalked = findViewById(R.id.tvDistanceWalked);
        tvCaloriesBurned = findViewById(R.id.tvCaloriesBurned);
        tvHealthBenefits = findViewById(R.id.tvHealthBenefits);
        tvMoneyGoal = findViewById(R.id.tvMoneyGoal);

        btnEditBiometrics = findViewById(R.id.btnEditBiometrics);

        switchPrivacy = findViewById(R.id.switchPrivacy);
        trophiesCheckboxContainer = findViewById(R.id.trophiesCheckboxContainer);
        btnSavePublicSettings = findViewById(R.id.btnSavePublicSettings);

        btnSavePublicSettings.setOnClickListener(v -> savePublicProfileSettings());
    }

    private void loadUserData() {
        firebaseHelper.getUser(currentUserId, user -> {
            if (user != null) {
                ((TextView) findViewById(R.id.tvUsername)).setText(user.getUsername());
                ((TextView) findViewById(R.id.tvEmail)).setText(user.getEmail());

                if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                    try {
                        Glide.with(ProfileActivity.this)
                                .load(user.getImageUrl())
                                .circleCrop()
                                .into((ImageView) findViewById(R.id.profileImage));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }

    private void setupTabLayout() {
        userDataView.setVisibility(View.VISIBLE);
        walkingStatsView.setVisibility(View.GONE);
        publicProfileView.setVisibility(View.GONE);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        userDataView.setVisibility(View.VISIBLE);
                        walkingStatsView.setVisibility(View.GONE);
                        publicProfileView.setVisibility(View.GONE);
                        break;
                    case 1:
                        userDataView.setVisibility(View.GONE);
                        walkingStatsView.setVisibility(View.VISIBLE);
                        publicProfileView.setVisibility(View.GONE);
                        loadWalkingStats();
                        break;
                    case 2:
                        userDataView.setVisibility(View.GONE);
                        walkingStatsView.setVisibility(View.GONE);
                        publicProfileView.setVisibility(View.VISIBLE);
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // --- customize profile ---

    private void loadPublicProfileSettings() {

        firebaseHelper.getUserHistory(currentUserId, snapshots -> {
            int routesCount = (snapshots != null) ? snapshots.size() : 0;

            firebaseHelper.getAllDailySteps(currentUserId, stepsList -> {
                long totalSteps = 0;
                for (Map<String, Object> day : stepsList) {
                    Long steps = (Long) day.get("steps");
                    if (steps != null) totalSteps += steps;
                }

                List<String> unlockedTrophies = new ArrayList<>();
                if (routesCount > 0) unlockedTrophies.add("First Journey");
                if (routesCount >= 5) unlockedTrophies.add("Explorer");
                if (routesCount >= 20) unlockedTrophies.add("Frequent Traveler");
                if (totalSteps >= 10000) unlockedTrophies.add("10K Club");
                if (totalSteps >= 50000) unlockedTrophies.add("Marathoner");

                // extract previous settings from firebase
                fetchUserPreferencesAndBuildUI(unlockedTrophies);
            });
        });
    }

    private void fetchUserPreferencesAndBuildUI(List<String> unlockedTrophies) {
        FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    boolean isPrivate = false;
                    List<String> selectedTrophies = new ArrayList<>();

                    if (doc.exists()) {
                        if (doc.contains("isPrivate")) isPrivate = doc.getBoolean("isPrivate");
                        if (doc.contains("selectedTrophies")) selectedTrophies = (List<String>) doc.get("selectedTrophies");
                    }

                    switchPrivacy.setChecked(isPrivate);
                    buildTrophiesCheckboxes(unlockedTrophies, selectedTrophies);
                });
    }

    private void buildTrophiesCheckboxes(List<String> unlockedTrophies, List<String> selectedTrophies) {
        trophiesCheckboxContainer.removeAllViews();
        trophyCheckBoxes.clear();

        if (unlockedTrophies.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("You haven't unlocked any trophies yet.");
            emptyText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            trophiesCheckboxContainer.addView(emptyText);
            return;
        }

        for (String trophyName : unlockedTrophies) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(trophyName);
            checkBox.setTextSize(16f);

            if (selectedTrophies != null && selectedTrophies.contains(trophyName)) {
                checkBox.setChecked(true);
            }

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && countSelectedCheckboxes() > 3) {
                    checkBox.setChecked(false);
                    Toast.makeText(this, "You can select a maximum of 3 trophies!", Toast.LENGTH_SHORT).show();
                }
            });

            trophyCheckBoxes.add(checkBox);
            trophiesCheckboxContainer.addView(checkBox);
        }
    }

    private int countSelectedCheckboxes() {
        int count = 0;
        for (CheckBox cb : trophyCheckBoxes) {
            if (cb.isChecked()) count++;
        }
        return count;
    }

    private void savePublicProfileSettings() {
        List<String> finalSelectedTrophies = new ArrayList<>();
        for (CheckBox cb : trophyCheckBoxes) {
            if (cb.isChecked()) {
                finalSelectedTrophies.add(cb.getText().toString());
            }
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("isPrivate", switchPrivacy.isChecked());
        updates.put("selectedTrophies", finalSelectedTrophies);

        FirebaseFirestore.getInstance().collection("users").document(currentUserId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Public profile updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update.", Toast.LENGTH_SHORT).show());
    }

    // --- biometric data ---

    private void loadBiometricData() {
        firebaseHelper.getBiometricData(currentUserId, data -> {
            if (data != null) biometricData = data;
            else biometricData = new BiometricData(0, 0, 0, "", 0);

            dailyStepGoal = biometricData.getDailyStepGoal();
            if (dailyStepGoal == 0) dailyStepGoal = 10000;

            updateBiometricViews();
            btnEditBiometrics.setOnClickListener(v -> showBiometricDataDialog());
        });
    }

    private void updateBiometricViews() {
        tvHeight.setText(biometricData.getHeight() > 0 ? "Height: " + biometricData.getHeight() + " cm" : "Height: Not set");
        tvWeight.setText(biometricData.getWeight() > 0 ? "Weight: " + biometricData.getWeight() + " kg" : "Weight: Not set");
        tvAge.setText(biometricData.getAge() > 0 ? "Age: " + biometricData.getAge() : "Age: Not set");
        tvGender.setText(biometricData.getGender() != null && !biometricData.getGender().isEmpty() ? "Gender: " + biometricData.getGender() : "Gender: Not set");
        int displayGoal = (biometricData != null && biometricData.getMonthlyMoneyGoal() > 0) ? biometricData.getMonthlyMoneyGoal() : 50;
        tvMoneyGoal.setText("Monthly Goal: €" + displayGoal);
    }

    private void showBiometricDataDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Biometric Data");

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_biometric_data, null);
        builder.setView(dialogView);

        EditText etHeight = dialogView.findViewById(R.id.etHeight);
        EditText etWeight = dialogView.findViewById(R.id.etWeight);
        EditText etAge = dialogView.findViewById(R.id.etAge);
        EditText etStepGoal = dialogView.findViewById(R.id.etStepGoal);
        EditText etMoneyGoal = dialogView.findViewById(R.id.etMoneyGoal);
        RadioGroup rgGender = dialogView.findViewById(R.id.rgGender);

        if (biometricData.getHeight() > 0) etHeight.setText(String.valueOf(biometricData.getHeight()));
        if (biometricData.getWeight() > 0) etWeight.setText(String.valueOf(biometricData.getWeight()));
        if (biometricData.getAge() > 0) etAge.setText(String.valueOf(biometricData.getAge()));
        etStepGoal.setText(String.valueOf(dailyStepGoal));

        int currentMoneyGoal = biometricData.getMonthlyMoneyGoal() > 0 ? biometricData.getMonthlyMoneyGoal() : 50;
        etMoneyGoal.setText(String.valueOf(currentMoneyGoal));

        if (biometricData.getGender() != null) {
            if (biometricData.getGender().equalsIgnoreCase("Male")) rgGender.check(R.id.rbMale);
            else if (biometricData.getGender().equalsIgnoreCase("Female")) rgGender.check(R.id.rbFemale);
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float height = Float.parseFloat(etHeight.getText().toString());
                float weight = Float.parseFloat(etWeight.getText().toString());
                int age = Integer.parseInt(etAge.getText().toString());
                dailyStepGoal = Integer.parseInt(etStepGoal.getText().toString());
                int moneyGoal = Integer.parseInt(etMoneyGoal.getText().toString());

                String gender = (rgGender.getCheckedRadioButtonId() == R.id.rbMale) ? "Male" : "Female";

                biometricData.setHeight(height);
                biometricData.setWeight(weight);
                biometricData.setAge(age);
                biometricData.setGender(gender);
                biometricData.setDailyStepGoal(dailyStepGoal);
                biometricData.setMonthlyMoneyGoal(moneyGoal);

                firebaseHelper.saveBiometricData(currentUserId, biometricData);
                Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT).show();
                updateBiometricViews();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void loadWalkingStats() {
        if (biometricData == null || biometricData.getHeight() == 0 || biometricData.getWeight() == 0) {
            tvTotalSteps.setText("Please set biometrics first");
            return;
        }

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        firebaseHelper.getDailySteps(currentUserId, currentDate, totalSteps -> {
            float stepLength = biometricData.getHeight() * 0.45f;
            float distanceWalked = (totalSteps * stepLength) / 100000f; // in km
            float caloriesBurned = totalSteps * (biometricData.getWeight() * 0.0005f);

            String healthBenefits;
            if (totalSteps >= dailyStepGoal) healthBenefits = "Excellent! You've met your daily goal.";
            else if (totalSteps >= dailyStepGoal * 0.75) healthBenefits = "Good job! Almost at your daily goal.";
            else if (totalSteps >= dailyStepGoal * 0.5) healthBenefits = "Keep going! You're halfway to your goal.";
            else healthBenefits = "You can do better! Try to walk more today.";

            tvTotalSteps.setText(totalSteps + " Steps");
            tvDistanceWalked.setText(String.format("%.2f km", distanceWalked));
            tvCaloriesBurned.setText(String.format("%.0f kcal", caloriesBurned));
            tvHealthBenefits.setText(healthBenefits);
        });
    }
}