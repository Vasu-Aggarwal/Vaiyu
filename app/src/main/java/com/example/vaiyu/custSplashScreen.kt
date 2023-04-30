package com.example.vaiyu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.net.URL

@SuppressLint("CustomSplashScreen")
class custSplashScreen : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    val weatherAPIKey: String = "b88a9b8b44ce43cea63a078b761f1574"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        supportActionBar?.hide()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) //remove night mode
    }

    override fun onStart() {
        super.onStart()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getCustomLocation()
    }

    private fun getCustomLocation() {
            //Check location permission
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
                return
            }

            //Get latitude and longitude
            val location = fusedLocationProviderClient.lastLocation
            location.addOnSuccessListener {
                if(it != null){
                    CallWeather(it.latitude.toString(), it.longitude.toString()).execute()
                }
            }

    }

    inner class CallWeather(val lat:String, val long:String) : AsyncTask<String, Void, String>() {

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg p0: String?): String? {
            var response: String?
            try {
                response = URL(
                    "https://api.weatherbit.io/v2.0/current?lat=" +
                            "${lat}&lon=${long}&key=${weatherAPIKey}&include=minutely\n"
                )
                    .readText(Charsets.UTF_8)
            } catch (e: Exception) {
                response = null
            }
            return response
        }

        @SuppressLint("SetTextI18n")
        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                val jsonObj = JSONObject(result.toString())
                Log.d("result123", "onPostExecute: "+jsonObj)
                val intent = Intent(this@custSplashScreen, MainActivity::class.java)
                intent.putExtra("json", jsonObj.toString())
                startActivity(intent)
            } catch (e: Exception) {
                // TODO: fill this
            }
        }
    }
}