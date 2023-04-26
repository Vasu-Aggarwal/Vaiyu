package com.example.vaiyu

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.example.vaiyu.apiCall.UploadImage
import com.example.vaiyu.apiCall.mLAPI
import com.example.vaiyu.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var img: Bitmap
    private lateinit var mlAPIObj: mLAPI
    lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.selectBtn.setOnClickListener {
            val intent = (Intent(Intent.ACTION_GET_CONTENT)).setType("image/*")
            startActivityForResult(intent, 101)
        }

        binding.predictBtn.setOnClickListener {
            img = Bitmap.createScaledBitmap(img, 256, 256, true)
            val fileDir = applicationContext.filesDir
            val file = File(fileDir, "image.jpeg")

            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream!!.copyTo(outputStream)
//            val stream = ByteArrayOutputStream()
//            img.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//            stream.toByteArray()
//            upload()
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    mlAPIObj = mLAPI(img)
                    mlAPIObj.uploadImage()
                }
            }
            catch (e:Exception){
                Log.d("err", "upload: "+e)
            }


//            binding.tv.text = apiRequest.toString()
//            // Creates inputs for reference.
//            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
//            val tensorImage = TensorImage(DataType.FLOAT32)
//            tensorImage.load(img)
//            val byteBuffer = tensorImage.buffer
//            inputFeature0.loadBuffer(byteBuffer)
//
//            // Runs model inference and gets result.
//            val outputs = model.process(inputFeature0)
//            val result = outputs.outputFeature0AsTensorBuffer
//
//            // Releases model resources if no longer used.
//            model.close()

//            if(result.floatArray[0] > result.floatArray[1] && result.floatArray[0] > result.floatArray[2]){
//                binding.tv.text = "POOR"
//            }
//
//            else if(result.floatArray[1] > result.floatArray[0] && result.floatArray[1] > result.floatArray[2]){
//                binding.tv.text = "VERY POOR"
//            }
//
//            else{
//                binding.tv.text = "SEVERE"
//            }
        }
    }

    private fun upload() {
            //Can't access content of gallery while uploading, therefore copy the image to application.
            val fileDir = applicationContext.filesDir
            val file = File(fileDir, "image.jpeg")

            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream!!.copyTo(outputStream)

            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("image", file.name, requestBody)

            val retrofit = Retrofit.Builder().baseUrl("http://192.168.1.8:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(UploadImage::class.java)

            try {
                CoroutineScope(Dispatchers.IO).launch {
                    val response = retrofit.uploadImage(part)
                    Log.d("UploadImage", response.toString())
                }
            }
            catch (e:Exception){
            Log.d("err", "upload: "+e)
        }
    }

//    private fun callApi(img: Bitmap?) {
//        val stream = ByteArrayOutputStream()
//        img?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//        stream.toByteArray()
//
//        val apiRequest = mlAPIObj?.uploadImageAndGetPrediction(stream.toByteArray(),
//            object : mlAPI.PredictionCallback {
//                override fun onPredictionReceived(predictionJson: String) {
//                    predictionJson
//                }
//            })
//        binding.tv.text = apiRequest.toString()
//    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101){
            binding.img.setImageURI(data?.data)
            uri = data?.data!!
            img = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            var lastBitmap: Bitmap? = null
            lastBitmap = img
//            val image: String = getStringImage(lastBitmap)
//            SendImage(image)
        }
    }

}
//        private fun SendImage(image: String) {
//                var stringRequest = StringRequest(
//                Request.Method.POST.toString(), "http://127.0.0.1:5000", new Response.Listener < String >() {
//                    @Override
//                    public void onResponse(String response) {
//                        Log.d("uploade", response);
//                        try {
//                            JSONObject jsonObject = new JSONObject(response);
//
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//        },
//        new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(Edit_Profile.this, "No internet connection", Toast.LENGTH_LONG).show();
//
//            }
//        }) {
//            @Override
//            protected Map<String, String> getParams() throws AuthFailureError {
//
//                Map<String, String> params = new Hashtable<String, String>();
//
//                params.put("image", image);
//                return params;
//
//
//            int socketTimeout = 30000;
//            RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
//            stringRequest.setRetryPolicy(policy);
//            RequestQueue requestQueue = Volley.newRequestQueue(this);
//            requestQueue.add(stringRequest);
//        }

    private fun getStringImage(bmp: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageBytes: ByteArray = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
