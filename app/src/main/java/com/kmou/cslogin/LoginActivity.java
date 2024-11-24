package com.kmou.cslogin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.CheckBox;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.user.UserApiClient;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private GoogleSignInClient googleSignInClient;
    private SharedPreferences sharedPreferences;
    private static final String TAG1 = "FacebookLogin";
    private static final String TAG2 = "GuestLogin";
    private static final String TAG3 = "KakaoLogin";
    private static final String TAG = "GoogleLogin";
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeFirebase();
        initializeSharedPreferences();
        initializeUI();
        setupGoogleSignIn();
        setupKakaoSDK();

        handleAutoLogin();
    }

    // Preferences에 사용자 정보 저장
    private void saveUserInfoToPreferences(FirebaseUser user) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("uid", user.getUid());
        editor.putString("name", user.getDisplayName() != null ? user.getDisplayName() : "No Name");
        editor.putString("email", user.getEmail() != null ? user.getEmail() : "No Email");
        editor.putString("loginType", sharedPreferences.getString("loginType", "Unknown"));
        editor.apply();
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeSharedPreferences() {
        sharedPreferences = getSharedPreferences("autoLogin", MODE_PRIVATE);
    }

    private void initializeUI() {
        TextView findPasswordText = findViewById(R.id.find);
        findPasswordText.setOnClickListener(v -> startActivity(new Intent(this, FindActivity.class)));

        TextView register = findViewById(R.id.register);
        register.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        ImageView googleLoginButton = findViewById(R.id.btn_google);
        googleLoginButton.setOnClickListener(v -> signInWithGoogle());

        ImageView kakaoLoginButton = findViewById(R.id.btn_kakao);
        kakaoLoginButton.setOnClickListener(v -> handleKakaoLogin());
    }

    // 자동 로그인 유형 저장
    private void saveLoginType(String loginType) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("loginType", loginType);
        editor.apply();
    }

    // 자동 로그인 처리 함수
    private void handleAutoLogin() {
        CheckBox autoLoginCheckbox = findViewById(R.id.autoLoginCheckbox);

        // 자동 로그인 체크박스 상태 설정
        boolean isAutoLoginEnabled = sharedPreferences.getBoolean("autoLoginEnabled", false);
        autoLoginCheckbox.setChecked(isAutoLoginEnabled);

        // 자동 로그인 체크박스 상태 변경 시 값 저장
        autoLoginCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("autoLoginEnabled", isChecked);
            editor.apply();
        });

        // 자동 로그인 체크 확인
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String loginType = sharedPreferences.getString("loginType", null);
        if (isAutoLoginEnabled && currentUser != null && loginType != null) {
            navigateToActivity(currentUser, loginType);
        }
    }

    // 구글 로그인 초기화
    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.android_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    // Google 로그인 요청
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // 구글 로그인 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    // Google 계정 정보 확인
                    String idToken = account.getIdToken();
                    Log.d("GoogleLogin", "Google 로그인 성공, ID Token: " + idToken);

                    // Firebase 인증에 Google 계정 사용
                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                    firebaseAuth.signInWithCredential(credential)
                            .addOnCompleteListener(this, authTask -> {
                                if (authTask.isSuccessful()) {
                                    Log.d("GoogleLogin", "Firebase 인증 성공");
                                    saveLoginType("Google");
                                    saveUserInfoToPreferences(firebaseAuth.getCurrentUser());
                                    navigateToActivity(firebaseAuth.getCurrentUser(), "Google");
                                } else {
                                    Log.e("GoogleLogin", "Firebase 인증 실패", authTask.getException());
                                    Toast.makeText(this, "Firebase 인증 실패", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (ApiException e) {
                // ApiException의 StatusCode를 통해 오류 파악
                Log.e("GoogleLogin", "Google 로그인 실패: " + e.getStatusCode() + " - " + e.getMessage());
                Toast.makeText(this, "Google 로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 카카오 로그인 초기화
    private void setupKakaoSDK() {
        String nativeAppKey = getString(R.string.kakao_app_key);
        KakaoSdk.init(this, nativeAppKey);
    }

    // 카카오 로그인 처리
    private void handleKakaoLogin() {
        if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
            UserApiClient.getInstance().loginWithKakaoTalk(this, (token, error) -> {
                if (error == null) {
                    saveLoginType("Kakao");
                    navigateToActivity(firebaseAuth.getCurrentUser(), "Kakao");
                } else {
                    Toast.makeText(this, "카카오톡 로그인 실패", Toast.LENGTH_SHORT).show();
                }
                return null;
            });
        } else {
            UserApiClient.getInstance().loginWithKakaoAccount(this, (token, error) -> {
                if (error == null) {
                    saveLoginType("Kakao");
                    navigateToActivity(firebaseAuth.getCurrentUser(), "Kakao");
                } else {
                    Toast.makeText(this, "카카오 계정 로그인 실패", Toast.LENGTH_SHORT).show();
                }
                return null;
            });
        }
    }

    // MainActivity로 이동
    private void navigateToActivity(FirebaseUser user, String loginType) {
        String uid = user.getUid();
        String email = user.getEmail() != null ? user.getEmail() : "No Email";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "No Name";
        String loginTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());

        // DataActivity로 이동하며 사용자 정보 전달
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("uid", uid);
        intent.putExtra("email", email);
        intent.putExtra("name", displayName);
        intent.putExtra("loginType", loginType);
        intent.putExtra("loginTime", loginTime);

        // 이전 계정으로 로그인되었다는 메시지 전달
        if ("Guest".equals(loginType)) {
            intent.putExtra("showToastMessage", "이전 게스트 계정으로 자동 로그인되었습니다.");
        } else if ("Google".equals(loginType)) {
            intent.putExtra("showToastMessage", "Google 계정으로 로그인되었습니다.");
        } else if ("Kakao".equals(loginType)) {
            intent.putExtra("showToastMessage", "Kakao 계정으로 로그인되었습니다.");
        }

        startActivity(intent);
        finish();
    }

    // 앱 종료 여부 대화상자
    @Override
    public void onBackPressed() {
        // AlertDialog 생성
        new AlertDialog.Builder(this)
                .setTitle("앱 종료")
                .setMessage("앱을 종료하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> {
                    // "예"를 클릭하면 앱 종료
                    finishAffinity(); // 현재 액티비티 및 모든 부모 액티비티 종료
                    System.exit(0);   // 프로세스 종료
                })
                .setNegativeButton("아니오", (dialog, which) -> {
                    // "아니오"를 클릭하면 다이얼로그 닫기
                    dialog.dismiss();
                })
                .setCancelable(false) // 백 버튼으로 다이얼로그 닫기 방지
                .show();
    }
}
