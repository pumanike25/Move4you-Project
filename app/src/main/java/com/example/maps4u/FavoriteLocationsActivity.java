package com.example.maps4u;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class FavoriteLocationsActivity extends AppCompatActivity {

    private RecyclerView rvFavorites;
    private FavoritesAdapter adapter;
    private List<FavoriteLocation> favoritesList;

    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        rvFavorites = findViewById(R.id.rvFavorites);

        favoritesList = new ArrayList<>();
        adapter = new FavoritesAdapter(favoritesList);
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        rvFavorites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        firebaseHelper.getFavoriteLocations(currentUserId, new FirebaseHelper.FirestoreCallback<List<FavoriteLocation>>() {
            @Override
            public void onCallback(List<FavoriteLocation> favs) {
                favoritesList.clear();
                favoritesList.addAll(favs);
                adapter.notifyDataSetChanged();

                if(favs.isEmpty()) {
                    Toast.makeText(FavoriteLocationsActivity.this, "No favorite locations saved yet.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Exception e) {
                Toast.makeText(FavoriteLocationsActivity.this, "Error loading favorites", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRoutingDialog(FavoriteLocation location) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_route_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        TextView tvAddress = view.findViewById(R.id.tvDialogAddress);
        LinearLayout btnCurrentLoc = view.findViewById(R.id.btnCurrentLocation);
        LinearLayout btnChooseStart = view.findViewById(R.id.btnChooseStart);

        tvAddress.setText(location.getAddress());

        btnCurrentLoc.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            sendRouteIntent(location, true);
        });

        btnChooseStart.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            sendRouteIntent(location, false);
        });

        bottomSheetDialog.show();
    }

    private void sendRouteIntent(FavoriteLocation location, boolean useCurrentLocation) {
        if (useCurrentLocation) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(currentLocation -> {
                if (currentLocation != null) {
                    // Verificăm dacă are mașina selectată (exact ca în HomeActivity)
                    firebaseHelper.getUser(currentUserId, new FirebaseHelper.FirestoreCallback<User>() {
                        @Override
                        public void onCallback(User user) {
                            if (user != null && user.getCarData() != null && !user.getCarData().isEmpty()) {

                                // Lansăm direct MapActivity (Traseul) și îi dăm direct coordonatele exacte!
                                Intent intent = new Intent(FavoriteLocationsActivity.this, MapActivity.class);
                                intent.putExtra("origin_lat", currentLocation.getLatitude());
                                intent.putExtra("origin_lng", currentLocation.getLongitude());
                                intent.putExtra("dest_lat", location.getLatitude());
                                intent.putExtra("dest_lng", location.getLongitude());
                                intent.putExtra("origin_address", "My Location");
                                intent.putExtra("destination_address", location.getAddress());

                                startActivity(intent);
                                finish(); // Închidem lista de favorite

                            } else {
                                Toast.makeText(FavoriteLocationsActivity.this, "Please select a car in Home first.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Exception e) {}
                    });
                } else {
                    Toast.makeText(FavoriteLocationsActivity.this, "Locating GPS... Try again in a second.", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Intent intent = new Intent(FavoriteLocationsActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            intent.putExtra("ACTION_ROUTE_TO_FAV", true);
            intent.putExtra("DEST_LAT", location.getLatitude());
            intent.putExtra("DEST_LNG", location.getLongitude());
            intent.putExtra("DEST_NAME", location.getAddress());
            intent.putExtra("USE_CURRENT_LOCATION", false);

            startActivity(intent);
            finish();
        }
    }

    // adapter
    private class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavViewHolder> {
        private List<FavoriteLocation> list;

        public FavoritesAdapter(List<FavoriteLocation> list) { this.list = list; }

        @NonNull
        @Override
        public FavViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite, parent, false);
            return new FavViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull FavViewHolder holder, int position) {
            FavoriteLocation fav = list.get(position);

            holder.tvFavName.setText(fav.getCustomName());
            holder.tvFavAddress.setText(fav.getAddress());

            holder.itemView.setOnClickListener(v -> showRoutingDialog(fav));

            holder.btnEditFav.setOnClickListener(v -> showEditDialog(fav));
            holder.btnDeleteFav.setOnClickListener(v -> showDeleteConfirmationDialog(fav));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class FavViewHolder extends RecyclerView.ViewHolder {
            TextView tvFavName, tvFavAddress;
            ImageView btnEditFav, btnDeleteFav;

            public FavViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFavName = itemView.findViewById(R.id.tvFavName);
                tvFavAddress = itemView.findViewById(R.id.tvFavAddress);
                btnEditFav = itemView.findViewById(R.id.btnEditFav);
                btnDeleteFav = itemView.findViewById(R.id.btnDeleteFav);
            }
        }
    }

    private void showEditDialog(FavoriteLocation location) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Edit Location Name");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setText(location.getCustomName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                firebaseHelper.updateFavoriteName(currentUserId, location.getId(), newName, new FirebaseHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onCallback(Void data) {
                        Toast.makeText(FavoriteLocationsActivity.this, "Name updated", Toast.LENGTH_SHORT).show();
                        loadFavorites(); // Reîncărcăm lista pentru a vedea modificarea
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(FavoriteLocationsActivity.this, "Error updating name", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteConfirmationDialog(FavoriteLocation location) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Delete Location");
        builder.setMessage("Are you sure you want to delete '" + location.getCustomName() + "'?");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            firebaseHelper.deleteFavoriteLocation(currentUserId, location.getId(), new FirebaseHelper.FirestoreCallback<Void>() {
                @Override
                public void onCallback(Void data) {
                    Toast.makeText(FavoriteLocationsActivity.this, "Location deleted", Toast.LENGTH_SHORT).show();
                    loadFavorites();
                }
                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(FavoriteLocationsActivity.this, "Error deleting location", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}