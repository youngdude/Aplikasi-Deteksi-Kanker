package com.dicoding.asclepius.response

import com.google.gson.annotations.SerializedName

data class NewsResponse(
	@SerializedName("totalResults") val totalResults: Int,
	@SerializedName("articles") val articles: List<NewsArticle>,
	@SerializedName("status") val status: String
)

data class NewsArticle(
	@SerializedName("title") val title: String,
	@SerializedName("description") val description: String?,
	@SerializedName("urlToImage") val urlToImage: String?,
	@SerializedName("url") val url: String
)
