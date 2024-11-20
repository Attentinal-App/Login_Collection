package com.kmou.cslogin

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class FirebaseHelper(private val context: Context) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val handler = Handler()

    private var sendDataRunnable: Runnable? = null
    init {
        // Firestore 설정 초기화
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }

    // Firestore 연결 상태 확인 메서드
    fun checkFirestoreConnection() {
        db.collection("test")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseHelper", "Firestore 연결 성공")
                } else {
                    Log.e("FirebaseHelper", "Firestore 연결 실패: ${task.exception?.message}")
                    Toast.makeText(context, "Firestore 연결 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Firestore로 데이터 전송 메서드
    fun sendData(
        name: String?,
        sleeptotal: Int,
        activity: String?,
        concStatus: Int,
        sleepStatus: Int,
        drugStatus: Int,
        machineStatus: Int,
        musicStatus: Int,
        avgNoiseLevel: Float,
        avgLightLevel: Float,
        latestHeartRate: Int,
        totalSteps: Int
    ) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "사용자가 로그인되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
            Log.e("FirebaseHelper", "사용자 인증 실패")
            return
        }

        val userId = currentUser.uid
        // 현재 시간 (수집 시간)
        val collectTime = System.currentTimeMillis()

        // 시간을 사람이 읽기 쉬운 형식으로 변환 (yyyy-MM-dd HH:mm:ss)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedCollectTime = dateFormat.format(Date(collectTime))

        // Firestore에 저장할 데이터
        val data = hashMapOf(
            "name" to name,
            "sleeptotal" to sleeptotal,
            "activity" to activity,
            "concStatus" to concStatus,
            "sleepStatus" to sleepStatus,
            "drugStatus" to drugStatus,
            "machineStatus" to machineStatus,
            "musicStatus" to musicStatus,
            "lightLevel" to avgLightLevel.toDouble(), // 조도 데이터
            "NoiseLevel" to avgNoiseLevel.toDouble(), // 소음 데이터
            "heartRate" to latestHeartRate,
            "stepCount" to totalSteps,
            "collect_time" to formattedCollectTime
        )

        // 이름이 제공되지 않으면 uid를 사용
        val documentId = if (name?.isNotEmpty() == true && name != "No Name") "${name}_$userId" else userId

        // Firestore에 측정 데이터(collect_data) 저장
        db.collection("user_data")
            .document(documentId)
            .collection("collect_data")
            .document(formattedCollectTime)
            .set(data)
            .addOnSuccessListener {
                Log.d("FirebaseHelper", "데이터 저장 성공 (User ID: $userId)")
                Toast.makeText(context, "데이터 저장 성공", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseHelper", "데이터 저장 실패: ${e.message}")
                Toast.makeText(context, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // 자동 데이터 전송 중지 메서드
    fun stopAutoSendData() {
        if (sendDataRunnable != null) {
            handler.removeCallbacks(sendDataRunnable!!)
            Log.d("FirebaseHelper", "자동 데이터 전송 중지")
        }
    }
}
