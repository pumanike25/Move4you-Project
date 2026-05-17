package com.example.maps4u;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TrophiesActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView rvTrophies;

    private FirebaseHelper firebaseHelper;
    private String currentUserId;

    private List<Trophy> allTrophies;
    private List<Trophy> displayedTrophies;
    private TrophiesAdapter adapter;

    private int currentRoutesCount = 0;
    private long currentTotalSteps = 0;
    private Map<String, Long> dbUnlockedTrophies; // Tine minte ce ai in baza de date

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trophies);

        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();

        initViews();
        setupRecyclerView();
        fetchUserStatsAndBuildTrophies();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayoutTrophies);
        rvTrophies = findViewById(R.id.rvTrophies);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterTrophies(tab.getPosition() == 0); // 0 = Unlocked, 1 = Locked
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        allTrophies = new ArrayList<>();
        displayedTrophies = new ArrayList<>();
        adapter = new TrophiesAdapter(displayedTrophies);
        rvTrophies.setLayoutManager(new LinearLayoutManager(this));
        rvTrophies.setAdapter(adapter);
    }

    private void fetchUserStatsAndBuildTrophies() {
        // 1. Luam rutele
        firebaseHelper.getUserHistory(currentUserId, new FirebaseHelper.FirestoreCallback<com.google.firebase.firestore.QuerySnapshot>() {
            @Override
            public void onCallback(com.google.firebase.firestore.QuerySnapshot snapshots) {
                currentRoutesCount = (snapshots != null) ? snapshots.size() : 0;

                // 2. Luam pasii
                firebaseHelper.getAllDailySteps(currentUserId, new FirebaseHelper.FirestoreCallback<List<Map<String, Object>>>() {
                    @Override
                    public void onCallback(List<Map<String, Object>> stepsList) {
                        for (Map<String, Object> day : stepsList) {
                            Long steps = (Long) day.get("steps");
                            if (steps != null) currentTotalSteps += steps;
                        }

                        // 3. Luam trofeele deblocate inainte din Firebase
                        firebaseHelper.getUnlockedTrophies(currentUserId, new FirebaseHelper.FirestoreCallback<Map<String, Long>>() {
                            @Override
                            public void onCallback(Map<String, Long> unlockedTrophiesData) {
                                dbUnlockedTrophies = unlockedTrophiesData;
                                buildTrophiesList();
                            }
                        });
                    }
                });
            }
        });
    }

    private void buildTrophiesList() {
        allTrophies.clear();

        // 1. Trofee pentru Rute
        addTrophyToCheck(new Trophy("route_1", "First Journey", "Complete your first route in the app.", android.R.drawable.ic_menu_mapmode, currentRoutesCount, 1, "#4CAF50"));
        addTrophyToCheck(new Trophy("route_5", "Explorer", "Complete 5 different routes.", android.R.drawable.ic_menu_compass, currentRoutesCount, 5, "#2196F3"));
        addTrophyToCheck(new Trophy("route_20", "Frequent Traveler", "Complete 20 routes.", android.R.drawable.star_on, currentRoutesCount, 20, "#FF9800"));
        addTrophyToCheck(new Trophy("route_50", "Road Master", "Complete 50 routes.", android.R.drawable.ic_menu_directions, currentRoutesCount, 50, "#9C27B0"));

        // 2. Trofee pentru Pasi / Eco
        addTrophyToCheck(new Trophy("step_1k", "Getting Started", "Walk your first 1,000 steps.", android.R.drawable.ic_menu_directions, (int)currentTotalSteps, 1000, "#4CAF50"));
        addTrophyToCheck(new Trophy("step_10k", "10K Club", "Walk a total of 10,000 steps.", android.R.drawable.ic_menu_directions, (int)currentTotalSteps, 10000, "#2196F3"));
        addTrophyToCheck(new Trophy("step_50k", "Marathoner", "Walk a total of 50,000 steps.", android.R.drawable.ic_menu_directions, (int)currentTotalSteps, 50000, "#E91E63"));
        addTrophyToCheck(new Trophy("step_100k", "Eco Warrior", "Walk 100,000 steps and save massive CO2.", android.R.drawable.ic_menu_directions, (int)currentTotalSteps, 100000, "#4CAF50"));

        // Afisam trofeele pentru tab-ul activ
        filterTrophies(tabLayout.getSelectedTabPosition() == 0);
    }

    private void addTrophyToCheck(Trophy t) {
        // Daca l-ai deblocat conform progresului
        if (t.isUnlocked()) {
            if (dbUnlockedTrophies.containsKey(t.id)) {
                // Dacă exista deja în baza de date, preluăm timestamp-ul
                t.unlockedTimestamp = dbUnlockedTrophies.get(t.id);
            } else {
                // Dacă abia acum l-ai deblocat, il salvam in Firebase cu data de azi
                t.unlockedTimestamp = System.currentTimeMillis();
                firebaseHelper.saveUnlockedTrophy(currentUserId, t.id, null);
            }
        }
        allTrophies.add(t);
    }

    private void filterTrophies(boolean showUnlocked) {
        displayedTrophies.clear();
        for (Trophy t : allTrophies) {
            if (t.isUnlocked() == showUnlocked) {
                displayedTrophies.add(t);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // --- MODEL TROFEU ---
    class Trophy {
        String id, title, description, colorHex;
        int iconResId, currentProgress, maxProgress;
        long unlockedTimestamp = 0;
        boolean isExpanded = false;

        public Trophy(String id, String title, String description, int iconResId, int currentProgress, int maxProgress, String colorHex) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.iconResId = iconResId;
            this.currentProgress = currentProgress;
            this.maxProgress = maxProgress;
            this.colorHex = colorHex;
        }

        public boolean isUnlocked() {
            return currentProgress >= maxProgress;
        }

        public String getFormattedDate() {
            if (unlockedTimestamp == 0) return "Just now";
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(unlockedTimestamp));
        }
    }

    // --- ADAPTER ---
    private class TrophiesAdapter extends RecyclerView.Adapter<TrophiesAdapter.TrophyViewHolder> {
        private List<Trophy> trophies;

        public TrophiesAdapter(List<Trophy> trophies) { this.trophies = trophies; }

        @NonNull
        @Override
        public TrophyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trophy_card, parent, false);
            return new TrophyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TrophyViewHolder holder, int position) {
            Trophy trophy = trophies.get(position);

            holder.tvTrophyTitle.setText(trophy.title);
            holder.tvTrophyDesc.setText(trophy.description);
            holder.imgTrophyIcon.setImageResource(trophy.iconResId);

            if (trophy.isUnlocked()) {
                holder.imgTrophyIcon.setColorFilter(Color.parseColor(trophy.colorHex));
                holder.tvTrophyStatus.setText("Unlocked on: " + trophy.getFormattedDate());
                holder.tvTrophyStatus.setTextColor(Color.parseColor("#4CAF50")); // Verde

                holder.pbTrophyProgress.setProgress(100);
                holder.tvProgressText.setText("Completed");

                // Expandăm/ascundem descrierea la click pentru cele Unlocked
                holder.layoutTrophyDetails.setVisibility(trophy.isExpanded ? View.VISIBLE : View.GONE);

            } else {
                holder.imgTrophyIcon.setColorFilter(Color.GRAY);
                holder.tvTrophyStatus.setText("Status: Locked");
                holder.tvTrophyStatus.setTextColor(Color.GRAY);

                int progressPercent = (int) (((float) trophy.currentProgress / trophy.maxProgress) * 100);
                holder.pbTrophyProgress.setProgress(progressPercent);
                holder.tvProgressText.setText(trophy.currentProgress + " / " + trophy.maxProgress);

                // Pentru cele Locked lăsăm detaliile mereu vizibile să vadă progresul
                holder.layoutTrophyDetails.setVisibility(View.VISIBLE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (trophy.isUnlocked()) {
                    trophy.isExpanded = !trophy.isExpanded;
                    notifyItemChanged(position);
                }
            });
        }

        @Override
        public int getItemCount() { return trophies.size(); }

        class TrophyViewHolder extends RecyclerView.ViewHolder {
            ImageView imgTrophyIcon;
            TextView tvTrophyTitle, tvTrophyStatus, tvTrophyDesc, tvProgressText;
            ProgressBar pbTrophyProgress;
            LinearLayout layoutTrophyDetails;

            public TrophyViewHolder(@NonNull View itemView) {
                super(itemView);
                imgTrophyIcon = itemView.findViewById(R.id.imgTrophyIcon);
                tvTrophyTitle = itemView.findViewById(R.id.tvTrophyTitle);
                tvTrophyStatus = itemView.findViewById(R.id.tvTrophyStatus);
                tvTrophyDesc = itemView.findViewById(R.id.tvTrophyDesc);
                tvProgressText = itemView.findViewById(R.id.tvProgressText);
                pbTrophyProgress = itemView.findViewById(R.id.pbTrophyProgress);
                layoutTrophyDetails = itemView.findViewById(R.id.layoutTrophyDetails);
            }
        }
    }
}