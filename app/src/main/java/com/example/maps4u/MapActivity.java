package com.example.maps4u;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng origin;
    private LatLng destination;

    // UI Elements
    private TextView distanceText, durationText;
    private TextView fuelConsumptionText, co2EmissionsText, co2RatingText;
    private View progressIndicator;
    private Button selectButton;

    // Stats Views
    private TextView walkingDurationText, walkingDistanceText, caloriesBurnedText, healthBenefitsText;
    private TextView bicycleDurationText, bicycleDistanceText, bicycleCaloriesText, bicycleHealthText;
    private LinearLayout carStatsView, walkingStatsView, bicycleStatsView;
    private TabLayout tabLayout;

    // Data
    private Car selectedCar;
    private double distanceInKm;
    private String duration;
    private String selectedTransportMode = null;
    private String mapsApiKey;

    private FirebaseHelper firebaseHelper;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 1. Inițializare Firebase și API Key
        mapsApiKey = getString(R.string.google_maps_api_key);
        firebaseHelper = new FirebaseHelper();
        userId = firebaseHelper.getCurrentUserId();

        if (userId == null) {
            Toast.makeText(this, "User not logged in. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Inițializare Views
        initViews();
        setupTabs();

        // 3. Încărcare date despre mașină (Asincron din Firebase)
        loadCarData();

        // 4. Obținere coordonate din Intent
        Intent intent = getIntent();
        origin = new LatLng(intent.getDoubleExtra("origin_lat", 0), intent.getDoubleExtra("origin_lng", 0));
        destination = new LatLng(intent.getDoubleExtra("dest_lat", 0), intent.getDoubleExtra("dest_lng", 0));

        // 5. Setup Hartă
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 6. Setup Butoane
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            // Ne întoarcem la HomeActivity păstrând continuitatea (opțional putem trimite extra-uri înapoi)
            finish();
        });

        selectButton.setOnClickListener(v -> saveRouteSelection());

        // Default view
        showCarStats();
    }

    private void initViews() {
        distanceText = findViewById(R.id.distanceText);
        durationText = findViewById(R.id.durationText);
        fuelConsumptionText = findViewById(R.id.fuelConsumptionText);
        co2EmissionsText = findViewById(R.id.co2EmissionsText);
        co2RatingText = findViewById(R.id.co2RatingText);
        progressIndicator = findViewById(R.id.progressIndicator);
        selectButton = findViewById(R.id.selectButton);
        tabLayout = findViewById(R.id.tabLayout);

        // Walking Stats Elements
        walkingDurationText = findViewById(R.id.walkingDurationText);
        walkingDistanceText = findViewById(R.id.walkingDistanceText);
        caloriesBurnedText = findViewById(R.id.caloriesBurnedText);
        healthBenefitsText = findViewById(R.id.healthBenefitsText);

        // Bicycle Stats Elements
        bicycleDurationText = findViewById(R.id.bicycleDurationText);
        bicycleDistanceText = findViewById(R.id.bicycleDistanceText);
        bicycleCaloriesText = findViewById(R.id.bicycleCaloriesText);
        bicycleHealthText = findViewById(R.id.bicycleHealthText);

        // Layout Containers
        carStatsView = findViewById(R.id.carStatsView);
        walkingStatsView = findViewById(R.id.walkingStatsView);
        bicycleStatsView = findViewById(R.id.bicycleStatsView);
    }

    // --- FIREBASE: Încărcare Mașină ---
    private void loadCarData() {
        firebaseHelper.getUser(userId, new FirebaseHelper.FirestoreCallback<User>() {
            @Override
            public void onCallback(User user) {
                if (user != null && user.getCarData() != null && !user.getCarData().isEmpty()) {
                    try {
                        Gson gson = new Gson();
                        selectedCar = gson.fromJson(user.getCarData(), Car.class);
                        // Recalculăm detaliile doar când avem și mașina și distanța
                        if (distanceInKm > 0) {
                            updateCarDetails();
                        }
                    } catch (Exception e) {
                        Log.e("MapActivity", "Error parsing car data", e);
                    }
                }
            }
        });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (selectedTransportMode != null) {
                    // Dacă userul a ales deja un mod, forțăm tab-ul să rămână acolo
                    tabLayout.selectTab(tabLayout.getTabAt(getTransportModeTabIndex(selectedTransportMode)));
                    return;
                }

                switch (tab.getPosition()) {
                    case 0: // Car
                        showCarStats();
                        break;
                    case 1: // Walking
                        showWalkingStats();
                        break;
                    case 2: // Bicycle
                        showBicycleStats();
                        break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    // --- LOGICA DE SALVARE (FIREBASE) ---
    private void saveRouteSelection() {
        int selectedTab = tabLayout.getSelectedTabPosition();
        switch (selectedTab) {
            case 0: selectedTransportMode = "car"; break;
            case 1: selectedTransportMode = "walking"; break;
            case 2: selectedTransportMode = "bicycle"; break;
            default: selectedTransportMode = "car";
        }

        String originAddress = getIntent().getStringExtra("origin_address");
        String destinationAddress = getIntent().getStringExtra("destination_address");

        if (originAddress != null && destinationAddress != null) {
            // Salvăm în Firestore
            firebaseHelper.addRouteToHistory(userId, originAddress, destinationAddress, selectedTransportMode);

            // Afișăm confirmarea vizuală (Optimistic UI)
            Toast.makeText(this, "Route saved to history", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Could not save route: Address missing", Toast.LENGTH_SHORT).show();
        }

        // Blocăm interfața după selecție
        selectButton.setVisibility(View.GONE);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            if (tabLayout.getTabAt(i) != null) {
                tabLayout.getTabAt(i).view.setEnabled(false);
            }
        }
    }

    private int getTransportModeTabIndex(String transportMode) {
        if (transportMode == null) return 0;
        switch (transportMode) {
            case "car": return 0;
            case "walking": return 1;
            case "bicycle": return 2;
            default: return 0;
        }
    }

    // --- VISIBILITY HELPERS ---
    private void showCarStats() {
        carStatsView.setVisibility(View.VISIBLE);
        walkingStatsView.setVisibility(View.GONE);
        bicycleStatsView.setVisibility(View.GONE);
        updateCarDetails();
    }

    private void showWalkingStats() {
        carStatsView.setVisibility(View.GONE);
        walkingStatsView.setVisibility(View.VISIBLE);
        bicycleStatsView.setVisibility(View.GONE);
        updateWalkingStats();
    }

    private void showBicycleStats() {
        carStatsView.setVisibility(View.GONE);
        walkingStatsView.setVisibility(View.GONE);
        bicycleStatsView.setVisibility(View.VISIBLE);
        updateBicycleStats();
    }

    // --- UPDATE UI WITH DATA ---
    private void updateCarDetails() {
        if (selectedCar != null && distanceInKm > 0) {
            double fuelConsumptionTotal = (selectedCar.getFuelConsumption() * distanceInKm) / 100;
            double co2EmissionsTotal = (selectedCar.getCo2Emissions() * distanceInKm);

            fuelConsumptionText.setText(getString(R.string.fuel_consumption, String.format("%.2f", fuelConsumptionTotal)));
            co2EmissionsText.setText(getString(R.string.co2_emissions, String.format("%.2f", co2EmissionsTotal)));

            updateEcoRatingIndicator(co2EmissionsTotal);
        } else {
            fuelConsumptionText.setText(R.string.fuel_consumption_na);
            co2EmissionsText.setText(R.string.co2_emissions_na);
        }
    }

    private void updateWalkingStats() {
        double walkingSpeedKmH = 5.0;
        int walkingMinutes = (int) ((distanceInKm / walkingSpeedKmH) * 60);
        double caloriesBurned = distanceInKm * 65; // Approx calories/km

        walkingDistanceText.setText(String.format(getString(R.string.walking_distance), distanceInKm));
        walkingDurationText.setText(getString(R.string.walking_duration, walkingMinutes));
        caloriesBurnedText.setText(String.format(getString(R.string.calories_burned), caloriesBurned));

        if (distanceInKm < 1) healthBenefitsText.setText(R.string.health_benefits_short);
        else if (distanceInKm < 3) healthBenefitsText.setText(R.string.health_benefits_moderate);
        else healthBenefitsText.setText(R.string.health_benefits_long);
    }

    private void updateBicycleStats() {
        double cyclingSpeedKmH = 15.0;
        int cyclingMinutes = (int) ((distanceInKm / cyclingSpeedKmH) * 60);
        double caloriesBurned = distanceInKm * 35; // Approx calories/km

        bicycleDistanceText.setText(String.format(getString(R.string.bicycle_distance), distanceInKm));
        bicycleDurationText.setText(getString(R.string.bicycle_duration, cyclingMinutes));
        bicycleCaloriesText.setText(String.format(getString(R.string.bicycle_calories), caloriesBurned));

        if (distanceInKm < 5) bicycleHealthText.setText(R.string.bicycle_health_short);
        else if (distanceInKm < 15) bicycleHealthText.setText(R.string.bicycle_health_moderate);
        else bicycleHealthText.setText(R.string.bicycle_health_long);
    }

    private void updateEcoRatingIndicator(double co2EmissionsTotal) {
        if (distanceInKm == 0) return;

        double co2PerKm = co2EmissionsTotal / distanceInKm;
        String rating;
        int color;
        float progressPosition;

        if (co2PerKm < 100) {
            rating = getString(R.string.eco_rating_excellent); color = Color.GREEN; progressPosition = 0.1f;
        } else if (co2PerKm < 120) {
            rating = getString(R.string.eco_rating_good); color = Color.parseColor("#4CAF50"); progressPosition = 0.3f;
        } else if (co2PerKm < 150) {
            rating = getString(R.string.eco_rating_average); color = Color.parseColor("#FFC107"); progressPosition = 0.6f;
        } else {
            rating = getString(R.string.eco_rating_poor); color = Color.RED; progressPosition = 0.9f;
        }

        co2RatingText.setText(getString(R.string.eco_rating, rating));
        co2RatingText.setTextColor(color);

        // Update visual indicator position
        progressIndicator.post(() -> {
            View parent = (View) progressIndicator.getParent();
            int parentWidth = parent.getWidth();
            int newPosition = (int) (parentWidth * progressPosition);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) progressIndicator.getLayoutParams();
            // Ensure we don't go out of bounds
            int indicatorWidth = progressIndicator.getWidth();
            if (newPosition < 0) newPosition = 0;
            if (newPosition + indicatorWidth > parentWidth) newPosition = parentWidth - indicatorWidth;

            params.leftMargin = newPosition;
            progressIndicator.setLayoutParams(params);
            progressIndicator.setBackgroundColor(color);
        });
    }

    // --- GOOGLE MAPS ---
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Validare coordonate
        if ((origin.latitude == 0 && origin.longitude == 0) || (destination.latitude == 0 && destination.longitude == 0)) {
            Toast.makeText(this, "Invalid coordinates provided.", Toast.LENGTH_SHORT).show();
            return;
        }

        mMap.addMarker(new MarkerOptions().position(origin).title("Starting Point"));
        mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 10));

        getDirectionData();
    }

    private void getDirectionData() {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude + "&key="+ mapsApiKey ;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MapActivity.this, "Network error: Could not fetch route.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray routes = jsonObject.getJSONArray("routes");

                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);

                            // 1. Draw Polyline
                            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                            String encodedPolyline = overviewPolyline.getString("points");

                            // 2. Extract Distance & Duration
                            JSONObject legs = route.getJSONArray("legs").getJSONObject(0);
                            final String distanceStr = legs.getJSONObject("distance").getString("text");
                            duration = legs.getJSONObject("duration").getString("text");
                            distanceInKm = legs.getJSONObject("distance").getDouble("value") / 1000.0;

                            runOnUiThread(() -> {
                                // Update UI
                                distanceText.setText(getString(R.string.distance) + ": " + distanceStr);
                                durationText.setText(getString(R.string.duration) + ": " + duration);

                                // Recalculate Stats now that we have distance
                                updateCarDetails();
                                updateWalkingStats();
                                updateBicycleStats();

                                // Draw Route
                                List<LatLng> decodedPath = decodePolyline(encodedPolyline);
                                if (decodedPath != null && !decodedPath.isEmpty()) {
                                    mMap.addPolyline(new PolylineOptions().addAll(decodedPath).width(10).color(Color.BLUE));
                                }
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(MapActivity.this, "No route found by Google.", Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            polyline.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }
        return polyline;
    }
}