package com.kmou.cslogin;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.Manifest;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.FirebaseException;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput, passwordConfirmInput, phoneInput, verificationCodeInput;
    private Button emailCheckButton, sendVerificationButton, verificationConfirmButton, nextButton;
    private CheckBox checkboxAge, checkboxTerms;
    private TextView loginClickableText;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String verificationId;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeUI();

        // "로그인" 텍스트 클릭 시 LoginActivity로 이동
        loginClickableText = findViewById(R.id.loginClickableText);
        loginClickableText.setOnClickListener(v -> {
            finish();
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailCheckButton.setOnClickListener(v -> checkEmailExists());
        sendVerificationButton.setOnClickListener(v -> sendVerificationCode());
        verificationConfirmButton.setOnClickListener(v -> verifyCode());

        // 다음 버튼 클릭 리스너
        nextButton.setOnClickListener(v -> {
            if (validateInputs()) {
                // 모든 입력이 올바르면 다음 단계로 이동
                Toast.makeText(this, "회원가입 완료", Toast.LENGTH_SHORT).show();
                // 다음 화면으로 이동하는 Intent 코드 추가 가능
            }
        });
    }

    // UI 요소 초기화 메서드
    private void initializeUI() {
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        passwordConfirmInput = findViewById(R.id.passwordConfirmInput);
        phoneInput = findViewById(R.id.phoneInput);
        verificationCodeInput = findViewById(R.id.verificationCodeInput);

        emailCheckButton = findViewById(R.id.emailCheck);
        sendVerificationButton = findViewById(R.id.btnSend);
        verificationConfirmButton = findViewById(R.id.verifyButton);
        nextButton = findViewById(R.id.nextButton);

        checkboxAge = findViewById(R.id.checkboxAge);
        checkboxTerms = findViewById(R.id.checkboxTerms);
    }

    // 이메일 중복 확인 메서드
    private void checkEmailExists() {
        String email = emailInput.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "이메일을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Toast.makeText(this, "이미 등록된 이메일입니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "사용 가능한 이메일입니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 인증번호 전송 메서드
    private void sendVerificationCode() {
        String phoneNumber = phoneInput.getText().toString().trim();

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "휴대전화 번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // '010' 형식의 번호를 E.164 형식으로 변환
        if (phoneNumber.startsWith("010")) {
            phoneNumber = phoneNumber.replace("-", ""); // '-' 제거
            phoneNumber = "+82" + phoneNumber.substring(1); // '010' -> '+8210'
        } else {
            Toast.makeText(this, "유효한 010 번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        Toast.makeText(RegisterActivity.this, "자동 인증 완료", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(RegisterActivity.this, "인증 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = s;
                        Toast.makeText(RegisterActivity.this, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // 인증번호 검증 메서드
    private void verifyCode() {
        String code = verificationCodeInput.getText().toString().trim();

        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, "인증번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "전화번호 인증 성공", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "인증 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 입력 검증 메서드
    private boolean validateInputs() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String passwordConfirm = passwordConfirmInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("이름을 입력해주세요");
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("이메일을 입력해주세요");
            return false;
        }

        if (TextUtils.isEmpty(password) || !password.equals(passwordConfirm)) {
            passwordConfirmInput.setError("비밀번호가 일치하지 않습니다");
            return false;
        }

        if (!checkboxAge.isChecked() || !checkboxTerms.isChecked()) {
            Toast.makeText(this, "모든 필수 항목을 체크하세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
