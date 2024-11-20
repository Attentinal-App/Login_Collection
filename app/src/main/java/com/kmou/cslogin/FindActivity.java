package com.kmou.cslogin;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.widget.ImageView;

import java.util.Random;

public class FindActivity extends AppCompatActivity {

    private EditText phoneInput, verificationCodeInput, idInput, phoneInputPassword, verificationCodeInputPassword;
    private Button sendCodeButton, verifyButton, findIdButton, sendCodeButtonPassword, verifyButtonPassword, resetPasswordButton;
    private ImageView backButton;
    private String checkNum;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private static final String TAG = "FindActivity";
    private boolean isIdTab = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        pref = getPreferences(Context.MODE_PRIVATE);
        editor = pref.edit();

        backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish());

        // TabHost 초기화
        TabHost tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        // 아이디 찾기 탭 설정
        TabHost.TabSpec tabSpec1 = tabHost.newTabSpec("FindId").setIndicator("아이디 찾기");
        tabSpec1.setContent(R.id.tabFindId);
        tabHost.addTab(tabSpec1);

        // 비밀번호 찾기 탭 설정
        TabHost.TabSpec tabSpec2 = tabHost.newTabSpec("FindPassword").setIndicator("비밀번호 찾기");
        tabSpec2.setContent(R.id.tabFindPassword);
        tabHost.addTab(tabSpec2);

        // 탭이 변경될 때마다 플래그 업데이트
        tabHost.setOnTabChangedListener(tabId -> isIdTab = tabId.equals("FindId"));

        // 아이디 찾기 UI 요소
        phoneInput = findViewById(R.id.phoneInput);
        verificationCodeInput = findViewById(R.id.verificationCodeInput);
        sendCodeButton = findViewById(R.id.sendCodeButton);
        verifyButton = findViewById(R.id.verifyButton);
        findIdButton = findViewById(R.id.findIdButton);

        sendCodeButton.setOnClickListener(v -> requestSmsPermission(true));
        verifyButton.setOnClickListener(v -> verifyCode(verificationCodeInput));
        findIdButton.setOnClickListener(v -> {
            if (isCodeVerified(verificationCodeInput)) {
                Toast.makeText(FindActivity.this, "사용자님의 아이디는 ? 입니다.", Toast.LENGTH_LONG).show();
            }
        });

        // 비밀번호 찾기 UI 요소
        idInput = findViewById(R.id.idInput);
        phoneInputPassword = findViewById(R.id.phoneInputPassword);
        verificationCodeInputPassword = findViewById(R.id.verificationCodeInputPassword);
        sendCodeButtonPassword = findViewById(R.id.sendCodeButtonPassword);
        verifyButtonPassword = findViewById(R.id.verifyButtonPassword);
        resetPasswordButton = findViewById(R.id.btn_reset);

        sendCodeButtonPassword.setOnClickListener(v -> requestSmsPermission(false));
        verifyButtonPassword.setOnClickListener(v -> verifyCode(verificationCodeInputPassword));
        resetPasswordButton.setOnClickListener(v -> {
            if (isCodeVerified(verificationCodeInputPassword) && !idInput.getText().toString().isEmpty()) {
                Toast.makeText(FindActivity.this, "비밀번호를 재설정합니다.", Toast.LENGTH_SHORT).show();
            } else if (idInput.getText().toString().isEmpty()) {
                Toast.makeText(FindActivity.this, "아이디를 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 인증번호 전송을 위한 공통 메소드
    private void sendVerificationCode(boolean isForId) {
        EditText phoneInputField = isForId ? phoneInput : phoneInputPassword;
        checkNum = generateRandomNumber(6);
        editor.putString("checkNum", checkNum);
        editor.apply();
        sendSMS(phoneInputField.getText().toString(), "인증번호: " + checkNum);
        Toast.makeText(FindActivity.this, "인증번호가 전송되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private void requestSmsPermission(boolean isForId) {
        if (ContextCompat.checkSelfPermission(FindActivity.this, android.Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FindActivity.this, new String[]{android.Manifest.permission.SEND_SMS}, isForId ? 1 : 2);
        } else {
            sendVerificationCode(isForId);
        }
    }

    private boolean isCodeVerified(EditText verificationCodeInputField) {
        String enteredCode = verificationCodeInputField.getText().toString();
        String savedCode = pref.getString("checkNum", "");
        return savedCode.equals(enteredCode);
    }

    private void verifyCode(EditText verificationCodeInputField) {
        if (isCodeVerified(verificationCodeInputField)) {
            Toast.makeText(FindActivity.this, "인증 성공!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(FindActivity.this, "인증번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String generateRandomNumber(int len) {
        Random rand = new Random();
        StringBuilder numStr = new StringBuilder();
        for (int i = 0; i < len; i++) {
            numStr.append(rand.nextInt(10));
        }
        return numStr.toString();
    }

    private void sendSMS(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Log.d(TAG, "sendSMS: 번호=" + phoneNumber + ", 메시지=" + message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendVerificationCode(requestCode == 1);
        } else {
            Toast.makeText(this, "SMS 전송 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
