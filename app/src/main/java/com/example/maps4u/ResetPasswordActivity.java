package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ResetPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_reset_password);

        //Button btnConfirm = findViewById(R.id.btn_confirm_reset_password);

        // Acest ecran nu mai este funcțional tehnic deoarece parola se schimbă în browser,
        // dar îl păstrăm pentru structură.
        /*btnConfirm.setOnClickListener(v -> {
            Toast.makeText(this, "Please login with your new password.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ResetPasswordActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });*/
    }
}