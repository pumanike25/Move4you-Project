package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        EditText usernameInput = findViewById(R.id.register_username);
        EditText emailInput = findViewById(R.id.register_email);
        EditText passwordInput = findViewById(R.id.register_password);
        EditText confirmPasswordInput = findViewById(R.id.register_confirm_password);
        Button registerButton = findViewById(R.id.register_button);

        registerButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPass = confirmPasswordInput.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                String uid = firebaseUser.getUid();
                                String defaultImage = "https://firebasestorage.googleapis.com/v0/b/maps4u-d0355.firebasestorage.app/o/profile_images%2Fguest.jpg?alt=media&token=1564162d-9bbf-4b02-87d6-b473cef4ce30";

                                User newUser = new User(username, email, "user", defaultImage);

                                firebaseHelper.saveUser(uid, newUser, new FirebaseHelper.FirestoreCallback<Void>() {
                                    @Override
                                    public void onCallback(Void data) {
                                        Toast.makeText(RegisterActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            }
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}