package com.example.downloadfile.client

import android.telecom.Call
import com.example.downloadfile.model.Photo
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.GET

interface API {

    @GET("photos")
    fun getListPhoto() : Observable<List<Photo>>

    @GET("photos")
    fun getListPhotoVer2(): retrofit2.Call<List<Photo>>
}