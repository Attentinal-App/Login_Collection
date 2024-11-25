package com.kmou.cslogin;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 사용자 정보 가져오기
        String name = getIntent().getStringExtra("name");
        String email = getIntent().getStringExtra("email");

        // UI 업데이트
        TextView profileNameTextView = findViewById(R.id.profileNameTextView);
        TextView profileEmailTextView = findViewById(R.id.profileEmailTextView);

        profileNameTextView.setText(name);
        profileEmailTextView.setText(email);
    }
}
