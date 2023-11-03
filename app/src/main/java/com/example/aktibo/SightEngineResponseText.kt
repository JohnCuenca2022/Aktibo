package com.example.aktibo

import com.google.gson.annotations.SerializedName

data class SightEngineResponseText(
    @SerializedName("status") val status: String,
    @SerializedName("request") val request: Request,
    @SerializedName("profanity") val profanity: Profanity
)

data class Profanity(
    @SerializedName("matches") val matches: List<Match>,
)

data class Match(
    @SerializedName("type") val type: String,
    @SerializedName("intensity") val intensity: String,
    @SerializedName("match") val match: String,
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int
)

data class TextModerationRequest(
    val text: String,
    val api_user: String,
    val api_secret: String,
)