package com.aystudy.airquality

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aystudy.airquality.databinding.ActivityMainBinding
import com.aystudy.airquality.ui.theme.AirQualityTheme
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var locationProvider: LocationProvider

    private val PERMISSIONS_REQUEST_CODE = 100

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    lateinit var getGPSPermissionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions()
        updateUI()
    }

    private fun updateUI() {
        locationProvider = LocationProvider(this@MainActivity)

        val latitude : Double? = locationProvider.getLocationLatitude()
        val longitude : Double? = locationProvider.getLocationLongitude()

        if (latitude != null && longitude != null) {
            // 1. 현재 위치 가져오고 UI 업데이트
            val address = getCurrentAddress(latitude, longitude)

            address?.let {
                binding.tvLocationTitle.text = "${it.thoroughfare}"
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}"
            }

            // 2. 미세먼지 농도 가져오고 UI 업데이트
        } else {
            Toast.makeText(this, "위도, 경도 정보를 가져올 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getCurrentAddress (latitude : Double, longitude : Double) : Address? {
        val getCoder = Geocoder(this, Locale.KOREA)

        val addresses: List<Address>?

        addresses = try {
            getCoder.getFromLocation(latitude, longitude, 7)
        } catch (ioException : IOException) {
            Toast.makeText(this, "지오코더 서비스를 이용불가 합니다.", Toast.LENGTH_LONG).show()
            return null
        } catch (illegalArgumentException : java.lang.IllegalArgumentException) {
            Toast.makeText(this, "잘못된 위도, 경도 입니다.", Toast.LENGTH_LONG).show()
            return null
        }

        if (addresses == null || addresses.size ==0) {
            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
            return null
        }

        return addresses[0]
    }

    private fun checkAllPermissions() {
        if (!isLocationServicesAvailable()) {
            showDialogForLocationServiceSetting()
        } else {
            isRunTimePermissionsGranted()
        }
    }

    private fun isLocationServicesAvailable() : Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    }

    private fun isRunTimePermissionsGranted() {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (hasFineLocationPermission !=PackageManager.PERMISSION_GRANTED
            || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE &&
            grantResults.size == REQUIRED_PERMISSIONS.size
        ) {
            var checkResult = true

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false
                    break
                }
            }

            if (checkResult) {
                // 위치값을 가지고 올 수 있음
                updateUI()
            } else {
                Toast.makeText(this@MainActivity,
                    "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.",
                    Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun showDialogForLocationServiceSetting() {
        getGPSPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (isLocationServicesAvailable()) {
                    isRunTimePermissionsGranted()
                } else {
                    Toast.makeText(this@MainActivity,
                        "위치 서비스를 사용할 수 없습니다.",
                        Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        val builder : AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("위치 서비스가 꺼져있습니다. 설정해야 앱을 사용할 수 있습니다.")
        builder.setCancelable(true)
        builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialogInterface, i ->
            val callGPSSettingInterface = Intent(Settings.ACTION_LOCALE_SETTINGS)
            getGPSPermissionLauncher.launch(callGPSSettingInterface)
        })
        builder.setNegativeButton("취소", DialogInterface.OnClickListener{ dialogInterface, i ->
            dialogInterface.cancel()
            Toast.makeText(this@MainActivity,
                "위치 서비스를 사용할 수 없습니다.",
                Toast.LENGTH_LONG).show()
            finish()
        })

        builder.create().show()
    }
}

