package com.dicoding.asclepius.retrofit

import com.dicoding.asclepius.response.NewsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("top-headlines")
    fun getHealthNews(
        @Query("country") country: String = "id",
        @Query("category") category: String = "health",
        @Query("q") query: String = "cancer",
        @Query("language") language: String = "en",
        @Query("apiKey") apiKey: String
    ): Call<NewsResponse>
}