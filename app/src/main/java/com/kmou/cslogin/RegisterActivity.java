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
import android.content.pm.PackageManager;
import android.util.Log;
import android.Manifest;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.Random;
import android.telephony.SmsManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput, emailInput, passwordInput, passwordConfirmInput, phoneInput, verificationCodeInput;
    private Button emailCheckButton, sendVerificationButton, verificationConfirmButton, nextButton;
    private CheckBox checkboxAge, checkboxTerms;
    private String verificationCode = "123456"; // 테스트용 인증번호
    private TextView loginClickableText;
    private String checkNum;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        pref = getSharedPreferences("prefs", MODE_PRIVATE);
        editor = pref.edit();

        initializeUI();

        // "로그인" 텍스트 클릭 시 LoginActivity로 이동
        loginClickableText = findViewById(R.id.loginClickableText);
        loginClickableText.setOnClickListener(v -> {
            finish();
        });

        // 이메일 중복 확인 버튼 클릭 리스너
        emailCheckButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                emailInput.setError("이메일을 입력해주세요");
                Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
            } else {
                // 이메일 중복 확인 로직 (예시)
                Toast.makeText(this, "이메일 중복 확인 완료", Toast.LENGTH_SHORT).show();
            }
        });

        // 인증번호 전송 버튼 클릭 리스너
        sendVerificationButton.setOnClickListener(v -> {
            String phone = phoneInput.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                phoneInput.setError("휴대전화 번호를 입력해주세요");
                Toast.makeText(this, "휴대전화 번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            } else {
                if (ContextCompat.checkSelfPermission(RegisterActivity.this, android.Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    // 권한이 없는 경우 요청
                    ActivityCompat.requestPermissions(RegisterActivity.this, new String[]{Manifest.permission.SEND_SMS}, 1);
                } else {
                    // 권한이 있는 경우 인증번호 전송
                    checkNum = generateRandomNumber(6);
                    editor.putString("checkNum", checkNum);
                    editor.apply(); // 인증번호 저장
                    sendSMS(phoneInput.getText().toString(), "인증번호: " + checkNum);
                    Toast.makeText(RegisterActivity.this, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 인증번호 확인 버튼 클릭 리스너
        verificationConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String enteredCode = verificationCodeInput.getText().toString();
                String savedCode = pref.getString("checkNum", "");

                if (savedCode.equals(enteredCode)) {
                    Toast.makeText(RegisterActivity.this, "인증 성공!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "인증번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

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

    // 입력 검증 메서드
    private boolean validateInputs() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String passwordConfirm = passwordConfirmInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String code = verificationCodeInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("이름을 입력해주세요");
            Toast.makeText(this, "이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("이메일을 입력해주세요");
            Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("비밀번호를 입력해주세요");
            Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(passwordConfirm) || !passwordConfirm.equals(password)) {
            passwordConfirmInput.setError("비밀번호가 일치하지 않습니다");
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("휴대전화 번호를 입력해주세요");
            Toast.makeText(this, "휴대전화 번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

//        if (TextUtils.isEmpty(code) || !code.equals(verificationCode)) {
//            verificationCodeInput.setError("인증번호가 일치하지 않습니다");
//            Toast.makeText(this, "인증번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
//            return false;
//        }

        if (!checkboxAge.isChecked()) {
            Toast.makeText(this, "만 14세 이상임을 확인해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!checkboxTerms.isChecked()) {
            Toast.makeText(this, "서비스 이용약관에 동의해주세요", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // 인증번호 생성 메소드
    private String generateRandomNumber(int len) {
        Random rand = new Random();
        StringBuilder numStr = new StringBuilder();

        for (int i = 0; i < len; i++) {
            numStr.append(rand.nextInt(10)); // 0~9 사이의 랜덤 숫자 생성
        }
        return numStr.toString();
    }

    // SMS 발송 메소드
    private void sendSMS(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Log.d(TAG, "sendSMS: 번호=" + phoneNumber + ", 메시지=" + message);
    }

    // 권한 요청 결과 처리 메소드
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우 SMS 전송
                checkNum = generateRandomNumber(6);
                editor.putString("checkNum", checkNum);
                editor.apply(); // 인증번호 저장
                sendSMS(phoneInput.getText().toString(), "인증번호: " + checkNum);
                Toast.makeText(this, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS 전송 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
