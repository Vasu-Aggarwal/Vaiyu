package com.example.vaiyu.apiCall

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadImage {

    @Multipart
    @POST("/single")
    suspend fun uploadImage(@Part image: MultipartBody.Part): ResponseBody

}