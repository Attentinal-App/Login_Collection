package com.kmou.cslogin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase Auth 초기화
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser(); // 현재 사용자 정보 초기화

        if (currentUser == null) {
            // 사용자 정보가 없을 경우, 로그인 화면으로 이동
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    // 사이드바 초기화 메서드
    protected void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // 사이드바 열기 버튼 클릭 리스너
        ImageView openDrawerButton = findViewById(R.id.btn_open_drawer);
        if (openDrawerButton != null) {
            openDrawerButton.setOnClickListener(v -> {
                if (!drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.openDrawer(navigationView);
                }
            });
        }

        // 사용자 정보 업데이트
        updateUserInfo();

        // 네비게이션 항목 클릭 리스너 설정
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // 현재 액티비티가 동일한 경우 이동하지 않음
            if ((id == R.id.nav_dashboard && this instanceof MainActivity) ||
                    (id == R.id.nav_data_collection && this instanceof DataActivity)) {
                drawerLayout.closeDrawer(navigationView);
                return true;
            }

            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(BaseActivity.this, MainActivity.class));
            } else if (id == R.id.nav_data_collection) {
                navigateToDataActivity();
            } else if (id == R.id.profileImageView) {
                Toast.makeText(BaseActivity.this, "프로필 클릭", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_settings) {
                Toast.makeText(BaseActivity.this, "설정 클릭", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_analysis) {
                Toast.makeText(BaseActivity.this, "통계 클릭", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BaseActivity.this, AnalysisActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_logout) {
                firebaseAuth.signOut();
                Toast.makeText(BaseActivity.this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BaseActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            drawerLayout.closeDrawer(navigationView);
            return true;
        });
    }

    // 사용자 정보 업데이트 메서드 수정
    protected void updateUserInfo() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String uid = sharedPreferences.getString("uid", "No UID");
        String name = sharedPreferences.getString("name", "No Name");
        String email = sharedPreferences.getString("email", "No Email");
        String loginType = sharedPreferences.getString("loginType", "Unknown");

        // 네비게이션 헤더 뷰 참조
        View headerView = navigationView.getHeaderView(0);
        TextView userNameTextView = headerView.findViewById(R.id.userNameTextView);
        TextView userEmailTextView = headerView.findViewById(R.id.userEmailTextView);
        ImageView profileImageView = headerView.findViewById(R.id.profileImageView);

        // 로그인 유형에 따라 사용자명 또는 UID 출력
        if ("Guest".equals(loginType)) {
            userNameTextView.setText(uid); // 게스트 로그인 시 UID 출력
        } else {
            userNameTextView.setText(name); // 일반 로그인 시 사용자명 출력
        }
        userEmailTextView.setText(email);

        // 프로필 이미지 클릭 리스너 추가
        profileImageView.setOnClickListener(v -> {
            Intent intent = new Intent(BaseActivity.this, ProfileActivity.class);
            intent.putExtra("uid", uid);
            intent.putExtra("name", name);
            intent.putExtra("email", email);
            startActivity(intent);
        });
    }

    // DataActivity로 이동하는 메서드
    private void navigateToDataActivity() {
        if (currentUser != null) {
            Intent intent = new Intent(BaseActivity.this, DataActivity.class);
            intent.putExtra("uid", currentUser.getUid());
            intent.putExtra("name", currentUser.getDisplayName());
            intent.putExtra("email", currentUser.getEmail());
            startActivity(intent);
        }
    }
}
