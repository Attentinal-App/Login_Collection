package com.kmou.cslogin

import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.kmou.cslogin.databinding.ActivityPermissionsRationaleBinding

class PermissionsRationaleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsRationaleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = ActivityPermissionsRationaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // "권한 허용" 버튼 클릭 시 권한 요청 진행
        binding.confirmButton.setOnClickListener {
            Log.d("PermissionsRationale", "사용자가 권한 허용을 선택했습니다.")
            setResult(RESULT_OK)  // 결과를 OK로 설정
            finish()  // Activity 종료
        }

        // "취소" 버튼 클릭 시 Activity 종료
        binding.cancelButton.setOnClickListener {
            Log.d("PermissionsRationale", "사용자가 권한 요청을 취소했습니다.")
            setResult(RESULT_CANCELED)  // 결과를 CANCELED로 설정
            finish()  // Activity 종료
        }
    }
}
