package com.example.maps4u;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    // --- UI Components ---
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private ImageView menuIcon, btnGps;
    private EditText locInput, desiredInput;
    private LinearLayout destinationContainer;
    private Button btnGo;
    private TextView tvStepCount;

    // --- POPUP COMPONENTS ---
    private CardView locationInfoCard;
    private TextView selectedAddressText;
    private Button btnSaveFavorite, btnGoHere;

    // --- Map & Location ---
    private GoogleMap mMap;
    private java.util.List<com.google.android.gms.maps.model.Marker> meetupMarkers = new java.util.ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private Marker selectedPointMarker;
    private LatLng selectedLatLng;

    private Location currentGpsLocation;
    private boolean userChangedLocationText = false;

    // --- Sensors ---
    private static final int REQUEST_ACTIVITY_RECOGNITION_PERMISSION = 1001;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int previousStepCount = 0;
    private Handler stepHandler = new Handler();
    private int dailySteps = 0;
    private String currentDate = "";
    private Handler dailyStepHandler = new Handler();
    private Runnable dailyStepRunnable;

    // --- Firebase ---
    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private FirebaseAuth mAuth;
    private String currentUserRole = "user";
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_home);

        if (!Places.isInitialized()) {
            String apiKey = getString(R.string.google_maps_api_key);
            Places.initialize(getApplicationContext(), apiKey);
        }
        PlacesClient placesClient = Places.createClient(this);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();

        EncryptionHelper.generateKeysIfNeeded();

        String myPublicKey = EncryptionHelper.getPublicKeyBase64();
        if (myPublicKey != null) {
            firebaseHelper.savePublicKey(myPublicKey);
        }

        if (currentUserId == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initViews();
        setupNavigation();
        setupLocationAndMap();
        setupSteps();
        loadUserData();
        checkFriendRequestsBadge();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        menuIcon = findViewById(R.id.menuIcon);
        btnGps = findViewById(R.id.btn_gps);
        locInput = findViewById(R.id.locinput);
        desiredInput = findViewById(R.id.desiredinput);
        destinationContainer = findViewById(R.id.destinationContainer);
        btnGo = findViewById(R.id.btn_go);
        tvStepCount = findViewById(R.id.tvStepCount);

        locationInfoCard = findViewById(R.id.locationInfoCard);
        selectedAddressText = findViewById(R.id.selectedAddressText);
        btnSaveFavorite = findViewById(R.id.btn_save_favorite);
        btnGoHere = findViewById(R.id.btn_go_here);

        // watcher for input on location
        locInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (locInput.hasFocus()) {
                    userChangedLocationText = true;
                }

                if (s.length() > 0) destinationContainer.setVisibility(View.VISIBLE);
                else destinationContainer.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnGps.setOnClickListener(v -> getCurrentLocation());
        btnGo.setOnClickListener(v -> handleDesiredLocation());

        btnGoHere.setOnClickListener(v -> {
            if (selectedAddressText != null && !selectedAddressText.getText().toString().equals("Loading address...")) {
                desiredInput.setText(selectedAddressText.getText().toString());
                handleDesiredLocation();
                locationInfoCard.setVisibility(View.GONE);
            }
        });
        ExtendedFloatingActionButton btnDiscover = findViewById(R.id.btnDiscover);
        if (btnDiscover != null) {
            btnDiscover.setOnClickListener(v -> discoverNearbyPlaces());
        }

        btnSaveFavorite.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                String address = selectedAddressText.getText().toString();

                firebaseHelper.saveFavoriteLocation(currentUserId, address, address, selectedLatLng.latitude, selectedLatLng.longitude, new FirebaseHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onCallback(Void data) {
                        Toast.makeText(HomeActivity.this, "Saved to Favorites!", Toast.LENGTH_SHORT).show();
                        locationInfoCard.setVisibility(View.GONE);
                    }
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(HomeActivity.this, "Error saving favorite.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void setupNavigation() {
        if (menuIcon != null) menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.drawer_profile) {
                    startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                } else if (id == R.id.drawer_history) {
                    startActivity(new Intent(HomeActivity.this, HistoryActivity.class));
                } else if (id == R.id.drawer_car_engine) {
                    openEngineChoiceDialog();
                } else if (id == R.id.drawer_trophies) {
                    startActivity(new Intent(HomeActivity.this, TrophiesActivity.class));
                } else if (id == R.id.drawer_upload_image) {
                    openImageChooser();
                } else if (id == R.id.drawer_logout) {
                    mAuth.signOut();
                    startActivity(new Intent(HomeActivity.this, MainActivity.class));
                    finish();
                } else if (id == R.id.drawer_statistics) {
                    startActivity(new Intent(HomeActivity.this, StatisticsActivity.class));
                    }else if (id == R.id.drawer_favorites) {
                startActivity(new Intent(HomeActivity.this, FavoriteLocationsActivity.class));
                drawerLayout.closeDrawers();
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_transport);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_history) {
                    startActivity(new Intent(HomeActivity.this, HistoryActivity.class));
                    return true;
                } else if (id == R.id.nav_community) {
                    startActivity(new Intent(HomeActivity.this, CommunityActivity.class));
                    return true;
                }else if (id == R.id.nav_chat) {
                    startActivity(new Intent(HomeActivity.this, ChatListActivity.class));
                    return true;
                }
                return true;
            });
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderName = headerView.findViewById(R.id.nav_header_title);
        TextView navHeaderSub = headerView.findViewById(R.id.nav_header_subtitle);
        ImageView navHeaderImage = headerView.findViewById(R.id.nav_header_image);

        if (currentUser != null && currentUser.isAnonymous()) {
            currentUserRole = "guest";
            navHeaderName.setText("Welcome, Guest!");
            return;
        }

        firebaseHelper.getUser(currentUserId, new FirebaseHelper.FirestoreCallback<User>() {
            @Override
            public void onCallback(User user) {
                if (user != null) {
                    currentUserRole = user.getRole();
                    navHeaderName.setText("Welcome, " + user.getUsername() + "!");
                    navHeaderSub.setText(user.getEmail());
                    String imageUrl = user.getImageUrl();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        try { Glide.with(HomeActivity.this).load(imageUrl).circleCrop().into(navHeaderImage); }
                        catch (Exception e) {}
                    }
                }
            }
        });
    }

    // route logic
    private void handleDesiredLocation() {
        String current = locInput.getText().toString();
        String dest = desiredInput.getText().toString();
        if (current.isEmpty() || dest.isEmpty()) {
            Toast.makeText(this, "Please enter start and destination", Toast.LENGTH_SHORT).show();
            return;
        }
        firebaseHelper.getUser(currentUserId, new FirebaseHelper.FirestoreCallback<User>() {
            @Override
            public void onCallback(User user) {
                if (user != null && user.getCarData() != null && !user.getCarData().isEmpty()) {
                    if (!userChangedLocationText && currentGpsLocation != null) {
                        getCoordinatesFromAddress(dest, destLocation -> launchMapActivity(currentGpsLocation.getLatitude(), currentGpsLocation.getLongitude(),
                                destLocation.getLatitude(), destLocation.getLongitude(),
                                current, dest));
                    } else {
                        performGeocodingAndNavigate(current, dest);
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "Please select a car first.", Toast.LENGTH_SHORT).show();
                    openEngineChoiceDialog();
                }
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void performGeocodingAndNavigate(String currentLocStr, String desiredLocStr) {
        getCoordinatesFromAddress(currentLocStr, currentLoc -> getCoordinatesFromAddress(desiredLocStr, desiredLoc -> {
            launchMapActivity(currentLoc.getLatitude(), currentLoc.getLongitude(),
                    desiredLoc.getLatitude(), desiredLoc.getLongitude(),
                    currentLocStr, desiredLocStr);
        }));
    }

    private void launchMapActivity(double startLat, double startLng, double endLat, double endLng, String startAddr, String endAddr) {
        Intent intent = new Intent(HomeActivity.this, MapActivity.class);
        intent.putExtra("origin_lat", startLat);
        intent.putExtra("origin_lng", startLng);
        intent.putExtra("dest_lat", endLat);
        intent.putExtra("dest_lng", endLng);
        intent.putExtra("origin_address", startAddr);
        intent.putExtra("destination_address", endAddr);
        startActivity(intent);
    }

    private void getCoordinatesFromAddress(String address, Consumer<Location> callback) {
        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(HomeActivity.this);
            try {
                List<Address> addresses = geocoder.getFromLocationName(address, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address locAddr = addresses.get(0);
                    Location loc = new Location("");
                    loc.setLatitude(locAddr.getLatitude());
                    loc.setLongitude(locAddr.getLongitude());
                    runOnUiThread(() -> callback.accept(loc));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Address not found: " + address, Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) { e.printStackTrace(); }
        });
    }

    // map logic
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();

        mMap.setOnMapLongClickListener(latLng -> {
            selectedLatLng = latLng;
            if (selectedPointMarker != null) selectedPointMarker.remove();
            selectedPointMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Point"));

            locationInfoCard.setVisibility(View.VISIBLE);
            selectedAddressText.setText("Loading address...");

            executor.execute(() -> {
                Geocoder geocoder = new Geocoder(HomeActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        String address = addresses.get(0).getAddressLine(0);
                        runOnUiThread(() -> selectedAddressText.setText(address));
                    }
                } catch (IOException e) {
                    runOnUiThread(() -> selectedAddressText.setText("Unknown Location"));
                }
            });
        });

        mMap.setOnMapClickListener(latLng -> {
            locationInfoCard.setVisibility(View.GONE);
            if (selectedPointMarker != null) {
                selectedPointMarker.remove();
                selectedPointMarker = null;
            }
        });
        loadAcceptedMeetupsOnMap();
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof ChatMessage) {
                ChatMessage meetup = (ChatMessage) tag;
                showMeetupDetailsDialog(meetup);
                return true;
            }
            return false;
        });
    }

    private void setupLocationAndMap() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) updateMapWithLocation(location);
            }
        };
    }

    private void updateMapWithLocation(Location location) {
        if (mMap == null) return;
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.clear();
        loadAcceptedMeetupsOnMap();

        if (selectedPointMarker != null && selectedLatLng != null) {
            selectedPointMarker = mMap.addMarker(new MarkerOptions().position(selectedLatLng).title("Selected Point"));
        }

        mMap.addCircle(new CircleOptions()
                .center(currentLatLng)
                .radius(120)
                .strokeWidth(3f)
                .strokeColor(Color.BLUE)
                .fillColor(Color.argb(50, 0, 0, 255)));

        try { mMap.setMyLocationEnabled(true); } catch (SecurityException e) {}
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentGpsLocation = location;
                userChangedLocationText = false;

                try {
                    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        locInput.setText(addresses.get(0).getAddressLine(0));
                        userChangedLocationText = false;
                    }
                } catch (IOException e) { e.printStackTrace(); }

                updateMapWithLocation(location);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
            }
        });
    }

    // car and upload and sensors
    private void openEngineChoiceDialog() {
        String[] options = {"Select from Database", "Create Custom Profile", "Select from Your Profiles"};
        new AlertDialog.Builder(this).setTitle("Choose Car Option")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showCarMakesDialog(); break;
                        case 1: startActivity(new Intent(HomeActivity.this, CreateCarProfileActivity.class)); break;
                        case 2: loadAndShowCustomProfiles(); break;
                    }
                }).show();
    }

    private void showCarMakesDialog() {
        String[] carMakes = {"Toyota", "Ford", "Honda", "BMW", "Audi"};
        new AlertDialog.Builder(this).setTitle("Choose Make")
                .setItems(carMakes, (dialog, which) -> fetchCarsFromAPI(carMakes[which])).show();
    }

    private void fetchCarsFromAPI(String make) {
        String url = "https://vpic.nhtsa.dot.gov/api/vehicles/getmodelsformake/" + make + "?format=json";
        new OkHttpClient().newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    List<Car> cars = parseCarsFromResponse(response.body().string());
                    for (Car c : cars) { c.setFuelConsumption(8.0); c.setCo2Emissions(150); }
                    runOnUiThread(() -> displayCarsInDialog(cars));
                }
            }
        });
    }

    private List<Car> parseCarsFromResponse(String jsonResponse) {
        List<Car> cars = new ArrayList<>();
        try {
            JSONArray results = new JSONObject(jsonResponse).getJSONArray("Results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject obj = results.getJSONObject(i);
                cars.add(new Car(obj.getString("Make_Name"), obj.getString("Model_Name"), obj.optString("Model_Year", "N/A"), 0, 0, "Gasoline"));
            }
        } catch (JSONException e) {}
        return cars;
    }

    private void displayCarsInDialog(List<Car> cars) {
        String[] options = new String[cars.size()];
        for (int i=0; i<cars.size(); i++) options[i] = cars.get(i).getModel();
        new AlertDialog.Builder(this).setTitle("Choose Model").setItems(options, (d, w) -> saveCarToDatabase(cars.get(w))).show();
    }

    private void saveCarToDatabase(Car car) {
        if (currentUserId != null) {
            firebaseHelper.saveCarData(currentUserId, new Gson().toJson(car));
            Toast.makeText(this, "Car Saved: " + car.getModel(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAndShowCustomProfiles() {
        firebaseHelper.getCustomCarProfiles(currentUserId, new FirebaseHelper.FirestoreCallback<List<Car>>() {
            @Override public void onCallback(List<Car> customProfiles) {
                String[] options = new String[customProfiles.size()];
                for (int i=0; i<customProfiles.size(); i++) options[i] = customProfiles.get(i).getModel();
                new AlertDialog.Builder(HomeActivity.this).setTitle("Custom Profiles").setItems(options, (d, w) -> saveCarToDatabase(customProfiles.get(w))).show();
            }
        });
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            if (navigationView != null) {
                View headerView = navigationView.getHeaderView(0);
                ImageView navHeaderImage = headerView.findViewById(R.id.nav_header_image);
                if (navHeaderImage != null) {
                    Glide.with(HomeActivity.this).load(imageUri).circleCrop().into(navHeaderImage);
                }
            }
            uploadImageToFirebase();
        }
    }

    private void uploadImageToFirebase() {
        if (imageUri != null && currentUserId != null) {
            StorageReference ref = FirebaseStorage.getInstance().getReference("profile_images").child(currentUserId + ".jpg");

            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

            ref.putFile(imageUri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {

                firebaseHelper.getUser(currentUserId, new FirebaseHelper.FirestoreCallback<User>() {
                    @Override
                    public void onCallback(User existingUser) {
                        if (existingUser != null) {
                            existingUser.setImageUrl(uri.toString());

                            firebaseHelper.saveUser(currentUserId, existingUser, new FirebaseHelper.FirestoreCallback<Void>() {
                                @Override
                                public void onCallback(Void data) {
                                    Toast.makeText(HomeActivity.this, "Profile successfully updated!", Toast.LENGTH_SHORT).show();
                                    loadUserData();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(HomeActivity.this, "Failed to update database.", Toast.LENGTH_SHORT).show();
                    }
                });

            })).addOnFailureListener(e -> {
                Toast.makeText(HomeActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupSteps() {
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        loadDailySteps();
        dailyStepRunnable = new Runnable() {
            @Override public void run() { if(currentUserId!=null) firebaseHelper.saveDailySteps(currentUserId, currentDate, dailySteps); dailyStepHandler.postDelayed(this, 3600000); }
        };
        dailyStepHandler.post(dailyStepRunnable);
    }

    private void loadDailySteps() {
        firebaseHelper.getDailySteps(currentUserId, currentDate, new FirebaseHelper.FirestoreCallback<Integer>() {
            @Override public void onCallback(Integer steps) {
                dailySteps = steps;
                tvStepCount.setText("Steps: " + dailySteps);
            }
        });
    }
    private void saveDailySteps() { if (currentUserId != null) firebaseHelper.saveDailySteps(currentUserId, currentDate, dailySteps); }
    private void startDailyStepChecker() {
        dailyStepRunnable = new Runnable() {
            @Override public void run() { saveDailySteps(); dailyStepHandler.postDelayed(this, 120000); } //2mins
        };
        dailyStepHandler.post(dailyStepRunnable);
    }

    private void initStepCounter() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            sensorManager.registerListener(new SensorEventListener() {
                @Override public void onSensorChanged(SensorEvent event) {
                    if (previousStepCount == 0) previousStepCount = (int) event.values[0];
                    dailySteps += (int) event.values[0] - previousStepCount;
                    previousStepCount = (int) event.values[0];
                    tvStepCount.setText("Steps: " + dailySteps);
                }
                @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            }, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) getCurrentLocation();
            if (requestCode == REQUEST_ACTIVITY_RECOGNITION_PERMISSION) initStepCounter();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkFriendRequestsBadge();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && fusedLocationClient != null) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
        initStepCounter();
        if (mMap != null) {
            loadAcceptedMeetupsOnMap();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //checkFriendRequestsBadge();
        if (fusedLocationClient != null) fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void checkFriendRequestsBadge() {
        if (currentUserId != null && bottomNavigationView != null) {
            firebaseHelper.getFriendRequests(currentUserId, new FirebaseHelper.FirestoreCallback<List<User>>() {
                @Override
                public void onCallback(List<User> users) {
                    int count = users.size();
                    if (count > 0) {
                        // Afișează bula roșie cu numărul de cereri
                        bottomNavigationView.getOrCreateBadge(R.id.nav_community).setNumber(count);
                        bottomNavigationView.getOrCreateBadge(R.id.nav_community).setBackgroundColor(getColor(R.color.design_default_color_error));
                    } else {
                        // Ascunde bula dacă nu ai cereri
                        bottomNavigationView.removeBadge(R.id.nav_community);
                    }
                }
                @Override public void onFailure(Exception e) {}
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingRouteIntent(intent);
    }

    private void handleIncomingRouteIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("ACTION_ROUTE_TO_FAV", false)) {
            String destName = intent.getStringExtra("DEST_NAME");

            destinationContainer.setVisibility(View.VISIBLE);
            desiredInput.setText(destName);

            locInput.setText("");
            locInput.requestFocus();
            userChangedLocationText = true;

            Toast.makeText(this, "Please enter your starting point", Toast.LENGTH_SHORT).show();

            intent.removeExtra("ACTION_ROUTE_TO_FAV");
            setIntent(intent);
        }
    }

    private void discoverNearbyPlaces() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission needed!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentGpsLocation == null) {
            Toast.makeText(this, "Locating you... please wait a second.", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
            return;
        }

        Toast.makeText(this, "Finding cool places nearby...", Toast.LENGTH_SHORT).show();

        double lat = currentGpsLocation.getLatitude();
        double lng = currentGpsLocation.getLongitude();
        String apiKey = getString(R.string.google_maps_api_key);

        JSONObject jsonBody = new JSONObject();
        try {
            JSONArray includedTypes = new JSONArray();
            includedTypes.put("restaurant");
            includedTypes.put("park");
            includedTypes.put("museum");
            includedTypes.put("cafe");
            includedTypes.put("tourist_attraction");
            jsonBody.put("includedTypes", includedTypes);
            jsonBody.put("maxResultCount", 15);

            JSONObject center = new JSONObject();
            center.put("latitude", lat);
            center.put("longitude", lng);

            JSONObject circle = new JSONObject();
            circle.put("center", center);
            circle.put("radius", 2000.0); // am mărit raza la 2km

            JSONObject locationRestriction = new JSONObject();
            locationRestriction.put("circle", circle);
            jsonBody.put("locationRestriction", locationRestriction);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        String url = "https://places.googleapis.com/v1/places:searchNearby";

        // asking for the additional data from places API
        String fieldMask = "places.id,places.displayName,places.formattedAddress,places.rating,places.photos,places.location";

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Goog-FieldMask", fieldMask)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Network error", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());

                        if (jsonObject.has("places")) {
                            JSONArray placesArray = jsonObject.getJSONArray("places");
                            List<DiscoverPlace> foundPlaces = new ArrayList<>();

                            for (int i = 0; i < placesArray.length(); i++) {
                                JSONObject p = placesArray.getJSONObject(i);

                                String id = p.optString("id", "");
                                String name = p.has("displayName") ? p.getJSONObject("displayName").optString("text", "Unknown") : "Unknown";
                                String address = p.optString("formattedAddress", "No address available");
                                double rating = p.optDouble("rating", 0.0);

                                String photoUrl = "";
                                if (p.has("photos")) {
                                    JSONArray photos = p.getJSONArray("photos");
                                    if (photos.length() > 0) {
                                        String photoName = photos.getJSONObject(0).getString("name");
                                        photoUrl = "https://places.googleapis.com/v1/" + photoName + "/media?maxHeightPx=400&maxWidthPx=400&key=" + apiKey;
                                    }
                                }

                                double placeLat = 0.0;
                                double placeLng = 0.0;
                                if (p.has("location")) {
                                    JSONObject loc = p.getJSONObject("location");
                                    placeLat = loc.optDouble("latitude", 0.0);
                                    placeLng = loc.optDouble("longitude", 0.0);
                                }

                                // distance by an android func
                                float[] distanceResults = new float[1];
                                android.location.Location.distanceBetween(lat, lng, placeLat, placeLng, distanceResults);
                                float distance = distanceResults[0];

                                foundPlaces.add(new DiscoverPlace(id, name, address, photoUrl, rating, placeLat, placeLng, distance));
                            }

                            runOnUiThread(() -> showDiscoverBottomSheet(foundPlaces));

                        } else {
                            runOnUiThread(() -> Toast.makeText(HomeActivity.this, "No places found in this area.", Toast.LENGTH_SHORT).show());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void showDiscoverBottomSheet(List<DiscoverPlace> placesList) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_discover_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        RecyclerView rv = view.findViewById(R.id.rvDiscoverPlaces);
        rv.setLayoutManager(new LinearLayoutManager(this));

        DiscoverPlacesAdapter adapter = new DiscoverPlacesAdapter(this, placesList, place -> {
            showPlaceDetails(place);
        });
        rv.setAdapter(adapter);

        bottomSheetDialog.show();
    }
    private void showPlaceDetails(DiscoverPlace place) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_place_details, null);
        dialog.setContentView(view);

        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        ImageView img = view.findViewById(R.id.detailImage);
        TextView name = view.findViewById(R.id.detailName);
        TextView distance = view.findViewById(R.id.detailDistance);
        com.google.android.material.button.MaterialButton btnGoTo = view.findViewById(R.id.btnGoTo);
        com.google.android.material.button.MaterialButton btnFav = view.findViewById(R.id.btnFavorite);

        RecyclerView rvComments = view.findViewById(R.id.rvPlaceComments);
        EditText etNewComment = view.findViewById(R.id.etNewComment);
        ImageView btnSendComment = view.findViewById(R.id.btnSendComment);

        name.setText(place.getName());
        distance.setText(String.format(java.util.Locale.getDefault(), "%.1f km away • %s", place.getDistanceInMeters() / 1000f, place.getAddress()));

        if (place.getPhotoUrl() != null && !place.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(place.getPhotoUrl()).into(img);
        }

        // RecyclerView for comms
        rvComments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        // load comments func
        Runnable loadComments = () -> {
            firebaseHelper.getPlaceComments(place.getId(), new FirebaseHelper.FirestoreCallback<List<PlaceComment>>() {
                @Override
                public void onCallback(List<PlaceComment> comments) {
                    PlaceCommentsAdapter adapter = new PlaceCommentsAdapter(HomeActivity.this, comments, currentUserId, new PlaceCommentsAdapter.OnCommentActionListener() {
                        @Override
                        public void onHelpfulClicked(PlaceComment comment, boolean isCurrentlyHelpful) {
                            firebaseHelper.setCommentVote(place.getId(), comment.getId(), currentUserId, true, new FirebaseHelper.FirestoreCallback<Void>() {
                                @Override public void onCallback(Void data) { /* Reîncărcăm lista pentru a vedea votul */ loadPlaceComments(place.getId(), rvComments); }
                            });
                        }

                        @Override
                        public void onUnhelpfulClicked(PlaceComment comment, boolean isCurrentlyUnhelpful) {
                            firebaseHelper.setCommentVote(place.getId(), comment.getId(), currentUserId, false, new FirebaseHelper.FirestoreCallback<Void>() {
                                @Override public void onCallback(Void data) { loadPlaceComments(place.getId(), rvComments); }
                            });
                        }

                        @Override
                        public void onReplyClicked(PlaceComment comment) {
                            showReplyDialog(place.getId(), comment);
                        }
                    });
                    rvComments.setAdapter(adapter);
                }
            });
        };

        loadComments.run();

        // new comms
        btnSendComment.setOnClickListener(v -> {
            String text = etNewComment.getText().toString().trim();
            if (!text.isEmpty()) {
                // user data and photos
                firebaseHelper.getUser(currentUserId, new FirebaseHelper.FirestoreCallback<User>() {
                    @Override
                    public void onCallback(User user) {
                        if (user != null) {
                            firebaseHelper.addPlaceComment(place.getId(), currentUserId, user.getUsername(), user.getImageUrl(), text, new FirebaseHelper.FirestoreCallback<Void>() {
                                @Override
                                public void onCallback(Void data) {
                                    etNewComment.setText("");
                                    Toast.makeText(HomeActivity.this, "Review posted!", Toast.LENGTH_SHORT).show();
                                    loadComments.run();
                                }
                            });
                        }
                    }
                });
            }
        });

        // go to
        btnGoTo.setOnClickListener(v -> {
            dialog.dismiss();
            if (currentGpsLocation != null) {
                launchMapActivity(
                        currentGpsLocation.getLatitude(), currentGpsLocation.getLongitude(),
                        place.getLat(), place.getLng(),
                        "Current Location", place.getName()
                );
            } else {
                Toast.makeText(this, "Locația curentă nu este disponibilă.", Toast.LENGTH_SHORT).show();
            }
        });

        // faves
        btnFav.setIconResource(android.R.drawable.star_off);
        btnFav.setText("Save");
        final boolean[] isFav = {false};
        final String[] favDocId = {null};

        firebaseHelper.checkFavoriteExists(currentUserId, place.getAddress(), new FirebaseHelper.FirestoreCallback<String>() {
            @Override
            public void onCallback(String docId) {
                if (docId != null) {
                    isFav[0] = true;
                    favDocId[0] = docId;
                    btnFav.setIconResource(android.R.drawable.star_on);
                    btnFav.setText("Saved");
                }
            }
        });

        btnFav.setOnClickListener(v -> {
            if (isFav[0] && favDocId[0] != null) {
                firebaseHelper.deleteFavoriteLocation(currentUserId, favDocId[0], new FirebaseHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onCallback(Void data) {
                        isFav[0] = false;
                        favDocId[0] = null;
                        btnFav.setIconResource(android.R.drawable.star_off);
                        btnFav.setText("Save");
                        Toast.makeText(HomeActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                firebaseHelper.saveFavoriteLocation(currentUserId, place.getName(), place.getAddress(), place.getLat(), place.getLng(), new FirebaseHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onCallback(Void data) {
                        isFav[0] = true;
                        btnFav.setIconResource(android.R.drawable.star_on);
                        btnFav.setText("Saved");
                        Toast.makeText(HomeActivity.this, "Saved to favorites!", Toast.LENGTH_SHORT).show();

                        firebaseHelper.checkFavoriteExists(currentUserId, place.getAddress(), new FirebaseHelper.FirestoreCallback<String>() {
                            @Override public void onCallback(String docId) { favDocId[0] = docId; }
                        });
                    }
                });
            }
        });

        dialog.show();
    }

    private void loadPlaceComments(String placeId, RecyclerView rvComments) {
        firebaseHelper.getPlaceComments(placeId, new FirebaseHelper.FirestoreCallback<List<PlaceComment>>() {
            @Override
            public void onCallback(List<PlaceComment> comments) {
                PlaceCommentsAdapter adapter = new PlaceCommentsAdapter(HomeActivity.this, comments, currentUserId, new PlaceCommentsAdapter.OnCommentActionListener() {
                    @Override
                    public void onHelpfulClicked(PlaceComment comment, boolean isCurrentlyHelpful) {
                        firebaseHelper.setCommentVote(placeId, comment.getId(), currentUserId, true, new FirebaseHelper.FirestoreCallback<Void>() {
                            @Override public void onCallback(Void data) { loadPlaceComments(placeId, rvComments); }
                        });
                    }

                    @Override
                    public void onUnhelpfulClicked(PlaceComment comment, boolean isCurrentlyUnhelpful) {
                        firebaseHelper.setCommentVote(placeId, comment.getId(), currentUserId, false, new FirebaseHelper.FirestoreCallback<Void>() {
                            @Override public void onCallback(Void data) { loadPlaceComments(placeId, rvComments); }
                        });
                    }

                    @Override
                    public void onReplyClicked(PlaceComment comment) {
                        showReplyDialog(placeId, comment);
                    }
                });
                rvComments.setAdapter(adapter);
            }
        });
    }

    private void showReplyDialog(String placeId, PlaceComment comment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reply to " + comment.getUserName());

        final EditText input = new EditText(this);
        input.setHint("Write your reply here...");
        input.setPadding(40, 40, 40, 40);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String replyText = input.getText().toString().trim();
            if (!replyText.isEmpty()) {
                firebaseHelper.getUser(currentUserId, new FirebaseHelper.FirestoreCallback<User>() {
                    @Override
                    public void onCallback(User user) {
                        if (user != null) {
                            firebaseHelper.addReplyToComment(placeId, comment.getId(), currentUserId, user.getUsername(), user.getImageUrl(), replyText, new FirebaseHelper.FirestoreCallback<Void>() {
                                @Override
                                public void onCallback(Void data) {
                                    Toast.makeText(HomeActivity.this, "Reply posted!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    private void loadAcceptedMeetupsOnMap() {
        if (currentUserId == null || mMap == null) return;

        firebaseHelper.getAcceptedMeetups(currentUserId, new FirebaseHelper.FirestoreCallback<java.util.List<ChatMessage>>() {
            @Override
            public void onCallback(java.util.List<ChatMessage> meetups) {
                for (com.google.android.gms.maps.model.Marker m : meetupMarkers) {
                    if (m != null) m.remove();
                }
                meetupMarkers.clear();

                for (ChatMessage meetup : meetups) {
                    com.google.android.gms.maps.model.LatLng pos = new com.google.android.gms.maps.model.LatLng(meetup.getMeetupLat(), meetup.getMeetupLng());

                    com.google.android.gms.maps.model.MarkerOptions options = new com.google.android.gms.maps.model.MarkerOptions()
                            .position(pos)
                            .title(meetup.getMeetupName())
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_VIOLET));

                    com.google.android.gms.maps.model.Marker marker = mMap.addMarker(options);
                    if (marker != null) {
                        marker.setTag(meetup);
                        meetupMarkers.add(marker);
                    }
                }
            }
        });
    }

    private void showMeetupDetailsDialog(ChatMessage meetup) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_meetup_details, null);
        dialog.setContentView(view);

        android.view.View parent = (android.view.View) view.getParent();
        if (parent != null) {
            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        android.widget.TextView tvName = view.findViewById(R.id.tvMeetupDetailsName);
        android.widget.TextView tvDesc = view.findViewById(R.id.tvMeetupDetailsDesc);
        android.widget.TextView tvTime = view.findViewById(R.id.tvMeetupDetailsTime);
        android.widget.TextView tvAddress = view.findViewById(R.id.tvMeetupDetailsAddress);
        com.google.android.material.button.MaterialButton btnGoTo = view.findViewById(R.id.btnGoToMeetup);

        tvName.setText(meetup.getMeetupName());

        if (meetup.getMeetupDescription() != null && !meetup.getMeetupDescription().trim().isEmpty()) {
            tvDesc.setText(meetup.getMeetupDescription());
            tvDesc.setVisibility(android.view.View.VISIBLE);
        } else {
            tvDesc.setVisibility(android.view.View.GONE);
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
        tvTime.setText("Scheduled for: " + sdf.format(new java.util.Date(meetup.getMeetupTime())));
        tvAddress.setText(meetup.getMeetupAddress());

        btnGoTo.setOnClickListener(v -> {
            dialog.dismiss();
            if (currentGpsLocation != null) {
                launchMapActivity(
                        currentGpsLocation.getLatitude(), currentGpsLocation.getLongitude(),
                        meetup.getMeetupLat(), meetup.getMeetupLng(),
                        "Current Location", meetup.getMeetupName()
                );
            } else {
                android.widget.Toast.makeText(this, "Locația curentă nu este disponibilă încă.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }


}