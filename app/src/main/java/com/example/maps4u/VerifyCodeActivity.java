package com.example.maps4u;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VerifyCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        Button btnVerify = findViewById(R.id.btn_verify_code);

        btnVerify.setText("I have reset my password");

        Toast.makeText(this, "Please check your email and click the reset link provided by Google.", Toast.LENGTH_LONG).show();

        btnVerify.setOnClickListener(v -> {
            Intent intent = new Intent(VerifyCodeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}