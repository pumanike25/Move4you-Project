package com.example.maps4u;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView rvChats;
    private ChatListAdapter adapter;
    private List<User> friendsList;
    private FirebaseHelper firebaseHelper;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();

        rvChats = findViewById(R.id.rvChats);
        rvChats.setLayoutManager(new LinearLayoutManager(this));

        friendsList = new ArrayList<>();
        adapter = new ChatListAdapter(this, friendsList);
        rvChats.setAdapter(adapter);

        loadFriends();
    }

    private void loadFriends() {
        if (currentUserId != null) {
            firebaseHelper.getFriendsList(currentUserId, new FirebaseHelper.FirestoreCallback<List<User>>() {
                @Override
                public void onCallback(List<User> friends) {
                    friendsList.clear();
                    friendsList.addAll(friends);
                    adapter.notifyDataSetChanged();

                    if (friends.isEmpty()) {
                        Toast.makeText(ChatListActivity.this, "No friends found! Add your friends from the community tab!", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ChatListActivity.this, "Error loading the list.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
