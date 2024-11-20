package com.kmou.cslogin

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PermissionHelper(
    private val activity: Activity,
    private val healthConnectClient: HealthConnectClient,
    private val permissionRequestLauncher: ActivityResultLauncher<Array<String>>
) {

    // Health Connect 설치 여부 체크
    fun isHealthConnectAvailable(): Boolean {
        return try {
            activity.packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Health Connect 및 마이크 권한 요청
    fun checkPermissionsAndRequest() {
        CoroutineScope(Dispatchers.Main).launch {
            val requiredHealthPermissions = setOf(
                HealthPermission.getReadPermission(HeartRateRecord::class),
                HealthPermission.getWritePermission(HeartRateRecord::class),
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getWritePermission(StepsRecord::class)
            )

            val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()

            // Health Connect 권한이 필요한 경우 요청
            if (!grantedPermissions.containsAll(requiredHealthPermissions)) {
                permissionRequestLauncher.launch(
                    arrayOf(
                        "android.permission.health.READ_HEART_RATE",
                        "android.permission.health.WRITE_HEART_RATE",
                        "android.permission.health.READ_STEPS",
                        "android.permission.health.WRITE_STEPS"
                    )
                )
            } else {
                // Health Connect 권한이 이미 허용된 경우 마이크 권한 요청
                requestMicrophonePermission()
            }
        }
    }

    // 권한 결과 처리 메서드
    fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val healthPermissionsGranted = permissions.filterKeys {
            it.startsWith("android.permission.health")
        }.values.all { it }

        val microphonePermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        // 헬스 커넥트 권한이 허용되면 마이크 권한 요청
        if (healthPermissionsGranted && !microphonePermissionGranted) {
            requestMicrophonePermission()
        } else if (microphonePermissionGranted) {
            Log.d("PermissionHelper", "모든 권한이 허용되었습니다.")
            Toast.makeText(activity, "모든 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Log.e("PermissionHelper", "권한이 거부되었습니다.")
            showPermissionDeniedDialog()
        }
    }

    // 마이크 권한 요청
    fun requestMicrophonePermission() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionRequestLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    // 권한 거부 시 다이얼로그 표시
    fun showPermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("권한 필요")
            .setMessage("앱을 사용하려면 필요한 권한을 허용해 주세요. 권한이 허용되지 않으면 앱이 종료됩니다.")
            .setPositiveButton("설정으로 이동") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", activity.packageName, null)
                activity.startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("종료") { dialog, _ ->
                dialog.dismiss()
                activity.finishAffinity()
            }
            .setCancelable(false)
            .show()
    }

    // Health Connect 설치 안내 다이얼로그
    fun showHealthConnectNotInstalledDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Health Connect 필요")
            .setMessage("이 앱을 사용하려면 Google의 Health Connect 앱이 필요합니다. Google Play 스토어에서 설치해 주세요.")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
                activity.finish()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        const val REQUEST_PERMISSION_RATIONALE = 1001
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}
