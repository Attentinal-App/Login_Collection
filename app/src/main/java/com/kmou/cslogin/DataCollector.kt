package com.kmou.cslogin

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.util.Log
import android.content.Context
import android.hardware.SensorManager

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class DataCollector(
    private val context: Context,
    private val healthConnectClient: HealthConnectClient,
    private val handler: Handler
) : SensorEventListener {

    // 변수 선언
    private var latestHeartRate = 0
    private var totalSteps = 0
    private var currentNoiseLevel = 0.0
    private var currentLightLevel = 0f
    private var isRecording = false
    private var audioRecord: AudioRecord? = null
    private var bufferSize = 0
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null

    val noiseDataList = ArrayList<Double>()
    val lightDataList = ArrayList<Double>()
    var avgNoiseLevel = 0.0f
    var avgLightLevel = 0.0f

    fun getAverageNoiseLevel() = avgNoiseLevel.toFloat()
    fun getAverageLightLevel() = avgLightLevel.toFloat()
    fun getLatestHeartRate() = latestHeartRate
    fun getTotalSteps() = totalSteps

    init {
        // SensorManager 초기화 및 조도 센서 등록
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.e("DataCollector", "조도 센서가 지원되지 않습니다.")
        }

        // 소음 측정 초기화
        setupNoiseMeter()
    }


    // 조도 및 소음 데이터 업데이트
    private fun updateLightAndNoiseData() {
        if (noiseDataList.size < 10) {
            noiseDataList.add(currentNoiseLevel)
        }
        if (lightDataList.size < 10) {
            lightDataList.add(currentLightLevel.toDouble())
        }

        if (noiseDataList.size == 10) {
            calculateAverage(noiseDataList, "Noise")
        }
        if (lightDataList.size == 10) {
            calculateAverage(lightDataList, "Light")
        }
    }

    // 평균 계산
    private fun calculateAverage(dataList: ArrayList<Double>, dataType: String) {
        if (dataList.size < 10) return
        Collections.sort(dataList)

        // 최소값과 최대값 1개씩만 삭제
        dataList.removeAt(0)
        dataList.removeAt(dataList.size - 1)

        val average = (dataList.sum().toFloat() / dataList.size)
        if (dataType == "Noise") {
            avgNoiseLevel = average
            Log.d("DataCollector", "평균 소음: $avgNoiseLevel dB")
        } else if (dataType == "Light") {
            avgLightLevel = average
            Log.d("DataCollector", "평균 조도: $avgLightLevel lux")
        }

        // 리스트 전체를 초기화하지 않고, 일부 데이터만 유지
        while (dataList.size > 10) {
            dataList.removeAt(0)
        }
    }

    // 소음 측정 초기화
    @SuppressLint("MissingPermission")
    fun setupNoiseMeter() {
        bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord!!.state == AudioRecord.STATE_INITIALIZED) {
            isRecording = true
            audioRecord!!.startRecording()
            startNoiseMeasurement()
        } else {
            Log.e("NoiseMeter", "AudioRecord initialization failed")
        }
    }

    // 소음 측정 시작
    private fun startNoiseMeasurement() {
        Thread {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord!!.read(buffer, 0, bufferSize)
                if (read > 0) {
                    var amplitudeSum = 0.0

                    // Signed 16-bit PCM 데이터 처리
                    for (i in 0 until read step 2) {
                        // 샘플 데이터를 Signed 16-bit 정수로 변환
                        val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                        val signedSample = if (sample > 32767) sample - 65536 else sample

                        // 진폭 제곱 합 계산
                        amplitudeSum += (signedSample * signedSample).toDouble()
                    }

                    // 평균 진폭 계산
                    val rmsAmplitude = sqrt(amplitudeSum / (read / 2))

                    // 진폭 검증 및 최소값 설정
                    val amplitude = if (rmsAmplitude <= 1.0) 1.0 else rmsAmplitude

                    // 데시벨 계산 (30 dB ~ 120 dB 범위로 제한)
                    currentNoiseLevel = (20 * log10(amplitude)).coerceIn(30.0, 120.0)
                    currentNoiseLevel = String.format(Locale.US, "%.1f", currentNoiseLevel).toDouble()

                    // 디버깅 로그 출력
                    Log.d("NoiseMeter", "RMS 진폭: $rmsAmplitude, 소음 레벨: $currentNoiseLevel dB")
                }
            }
        }.start()
    }

    fun startDataUpdate() {
        // 심박수 및 걸음수는 30초 간격으로 업데이트
        handler.post(object : Runnable {
            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    readHeartByTimeRange()
                    readStepsByTimeRange()
                }
                handler.postDelayed(this, TimeUnit.SECONDS.toMillis(30))
            }
        })

        // 소음 및 조도 데이터는 5초 간격으로 업데이트
        handler.post(object : Runnable {
            override fun run() {
                updateLightAndNoiseData()
                handler.postDelayed(this, TimeUnit.SECONDS.toMillis(5))
            }
        })
    }

    // 심박수 읽기
    private suspend fun readHeartByTimeRange() {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    HeartRateRecord::class,
                    TimeRangeFilter.between(Instant.now().minusSeconds(86400), Instant.now())
                )
            )
            val heartRate = response.records.lastOrNull()?.samples?.firstOrNull()?.beatsPerMinute
            latestHeartRate = heartRate?.toInt() ?: 0
            Log.d("HealthConnect", "심박수: $latestHeartRate")
        } catch (e: Exception) {
            Log.e("HealthConnect", "심박수 기록 읽기 실패: ${e.message}")
        }
    }

    // 걸음 수 읽기
    private suspend fun readStepsByTimeRange() {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    TimeRangeFilter.between(Instant.now().minusSeconds(86400), Instant.now())
                )
            )
            totalSteps = response.records.sumOf { it.count }.toInt()
            Log.d("HealthConnect", "걸음 수: $totalSteps")
        } catch (e: Exception) {
            Log.e("HealthConnect", "걸음수 기록 읽기 실패: ${e.message}")
        }
    }

    // 센서 변경 이벤트 처리
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LIGHT) {
            currentLightLevel = event.values[0]
            // 메인 스레드에서 UI 업데이트
            handler.post {
                Log.d("DataCollector", "실시간 조도 레벨: $currentLightLevel lux")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 필요시 구현
    }

    // 자원 해제
    fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        sensorManager.unregisterListener(this)

        if (audioRecord != null && isRecording) {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d("AudioRecord", "AudioRecord 리소스 해제됨")
        }
    }
}
