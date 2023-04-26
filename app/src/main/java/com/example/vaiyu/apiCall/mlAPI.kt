package com.example.vaiyu.apiCall
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Log
//import com.android.volley.AuthFailureError
//import com.android.volley.toolbox.JsonObjectRequest
//import com.android.volley.toolbox.Volley
//import com.example.vaiyu.databinding.ActivityMainBinding
//import okhttp3.*
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//import java.io.ByteArrayOutputStream
//import java.io.File
//import java.io.IOException
//import java.lang.reflect.Method
//import kotlin.jvm.Throws
//
//open class mlAPI(val context: Context) {
//
//    fun uploadImageAndGetPrediction(bitmap: Bitmap) {
//        val url = "http://192.168.1.8:8080/"
//
//        val requestQueue = Volley.newRequestQueue(context)
//
//        val byteArrayOutputStream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
//        val imageData = byteArrayOutputStream.toByteArray()
//
//        val requestBody = MultipartBody.Builder()
//            .setType(MultipartBody.FORM)
//            .addFormDataPart("image",
//                "image.jpeg", RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageData))
//            .build()
//
//        val request = Request.Builder()
//            .url(url)
//            .post(requestBody)
//            .build()
//
//        val jsonObjectRequest = JsonObjectRequest(com.android.volley.Request.Method.POST, request.url.toString(), null,
//            { response ->
//                // Handle the JSON response and log the prediction
//                val predictionJson = response.optJSONObject("prediction")
//                val prediction = predictionJson?.optDouble("prediction_value") ?: 0.0
//                Log.d("Prediction", "The predicted value is $prediction")
//            },
//            { error ->
//                // Handle the error here
//                Log.e("Prediction", "Error uploading image: ${error}")
//            }
//        )
//
//        requestQueue.add(jsonObjectRequest)
//    }
//}

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayOutputStream

class mLAPI(private val image: Bitmap) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()
    }

    private val apiEndpoint: String = "http://192.168.1.8:8080/"

    fun uploadImage() {
        val base64String = convertBitmapToBase64(image)
        val request = buildRequest(base64String)
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            // handle error
            Log.d("err", "uploadImage: "+"Error")
            return
        }
        val responseBody = response.body?.string()
        // handle the predicted result
        Log.d("ressss", "uploadImage: "+responseBody)
    }

    private fun buildRequest(base64String: String): Request {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "image.jpeg",
                base64String.toRequestBody("image/jpeg".toMediaType()))
            .build()
        return Request.Builder()
            .url(apiEndpoint)
            .post(requestBody)
            .build()
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}