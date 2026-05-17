package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private TabLayout tabLayoutCommunity;
    private LinearLayout viewFriends, viewRequests, viewDiscover;
    private EditText etSearchUsers;

    private RecyclerView rvFriendsList, rvRequestsList, rvSearchList;
    private UsersAdapter friendsAdapter, requestsAdapter, searchAdapter;
    private List<User> friendsList, requestsList, searchResults;

    private FirebaseHelper firebaseHelper;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();

        initViews();
        setupAdapters();
        setupTabs();
        setupSearchLogic();

        loadFriends();
        loadRequests(); // Il apelam aici ca sa punem notificarea (badge-ul) de la inceput
    }

    private void initViews() {
        tabLayoutCommunity = findViewById(R.id.tabLayoutCommunity);
        viewFriends = findViewById(R.id.viewFriends);
        viewRequests = findViewById(R.id.viewRequests);
        viewDiscover = findViewById(R.id.viewDiscover);
        etSearchUsers = findViewById(R.id.etSearchUsers);
        rvFriendsList = findViewById(R.id.rvFriendsList);
        rvRequestsList = findViewById(R.id.rvRequestsList);
        rvSearchList = findViewById(R.id.rvSearchList);

    }

    private void setupAdapters() {
        friendsList = new ArrayList<>();
        requestsList = new ArrayList<>();
        searchResults = new ArrayList<>();

        // Adapterele stiu acum in ce lista se afla pe baza string-ului
        friendsAdapter = new UsersAdapter(friendsList, "FRIENDS");
        requestsAdapter = new UsersAdapter(requestsList, "REQUESTS");
        searchAdapter = new UsersAdapter(searchResults, "DISCOVER");

        rvFriendsList.setLayoutManager(new LinearLayoutManager(this));
        rvFriendsList.setAdapter(friendsAdapter);

        rvRequestsList.setLayoutManager(new LinearLayoutManager(this));
        rvRequestsList.setAdapter(requestsAdapter);

        rvSearchList.setLayoutManager(new LinearLayoutManager(this));
        rvSearchList.setAdapter(searchAdapter);
    }

    private void setupTabs() {
        tabLayoutCommunity.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewFriends.setVisibility(View.GONE);
                viewRequests.setVisibility(View.GONE);
                viewDiscover.setVisibility(View.GONE);

                switch (tab.getPosition()) {
                    case 0:
                        viewFriends.setVisibility(View.VISIBLE);
                        loadFriends();
                        break;
                    case 1:
                        viewRequests.setVisibility(View.VISIBLE);
                        loadRequests();
                        break;
                    case 2:
                        viewDiscover.setVisibility(View.VISIBLE);
                        break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadFriends() {
        firebaseHelper.getFriendsList(currentUserId, new FirebaseHelper.FirestoreCallback<List<User>>() {
            @Override
            public void onCallback(List<User> users) {
                friendsList.clear();
                friendsList.addAll(users);
                friendsAdapter.notifyDataSetChanged();
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void loadRequests() {
        firebaseHelper.getFriendRequests(currentUserId, new FirebaseHelper.FirestoreCallback<List<User>>() {
            @Override
            public void onCallback(List<User> users) {
                requestsList.clear();
                requestsList.addAll(users);
                requestsAdapter.notifyDataSetChanged();

                // NOTIFICARE PE TAB (Bula rosie)
                updateTabBadge(users.size());
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void updateTabBadge(int count) {
        if (count > 0) {
            BadgeDrawable badge = tabLayoutCommunity.getTabAt(1).getOrCreateBadge();
            badge.setNumber(count);
            badge.setBackgroundColor(getColor(R.color.design_default_color_error));
            badge.setBadgeTextColor(getColor(R.color.white));
        } else {
            tabLayoutCommunity.getTabAt(1).removeBadge();
        }
    }

    private void setupSearchLogic() {
        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() > 0) {
                    firebaseHelper.searchUsers(query, new FirebaseHelper.FirestoreCallback<List<User>>() {
                        @Override
                        public void onCallback(List<User> users) {
                            searchResults.clear();
                            for (User u : users) {
                                if (u.getUid() != null && !u.getUid().equals(currentUserId)) searchResults.add(u);
                            }
                            searchAdapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    searchResults.clear();
                    searchAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    // --- ADAPTER-UL INTELIGENT ---
    private class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
        private List<User> usersList;
        private String listType;

        public UsersAdapter(List<User> usersList, String listType) {
            this.usersList = usersList;
            this.listType = listType;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_search, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = usersList.get(position);
            holder.tvUsername.setText(user.getUsername());

            if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                Glide.with(CommunityActivity.this).load(user.getImageUrl()).circleCrop().into(holder.imgProfile);
            } else {
                holder.imgProfile.setImageResource(R.drawable.ic_person);
            }

            // --- LOGICA DE BUTOANE IN FUNCTIE DE TAB ---
            if (listType.equals("REQUESTS")) {
                // Suntem in tab-ul de cereri: Afisam Accept si Decline
                holder.btnPrimary.setText("Accept");
                holder.btnSecondary.setVisibility(View.VISIBLE);

                // Click pe Accept
                holder.btnPrimary.setOnClickListener(v -> {
                    firebaseHelper.acceptFriendRequest(currentUserId, user.getUid(), new FirebaseHelper.FirestoreCallback<Void>() {
                        @Override public void onCallback(Void data) {
                            Toast.makeText(CommunityActivity.this, "Request Accepted!", Toast.LENGTH_SHORT).show();
                            loadRequests(); // Reincarca lista
                            loadFriends();  // Adauga in background la prieteni
                        }
                    });
                });

                // Click pe Decline
                holder.btnSecondary.setOnClickListener(v -> {
                    firebaseHelper.removeFriendOrRequest(currentUserId, user.getUid(), new FirebaseHelper.FirestoreCallback<Void>() {
                        @Override public void onCallback(Void data) {
                            Toast.makeText(CommunityActivity.this, "Request Declined", Toast.LENGTH_SHORT).show();
                            loadRequests(); // Reincarca lista
                        }
                    });
                });

            } else {
                // Suntem in Friends sau Discover: Afisam doar "View Profile"
                holder.btnPrimary.setText("View Profile");
                holder.btnSecondary.setVisibility(View.GONE);

                holder.btnPrimary.setOnClickListener(v -> {
                    Intent intent = new Intent(CommunityActivity.this, PublicProfileActivity.class);
                    intent.putExtra("TARGET_UID", user.getUid());
                    startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() { return usersList.size(); }

        class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView imgProfile;
            TextView tvUsername;
            Button btnPrimary, btnSecondary;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                imgProfile = itemView.findViewById(R.id.searchProfileImage);
                tvUsername = itemView.findViewById(R.id.searchUsername);
                btnPrimary = itemView.findViewById(R.id.btnPrimary);
                btnSecondary = itemView.findViewById(R.id.btnSecondary);
            }
        }
    }
}