package com.example.maps4u;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PublicProfileActivity extends AppCompatActivity {

    private ImageView publicProfileImage;
    private TextView publicUsername;
    private Button btnFriendAction;

    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private String targetUserId;
    private String currentFriendshipStatus = "none";
    private LinearLayout trophiesContainer;
    private com.google.android.material.card.MaterialCardView cardTrophies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();
        targetUserId = getIntent().getStringExtra("TARGET_UID");

        if (targetUserId == null || currentUserId == null) {
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadTargetUserData();
        checkStatusAndUpdateUI();

        loadUserPrivacySettings();
    }

    private void initViews() {
        publicProfileImage = findViewById(R.id.publicProfileImage);
        publicUsername = findViewById(R.id.publicUsername);
        btnFriendAction = findViewById(R.id.btnFriendAction);

        cardTrophies = findViewById(R.id.cardTrophies);

        trophiesContainer = findViewById(R.id.trophiesContainer);

        btnFriendAction.setOnClickListener(v -> handleFriendActionButton());
    }

    private void loadTargetUserData() {
        firebaseHelper.getUser(targetUserId, user -> {
            if (user != null) {
                publicUsername.setText(user.getUsername());
                if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                    Glide.with(PublicProfileActivity.this)
                            .load(user.getImageUrl())
                            .circleCrop()
                            .into(publicProfileImage);
                }
            }
        });
    }

    private void checkStatusAndUpdateUI() {
        btnFriendAction.setEnabled(false);
        firebaseHelper.checkFriendshipStatus(currentUserId, targetUserId, status -> {
            currentFriendshipStatus = status;
            updateButtonUI();
        });
    }

    private void updateButtonUI() {
        btnFriendAction.setEnabled(true);
        switch (currentFriendshipStatus) {
            case "none":
                btnFriendAction.setText("Add Friend");
                btnFriendAction.setBackgroundColor(getColor(R.color.primary));
                break;
            case "request_sent":
                btnFriendAction.setText("Request Sent (Cancel)");
                btnFriendAction.setBackgroundColor(Color.GRAY);
                break;
            case "request_received":
                btnFriendAction.setText("Accept Request");
                btnFriendAction.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
            case "friends":
                btnFriendAction.setText("Friends (Unfriend)");
                btnFriendAction.setBackgroundColor(Color.parseColor("#F44336"));
                break;
        }
    }

    private void handleFriendActionButton() {
        btnFriendAction.setEnabled(false);
        switch (currentFriendshipStatus) {
            case "none":
                firebaseHelper.sendFriendRequest(currentUserId, targetUserId, data -> checkStatusAndUpdateUI());
                break;
            case "request_sent":
            case "friends":
                firebaseHelper.removeFriendOrRequest(currentUserId, targetUserId, data -> checkStatusAndUpdateUI());
                break;
            case "request_received":
                firebaseHelper.acceptFriendRequest(currentUserId, targetUserId, data -> checkStatusAndUpdateUI());
                break;
        }
    }

    // --- private data load ---

    private void loadUserPrivacySettings() {
        FirebaseFirestore.getInstance().collection("users").document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    boolean isPrivate = false;
                    List<String> selectedTrophies = null;

                    if (doc.exists()) {
                        if (doc.contains("isPrivate")) isPrivate = doc.getBoolean("isPrivate");
                        if (doc.contains("selectedTrophies")) selectedTrophies = (List<String>) doc.get("selectedTrophies");
                    }

                    if (isPrivate) {
                        if (cardTrophies != null) {
                            cardTrophies.setVisibility(View.GONE);
                        }
                        Toast.makeText(this, "This user has a private profile.", Toast.LENGTH_SHORT).show();
                    } else {
                        if (selectedTrophies != null && !selectedTrophies.isEmpty()) {
                            for (String trophyName : selectedTrophies) {
                                renderSpecificTrophy(trophyName);
                            }
                        } else {
                            addTrophyToView("Newcomer", "Just starting the journey!", android.R.drawable.ic_menu_info_details, "#9E9E9E");
                        }
                    }
                });
    }

    private void renderSpecificTrophy(String trophyName) {
        switch (trophyName) {
            case "First Journey":
                addTrophyToView("First Journey", "Completed the first route.", android.R.drawable.ic_menu_mapmode, "#4CAF50");
                break;
            case "Explorer":
                addTrophyToView("Explorer", "Completed 5 different routes.", android.R.drawable.ic_menu_compass, "#2196F3");
                break;
            case "Frequent Traveler":
                addTrophyToView("Frequent Traveler", "Completed 20 routes.", android.R.drawable.star_on, "#FF9800");
                break;
            case "10K Club":
                addTrophyToView("10K Club", "Walked over 10,000 steps in total.", android.R.drawable.ic_menu_directions, "#4CAF50");
                break;
            case "Marathoner":
                addTrophyToView("Marathoner", "Walked over 50,000 steps in total.", android.R.drawable.ic_menu_directions, "#E91E63");
                break;
        }
    }

    private void addTrophyToView(String title, String desc, int iconResId, String colorHex) {
        runOnUiThread(() -> {
            View trophyView = getLayoutInflater().inflate(R.layout.item_trophy, trophiesContainer, false);

            ImageView icon = trophyView.findViewById(R.id.trophyIcon);
            TextView tvTitle = trophyView.findViewById(R.id.trophyTitle);
            TextView tvDesc = trophyView.findViewById(R.id.trophyDesc);

            icon.setImageResource(iconResId);
            icon.setColorFilter(Color.parseColor(colorHex));
            tvTitle.setText(title);
            tvDesc.setText(desc);

            trophiesContainer.addView(trophyView);
        });
    }
}