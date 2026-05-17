package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private FirebaseHelper firebaseHelper;
    private ProgressBar progressBar;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        firebaseHelper = new FirebaseHelper();

        recyclerView = findViewById(R.id.history_recycler_view);
        progressBar = new ProgressBar(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new HistoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);


        loadHistoryFromFirebase();
    }

    private void loadHistoryFromFirebase() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUid != null) {
            firebaseHelper.getUserHistory(currentUid, new FirebaseHelper.FirestoreCallback<QuerySnapshot>() {
                @Override
                public void onCallback(QuerySnapshot data) {
                    if (data != null && !data.isEmpty()) {
                        // converting the data from firebase into obj
                        List<RouteHistory> historyList = data.toObjects(RouteHistory.class);

                        adapter.updateData(historyList);
                    } else {
                        Toast.makeText(HistoryActivity.this, "No history found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(HistoryActivity.this, "Error loading history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }
}