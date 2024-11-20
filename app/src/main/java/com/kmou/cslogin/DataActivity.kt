package com.kmou.cslogin

import android.annotation.SuppressLint

import android.content.Intent

import android.os.Bundle
import android.os.Handler
import android.os.Looper

import android.util.Log
import java.util.concurrent.TimeUnit

import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Switch
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.health.connect.client.HealthConnectClient

import com.google.firebase.auth.FirebaseAuth


class DataActivity : BaseActivity() {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var dataCollector: DataCollector
    private lateinit var autoDataSwitch: Switch // 스위치 추가
    private var isAutoSending: Boolean = false // 자동 전송 상태 변수

    private lateinit var sleephourInput: EditText
    private lateinit var sleepminuteInput: EditText
    private lateinit var activityInput: EditText
    private lateinit var conc1: CheckBox
    private lateinit var conc2: CheckBox
    private lateinit var sleep1: CheckBox
    private lateinit var sleep2: CheckBox
    private lateinit var sleep3: CheckBox
    private lateinit var drug1: CheckBox
    private lateinit var drug2: CheckBox
    private lateinit var machine1: CheckBox
    private lateinit var machine2: CheckBox
    private lateinit var music1: CheckBox
    private lateinit var music2: CheckBox
    private lateinit var submitButton: ImageView
    private var averageNoiseTextView: TextView? = null
    private var averageLightTextView: TextView? = null
    private var heartRateTextView: TextView? = null
    private var stepCountTextView: TextView? = null
    private lateinit var resetButton: ImageView

    private var name: String = ""
    private var uid: String = ""
    private var email: String = ""
    private var loginType: String = ""
    private var loginTime: String = ""

    private lateinit var firebaseAuth: FirebaseAuth
    private val handler = Handler(Looper.getMainLooper())

    // 사용자 권한 요청 ActivityResultLauncher
    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionHelper.handlePermissionsResult(permissions)
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data)

        firebaseHelper = FirebaseHelper(this)
        firebaseHelper.checkFirestoreConnection()

        healthConnectClient = HealthConnectClient.getOrCreate(this)
        permissionHelper = PermissionHelper(this, healthConnectClient, permissionRequestLauncher)

        // DataCollector 초기화
        dataCollector = DataCollector(this, healthConnectClient, handler)
        startUpdatingUI()

        // BaseActivity의 사이드바 설정
        setupDrawer()
        updateUserInfo(); // 사용자 정보 업데이트

        // Intent로 전달받은 사용자 정보 추출
        uid = intent.getStringExtra("uid") ?: "No UID"
        email = intent.getStringExtra("email") ?: "No Email"
        name = intent.getStringExtra("name") ?: "No Name"
        loginType = intent.getStringExtra("loginType") ?: "No Type"
        loginTime = intent.getStringExtra("loginTime") ?: "No Time"

        initializeUI()

        // 스위치 초기화 및 리스너 추가
        autoDataSwitch = findViewById(R.id.autoDataSwitch)
        autoDataSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startAutoDataTransfer()
            } else {
                stopAutoDataTransfer()
            }
        }

        // 체크박스 리스너 설정
        setupMutuallyExclusiveCheckBoxes(conc1, conc2)
        setupMutuallyExclusiveCheckBoxes(sleep1, sleep2, sleep3)
        setupMutuallyExclusiveCheckBoxes(drug1, drug2)
        setupMutuallyExclusiveCheckBoxes(machine1, machine2)
        setupMutuallyExclusiveCheckBoxes(music1, music2)

        if (permissionHelper.isHealthConnectAvailable()) {
            permissionHelper.checkPermissionsAndRequest()
        } else {
            permissionHelper.showHealthConnectNotInstalledDialog()
        }

        resetButton.setOnClickListener {
            resetInputFields()
        }

        submitButton.setOnClickListener {
            submitData()
        }

        // 시작 시 데이터 업데이트
        dataCollector.startDataUpdate()
    }

    private fun initializeUI() {
        sleephourInput = findViewById(R.id.sleephourInput)
        sleepminuteInput = findViewById(R.id.sleepminuteInput)
        submitButton = findViewById(R.id.submitButton)
        conc1 = findViewById(R.id.conc1)
        conc2 = findViewById(R.id.conc2)
        sleep1 = findViewById(R.id.sleep1)
        sleep2 = findViewById(R.id.sleep2)
        sleep3 = findViewById(R.id.sleep3)
        drug1 = findViewById(R.id.drug1)
        drug2 = findViewById(R.id.drug2)
        machine1 = findViewById(R.id.machine1)
        machine2 = findViewById(R.id.machine2)
        music1 = findViewById(R.id.music1)
        music2 = findViewById(R.id.music2)
        averageLightTextView = findViewById(R.id.averageLightTextView)
        averageNoiseTextView = findViewById(R.id.averageNoiseTextView)
        heartRateTextView = findViewById(R.id.heartRateTextView)
        stepCountTextView = findViewById(R.id.stepCountTextView)
        activityInput = findViewById(R.id.activityInput)
        resetButton = findViewById(R.id.btn_reset)
    }

    private fun resetInputFields() {
        sleephourInput.text.clear()
        sleepminuteInput.text.clear()
        activityInput.text.clear()

        conc1.isChecked = false
        conc2.isChecked = false
        sleep1.isChecked = false
        sleep2.isChecked = false
        sleep3.isChecked = false
        drug1.isChecked = false
        drug2.isChecked = false
        machine1.isChecked = false
        machine2.isChecked = false
        music1.isChecked = false
        music2.isChecked = false

        Toast.makeText(this, "입력 값이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    // 즉시 데이터를 전송하는 메서드
    private fun submitData() {
        try {
            // 데이터 수집
            val activity = activityInput.text.toString().trim()
            val sleephourText = sleephourInput.text.toString().trim()
            val sleepminuteText = sleepminuteInput.text.toString().trim()

            // 입력 값 체크 및 경고 표시
            if (sleephourText.isEmpty()) {
                sleephourInput.error = "수면 시간을 입력해주세요"
                Toast.makeText(this@DataActivity, "수면 시간을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (sleepminuteText.isEmpty()) {
                sleepminuteInput.error = "수면 분을 입력해주세요"
                Toast.makeText(this@DataActivity, "수면 분을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (activity.isEmpty()) {
                activityInput.error = "활동 내용을 입력해주세요"
                Toast.makeText(this@DataActivity, "활동 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (!conc1.isChecked && !conc2.isChecked) {
                Toast.makeText(this@DataActivity, "집중 여부를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (!sleep1.isChecked && !sleep2.isChecked && !sleep3.isChecked) {
                Toast.makeText(this@DataActivity, "낮잠 여부를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (!drug1.isChecked && !drug2.isChecked) {
                Toast.makeText(this@DataActivity, "도핑 여부를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (!machine1.isChecked && !machine2.isChecked) {
                Toast.makeText(this@DataActivity, "전자기기 사용 여부를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            if (!music1.isChecked && !music2.isChecked) {
                Toast.makeText(this@DataActivity, "음악 여부를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return
            }

            val sleephour = sleephourText.toIntOrNull() ?: 0
            val sleepminute = sleepminuteText.toIntOrNull() ?: 0
            val sleeptotal = (sleephour * 60) + sleepminute

            val concStatus = if (conc1.isChecked) 1 else 0
            val sleepStatus = when {
                sleep1.isChecked -> 2
                sleep2.isChecked -> 1
                sleep3.isChecked -> 0
                else -> -1
            }
            val drugStatus = if (drug1.isChecked) 1 else 0
            val machineStatus = if (machine1.isChecked) 1 else 0
            val musicStatus = if (music1.isChecked) 1 else 0

            // Firebase로 데이터 전송
            firebaseHelper.sendData(
                name,
                sleeptotal,
                activity,
                concStatus,
                sleepStatus,
                drugStatus,
                machineStatus,
                musicStatus,
                dataCollector.getAverageLightLevel(), // 조도 데이터
                dataCollector.getAverageNoiseLevel(), // 소음 데이터
                dataCollector.getLatestHeartRate(),
                dataCollector.getTotalSteps()
            )

            Toast.makeText(this, "데이터 저장 완료", Toast.LENGTH_SHORT).show()
            Log.d("DataActivity", "데이터 저장 완료")

        } catch (e: Exception) {
            Log.e("DataActivity", "데이터 저장 중 오류 발생: ${e.message}")
        }
    }

    private fun startAutoDataTransfer() {
        if (isAutoSending) {
            Log.d("DataActivity", "이미 자동 데이터 전송 중입니다.")
            return
        }

        isAutoSending = true
        val dataSendRunnable = object : Runnable {
            override fun run() {
                try {
                    // 데이터 수집
                    val activity = activityInput.text.toString().trim()
                    val sleephourText = sleephourInput.text.toString().trim()
                    val sleepminuteText = sleepminuteInput.text.toString().trim()
                    val sleephour = sleephourText.toIntOrNull() ?: 0
                    val sleepminute = sleepminuteText.toIntOrNull() ?: 0
                    val sleeptotal = (sleephour * 60) + sleepminute

                    val concStatus = if (conc1.isChecked) 1 else 0
                    val sleepStatus = when {
                        sleep1.isChecked -> 2
                        sleep2.isChecked -> 1
                        sleep3.isChecked -> 0
                        else -> -1
                    }
                    val drugStatus = if (drug1.isChecked) 1 else 0
                    val machineStatus = if (machine1.isChecked) 1 else 0
                    val musicStatus = if (music1.isChecked) 1 else 0

                    // Firebase로 데이터 전송
                    firebaseHelper.sendData(
                        name,
                        sleeptotal,
                        activity,
                        concStatus,
                        sleepStatus,
                        drugStatus,
                        machineStatus,
                        musicStatus,
                        dataCollector.getAverageNoiseLevel(), // 평균 소음 데이터 사용
                        dataCollector.getAverageLightLevel(), // 평균 조도 데이터 사용
                        dataCollector.getLatestHeartRate(),
                        dataCollector.getTotalSteps()
                    )

                    Log.d("DataActivity", "자동 데이터 전송 완료")
                } catch (e: Exception) {
                    Log.e("DataActivity", "자동 전송 중 오류 발생: ${e.message}")
                }

                if (isAutoSending) {
                    handler.postDelayed(this, TimeUnit.MINUTES.toMillis(1))
                }
            }
        }

        handler.post(dataSendRunnable)
        Log.d("DataActivity", "자동 데이터 전송 시작")
    }

    private fun stopAutoDataTransfer() {
        isAutoSending = false
        handler.removeCallbacksAndMessages(null)
        Log.d("DataActivity", "자동 데이터 전송 중지")
    }

    private fun collectData(): CollectedData {
        val activity = activityInput.text.toString().trim()
        val sleephour = sleephourInput.text.toString().toIntOrNull() ?: 0
        val sleepminute = sleepminuteInput.text.toString().toIntOrNull() ?: 0
        val sleeptotal = (sleephour * 60) + sleepminute

        val concStatus = if (conc1.isChecked) 1 else 0
        val sleepStatus = when {
            sleep1.isChecked -> 2
            sleep2.isChecked -> 1
            sleep3.isChecked -> 0
            else -> -1
        }
        val drugStatus = if (drug1.isChecked) 1 else 0
        val machineStatus = if (machine1.isChecked) 1 else 0
        val musicStatus = if (music1.isChecked) 1 else 0

        return CollectedData(
            name = name,
            sleeptotal = sleeptotal,
            activity = activity,
            concStatus = concStatus,
            sleepStatus = sleepStatus,
            drugStatus = drugStatus,
            machineStatus = machineStatus,
            musicStatus = musicStatus,
            avgNoiseLevel = dataCollector.getAverageNoiseLevel(),
            avgLightLevel = dataCollector.getAverageLightLevel(),
            latestHeartRate = dataCollector.getLatestHeartRate(),
            totalSteps = dataCollector.getTotalSteps()
        )
    }

    data class CollectedData(
        val name: String,
        val sleeptotal: Int,
        val activity: String,
        val concStatus: Int,
        val sleepStatus: Int,
        val drugStatus: Int,
        val machineStatus: Int,
        val musicStatus: Int,
        val avgNoiseLevel: Float,
        val avgLightLevel: Float,
        val latestHeartRate: Int,
        val totalSteps: Int
    )

    // 상호 배타적 체크박스 설정 메서드
    private fun setupMutuallyExclusiveCheckBoxes(vararg checkBoxes: CheckBox) {
        for (checkBox in checkBoxes) {
            checkBox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    for (otherCheckBox in checkBoxes) {
                        if (otherCheckBox !== checkBox) {
                            otherCheckBox.isChecked = false // 다른 체크박스 해제
                        }
                    }
                }
            }
        }
    }

    // 주기적으로 UI 업데이트
    private fun startUpdatingUI() {
        handler.post(object : Runnable {
            override fun run() {
                runOnUiThread {
                    // 소수점 2자리까지 포맷팅하여 출력
                    val formattedNoiseLevel = String.format("%.2f", dataCollector.getAverageNoiseLevel())
                    val formattedLightLevel = String.format("%.2f", dataCollector.getAverageLightLevel())

                    averageNoiseTextView?.text = "평균 소음: $formattedNoiseLevel dB"
                    averageLightTextView?.text = "평균 조도: $formattedLightLevel lux"
                    heartRateTextView?.text = "심박수: ${dataCollector.getLatestHeartRate()} bpm"
                    stepCountTextView?.text = "걸음수: ${dataCollector.getTotalSteps()} 걸음"

                    Log.d("UIUpdate", """
                    평균 소음: ${dataCollector.getAverageNoiseLevel()} dB
                    평균 조도: ${dataCollector.getAverageLightLevel()} lux
                """.trimIndent())
                }
                handler.postDelayed(this, TimeUnit.SECONDS.toMillis(5)) // 5초 간격으로 업데이트
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseHelper.stopAutoSendData()
        dataCollector.onDestroy()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("앱 종료")
            .setMessage("앱을 종료하시겠습니까?")
            .setPositiveButton("예") { _, _ -> finish() }
            .setNegativeButton("아니오") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }
}
