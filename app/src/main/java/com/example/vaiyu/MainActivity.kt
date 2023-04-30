package com.example.vaiyu

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.vaiyu.databinding.ActivityMainBinding
import com.example.vaiyu.ml.AqiQualityModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var img: Bitmap
    val weatherAPIKey: String = "b88a9b8b44ce43cea63a078b761f1574"
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    internal var dialog: BottomSheetDialog? = null
    private var json :String = ""
    lateinit var imageUri: Uri
    private val contract = registerForActivityResult(ActivityResultContracts.TakePicture()){
//        binding.img.setImageURI(imageUri)
        img = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
    }
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)      //Make UI Full Screen
        val windowInsetsController =
            ViewCompat.getWindowInsetsController(window.decorView)

        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())      // Hide the system bars.

        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())      // Show the system bars.
        windowInsetsController?.isAppearanceLightNavigationBars = true
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) //remove night mode
        supportActionBar?.hide()
        val intent = intent
        json = intent.extras?.getString("json").toString()
        Log.d("jsonTest", "onCreate: "+json)
        val json1 = JSONObject(json)
        val amt = json1.getJSONArray("data").getJSONObject(0).getJSONObject("weather").getString("code")

        updateUI(amt.toInt())

        binding.card.setOnClickListener {
            showBottomSheetDialog()
        }

        binding.predictBtn.setOnClickListener {

            if(this::img.isInitialized){
                updateProgress()
                img = Bitmap.createScaledBitmap(img, 256, 256, true)
                Handler().postDelayed({
                    predict(img)
                }, 1100)
            }
            else
                Toast.makeText(this, "Please select an Image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(amt: Int) {
        binding.apply {
            when(amt){

                //Thunderstorm
                in 200..232 -> {
                    weatherImg.setImageResource(R.drawable.ic_storm_weather)
                    mainLayout.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)
                }

                //Drizzle
                in 300..321 -> {
                    weatherImg.setImageResource(R.drawable.ic_few_clouds)

                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.drizzle_bg)
                }

                //Rain
                in 500..531 -> {
                    weatherImg.setImageResource(R.drawable.ic_rainy_weather)

                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.rain_bg)
                }

                //Snow
                in 600..622 -> {
                    weatherImg.setImageResource(R.drawable.ic_snow_weather)

                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.snow_bg)
                }

                //Atmosphere
                in 701..781 -> {
                    weatherImg.setImageResource(R.drawable.ic_broken_clouds)

                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.atmosphere_bg)
                }

                //Clear
                800 -> {
                    weatherImg.setImageResource(R.drawable.ic_clear_day)

                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clear_bg)
                }

                //Clouds
                in 801..804 -> {
                    weatherImg.setImageResource(R.drawable.ic_cloudy_weather)

                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.clouds_bg)
                }

                //unknown
                else->{
                    weatherImg.setImageResource(R.drawable.ic_unknown)

                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity, R.drawable.unknown_bg)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.analyzingTv.visibility = View.GONE
        binding.img.visibility = View.VISIBLE
        binding.progressBar.apply {
            progress = 0f
            progressBarWidth = 5f
            backgroundProgressBarWidth = 7f
        }
    }
    override fun onStart() {
        super.onStart()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()
    }

    private fun updateProgress() {
        binding.progressBar.visibility = View.VISIBLE
        binding.analyzingTv.text = "Analyzing..."
        binding.analyzingTv.visibility = View.VISIBLE
        binding.img.visibility = View.GONE
        binding.progressBar.apply {
            progressMax = 100f
            setProgressWithAnimation(100f, 1000)
            progressBarWidth = 5f
            backgroundProgressBarWidth = 7f
            progressBarColorStart = resources.getColor(R.color.maroon)
            progressBarColorEnd = resources.getColor(R.color.green)
        }
    }

    private fun showBottomSheetDialog() {
        val layoutInflater = this.getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.bottomsheet_dialog, null)
        dialog = BottomSheetDialog(this)
        dialog?.setContentView(view)
        dialog?.window?.findViewById<View>(R.layout.bottomsheet_dialog)
            ?.setBackgroundColor(resources.getColor(android.R.color.transparent))

        var textviewGallery = dialog!!.findViewById<TextView>(R.id.textViewPhoto)
        var textviewCamera = dialog!!.findViewById<TextView>(R.id.textViewCamera)
        var textviewCancel = dialog!!.findViewById<TextView>(R.id.textViewCancel)

        textviewCancel?.setOnClickListener{
            dialog!!.dismiss()
        }

        imageUri = createImageUri()!!

        textviewCamera?.setOnClickListener{
            dialog!!.dismiss()
            contract.launch(imageUri)
        }

        textviewGallery?.setOnClickListener{
            dialog!!.dismiss()
            val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            startActivityForResult(intent, 101)
        }

        dialog?.show()
        val lp = WindowManager.LayoutParams()
        val window = dialog?.window
        lp.copyFrom(window!!.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        window.attributes=lp
    }

    private fun createImageUri(): Uri? {
        val image = File(applicationContext.filesDir, "camera_picture.jpg")
        return FileProvider.getUriForFile(this, "com.example.vaiyu.fileProvider", image)
    }

    private fun predict(img: Bitmap) {
        val model = AqiQualityModel.newInstance(this)    // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(img)
        val byteBuffer = tensorImage.buffer
        inputFeature0.loadBuffer(byteBuffer)

        val outputs = model.process(inputFeature0)          // Runs model inference and gets result.
        val result = outputs.outputFeature0AsTensorBuffer

        model.close()       // Releases model resources if no longer used.


        if(result.floatArray[0] > result.floatArray[1] && result.floatArray[0] > result.floatArray[2])
            binding.analyzingTv.text = "POOR"

        else if(result.floatArray[1] > result.floatArray[0] && result.floatArray[1] > result.floatArray[2])
            binding.analyzingTv.text = "VERY POOR"

        else
            binding.analyzingTv.text = "SEVERE"
    }

    private fun getLocation() {
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

    @SuppressLint("StaticFieldLeak")
    inner class CallWeather(val lat:String, val long:String) : AsyncTask<String, Void, String>(){

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg p0: String?): String? {
            var response: String?
            try {
                response = URL("https://api.weatherbit.io/v2.0/current?lat=" +
                        "${lat}&lon=${long}&key=${weatherAPIKey}&include=minutely\n")
                .readText(Charsets.UTF_8)
            } catch (e: Exception){
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
                Log.d("temp1", "onPostExecute: "+jsonObj.getJSONArray("data").getJSONObject(0).getString("city_name"))
//                binding.tempTv.text = jsonObj.getJSONArray("data").getJSONObject(0).getString("temp")+ resources.getString(R.string.deg)
//                binding.weatherDescTv.text = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject("weather").getString("description")
                val amt = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject("weather").getString("code")

                updateUI(amt.toInt())
                val location = jsonObj.getJSONArray("data").getJSONObject(0).getString("city_name") +
                                ", " + jsonObj.getJSONArray("data").getJSONObject(0).getString("country_code")

//                binding.locationTv.text = location
            } catch (e: Exception){
                // TODO: fill this
            }
        }

        private fun updateUI(amt: Int) {
            binding.apply {
                when(amt){

                    //Thunderstorm
                    in 200..232 -> {
                        weatherImg.setImageResource(R.drawable.ic_storm_weather)
                        mainLayout.background=ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)
                    }

                    //Drizzle
                    in 300..321 -> {
                        weatherImg.setImageResource(R.drawable.ic_few_clouds)

                        mainLayout.background=ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.drizzle_bg)
                    }

                    //Rain
                    in 500..531 -> {
                        weatherImg.setImageResource(R.drawable.ic_rainy_weather)

                        mainLayout.background=ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.rain_bg)
                    }

                    //Snow
                    in 600..622 -> {
                        weatherImg.setImageResource(R.drawable.ic_snow_weather)

                        mainLayout.background=ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.snow_bg)
                    }

                    //Atmosphere
                    in 701..781 -> {
                        weatherImg.setImageResource(R.drawable.ic_broken_clouds)

                        mainLayout.background=ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.atmosphere_bg)
                    }

                    //Clear
                    800 -> {
                        weatherImg.setImageResource(R.drawable.ic_clear_day)

                        mainLayout.background=ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.clear_bg)
                    }

                    //Clouds
                    in 801..804 -> {
                        weatherImg.setImageResource(R.drawable.ic_cloudy_weather)

                        mainLayout.background=ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.clouds_bg)
                    }

                    //unknown
                    else->{
                        weatherImg.setImageResource(R.drawable.ic_unknown)

                        mainLayout.background=ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.unknown_bg)
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {
            try {
                val uri = data?.data!!
                img = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
//                binding.img.setImageURI(uri)
            } catch (e: Exception) {
                Log.e("cam", "onActivityResult: " + e)
            }
        }
    }
}
