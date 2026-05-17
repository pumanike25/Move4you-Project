package com.example.maps4u;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private ImageView btnSendMessage, btnMeetup;
    private TextView tvChatTitle;

    private ChatAdapter adapter;
    private List<ChatMessage> messageList;
    private FirebaseHelper firebaseHelper;
    private String currentUserId;
    private String targetUserId;
    private String targetUserName;
    private ListenerRegistration messageListener;
    private com.google.android.gms.maps.model.LatLng selectedMapPoint = null;

    private String targetPublicKeyBase64 = null;
    private String myPublicKeyBase64 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        firebaseHelper = new FirebaseHelper();
        currentUserId = firebaseHelper.getCurrentUserId();
        targetUserId = getIntent().getStringExtra("TARGET_USER_ID");
        targetUserName = getIntent().getStringExtra("TARGET_USER_NAME");

        myPublicKeyBase64 = EncryptionHelper.getPublicKeyBase64();
        fetchTargetPublicKey();

        initViews();
        android.view.View mainLayout = findViewById(R.id.mainChatLayout);
        android.view.View chatHeaderLayout = findViewById(R.id.chatHeaderLayout);
        ImageView ivProfilePic = findViewById(R.id.ivProfilePic);

        ivProfilePic.setOnClickListener(v -> finish());

        if (targetUserId != null) {
            firebaseHelper.getUser(targetUserId, new FirebaseHelper.FirestoreCallback<User>() {
                @Override
                public void onCallback(User user) {
                    if (user != null && user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                        com.bumptech.glide.Glide.with(ChatActivity.this)
                                .load(user.getImageUrl())
                                .into(ivProfilePic);
                    }
                }
            });
        }

        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            int bottomSpace = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime() | androidx.core.view.WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottomSpace);

            int standardPadding = (int) (6 * getResources().getDisplayMetrics().density);
            int fixedTopPadding = (int) (16 * getResources().getDisplayMetrics().density);

            chatHeaderLayout.setPadding(standardPadding, fixedTopPadding, standardPadding, standardPadding);

            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        setupRecyclerView();
        loadMessages();

        // CRYPTING
        btnSendMessage.setOnClickListener(v -> {
            String plainText = etMessageInput.getText().toString().trim();
            if (!plainText.isEmpty()) {

                if (targetPublicKeyBase64 == null) {
                    Toast.makeText(ChatActivity.this, "Securing connection... please wait.", Toast.LENGTH_SHORT).show();
                    fetchTargetPublicKey();
                    return;
                }

                java.security.PublicKey targetKey = EncryptionHelper.stringToPublicKey(targetPublicKeyBase64);
                java.security.PublicKey myKey = EncryptionHelper.stringToPublicKey(myPublicKeyBase64);

                if (targetKey != null && myKey != null) {
                    // crypt the mssg with the lock of the friend
                    String encryptedForTarget = EncryptionHelper.encryptMessage(plainText, targetKey);
                    // crypt the mssg with the user's lock
                    String encryptedForMe = EncryptionHelper.encryptMessage(plainText, myKey);

                    // Save to Firebase the secure mssg
                    firebaseHelper.sendEncryptedMessage(currentUserId, targetUserId, encryptedForMe, encryptedForTarget, "text");
                    etMessageInput.setText("");
                } else {
                    Toast.makeText(ChatActivity.this, "Encryption failed!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMeetup.setOnClickListener(v -> showCreateMeetupDialog());
    }

    // the handshake
    private void fetchTargetPublicKey() {
        if (targetUserId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users").document(targetUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("publicKey")) {
                            targetPublicKeyBase64 = documentSnapshot.getString("publicKey");
                        }
                    });
        }
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        btnMeetup = findViewById(R.id.btnMeetup);
        tvChatTitle = findViewById(R.id.tvChatTitle);

        tvChatTitle.setText(targetUserName != null ? targetUserName : "Chat");
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(this, messageList, currentUserId, new ChatAdapter.OnMeetupActionListener() {
            @Override
            public void onAccept(ChatMessage meetupMsg) {
                String roomId = firebaseHelper.getChatRoomId(currentUserId, targetUserId);
                firebaseHelper.updateMeetupStatus(roomId, meetupMsg.getId(), "ACCEPTED", new FirebaseHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onCallback(Void data) {
                        Toast.makeText(ChatActivity.this, "The meeting has been set up!", Toast.LENGTH_SHORT).show();

                        meetupMsg.setMeetupStatus("ACCEPTED");
                        firebaseHelper.saveAcceptedMeetup(currentUserId, meetupMsg);
                        firebaseHelper.saveAcceptedMeetup(targetUserId, meetupMsg);
                    }
                });
            }
            @Override
            public void onDecline(ChatMessage meetupMsg) {
                String roomId = firebaseHelper.getChatRoomId(currentUserId, targetUserId);
                firebaseHelper.updateMeetupStatus(roomId, meetupMsg.getId(), "DECLINED", new FirebaseHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onCallback(Void data) {
                        Toast.makeText(ChatActivity.this, "You have declined the meeting.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && adapter.getItemCount() > 0) {
                rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1), 100);
            }
        });
    }

    private void loadMessages() {
        if (currentUserId != null && targetUserId != null) {
            messageListener = firebaseHelper.listenForMessages(currentUserId, targetUserId, new FirebaseHelper.FirestoreCallback<List<ChatMessage>>() {
                @Override
                public void onCallback(List<ChatMessage> data) {
                    messageList.clear();

                    ChatMessage systemMsg = new ChatMessage();
                    systemMsg.setSystemMessage(true);
                    systemMsg.setMessageText("🔒 Messages to this chat and calls are now secured with end-to-end encryption.");
                    messageList.add(systemMsg);

                    messageList.addAll(data);
                    adapter.notifyDataSetChanged();

                    if (!messageList.isEmpty()) {
                        rvMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                }
            });
        }
    }

    private void showCreateMeetupDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_create_meetup, null);
        dialog.setContentView(view);

        android.widget.EditText etName = view.findViewById(R.id.etMeetupName);
        android.widget.EditText etDesc = view.findViewById(R.id.etMeetupDesc);
        android.widget.EditText etAddress = view.findViewById(R.id.etMeetupAddress);
        android.widget.TextView tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        android.widget.TextView tvSelectedTime = view.findViewById(R.id.tvSelectedTime);

        com.google.android.gms.maps.MapView mapView = view.findViewById(R.id.mapViewMeetup);
        mapView.onCreate(null);
        mapView.onResume();
        android.widget.ImageView btnToggleFullscreen = view.findViewById(R.id.btnToggleFullscreen);
        android.widget.LinearLayout llInputs = view.findViewById(R.id.MeetupInputs);
        android.widget.Button btnSend = view.findViewById(R.id.btnSendMeetup);
        final boolean[] isMapFullscreen = {false};

        btnToggleFullscreen.setOnClickListener(v -> {
            isMapFullscreen[0] = !isMapFullscreen[0];
            android.view.ViewGroup.LayoutParams params = mapView.getLayoutParams();

            if (isMapFullscreen[0]) {
                llInputs.setVisibility(android.view.View.GONE);
                btnSend.setVisibility(android.view.View.GONE);

                int displayHeight = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;
                params.height = (int) (displayHeight * 0.7);
                mapView.setLayoutParams(params);

                btnToggleFullscreen.setImageResource(android.R.drawable.ic_menu_revert);
            } else {
                llInputs.setVisibility(android.view.View.VISIBLE);
                btnSend.setVisibility(android.view.View.VISIBLE);

                int originalHeight = (int) (200 * getResources().getDisplayMetrics().density);
                params.height = originalHeight;
                mapView.setLayoutParams(params);

                btnToggleFullscreen.setImageResource(android.R.drawable.ic_menu_zoom);
            }
        });

        selectedMapPoint = null;

        mapView.getMapAsync(googleMap -> {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);

                com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        com.google.android.gms.maps.model.LatLng currentLatLng = new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude());
                        googleMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                    }
                });
            } else {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            }

            googleMap.setOnMapClickListener(latLng -> {
                googleMap.clear();
                googleMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions().position(latLng).title("Meetup Point"));
                selectedMapPoint = latLng;

                etAddress.setText("");
                etAddress.setHint("Location picked on map!");
            });
        });

        final java.util.Calendar calendar = java.util.Calendar.getInstance();

        view.findViewById(R.id.btnPickDate).setOnClickListener(v -> {
            new android.app.DatePickerDialog(this, (view1, y, m, d) -> {
                calendar.set(y, m, d);
                tvSelectedDate.setText(new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(calendar.getTime()));
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
        });

        view.findViewById(R.id.btnPickTime).setOnClickListener(v -> {
            new android.app.TimePickerDialog(this, (view12, h, min) -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, h);
                calendar.set(java.util.Calendar.MINUTE, min);
                tvSelectedTime.setText(new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(calendar.getTime()));
            }, calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE), true).show();
        });

        view.findViewById(R.id.btnSendMeetup).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String addressText = etAddress.getText().toString().trim();

            if (name.isEmpty()) {
                android.widget.Toast.makeText(this, "The name of the meeting is mandatory!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (tvSelectedDate.getText().toString().equals("Select Date") || tvSelectedTime.getText().toString().equals("Select Time")) {
                android.widget.Toast.makeText(this, "Select the time and date!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (addressText.isEmpty() && selectedMapPoint == null) {
                android.widget.Toast.makeText(this, "Enter an address or select it on the map!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            double lat = selectedMapPoint != null ? selectedMapPoint.latitude : 0.0;
            double lng = selectedMapPoint != null ? selectedMapPoint.longitude : 0.0;
            String finalAddress = !addressText.isEmpty() ? addressText : "Map Location Selected";

            firebaseHelper.sendMeetupRequest(currentUserId, targetUserId, name, desc, finalAddress, lat, lng, calendar.getTimeInMillis());

            android.widget.Toast.makeText(this, "Invite sent!", android.widget.Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}