package com.example.aktibo

import com.google.gson.annotations.SerializedName

data class SightEngineResponse(
    @SerializedName("status") val status: String,
    @SerializedName("request") val request: Request,
    @SerializedName("summary") val summary: Summary
)

data class Request(
    @SerializedName("id") val id: String,
    @SerializedName("timestamp") val timestamp: Double,
    @SerializedName("operations") val operations: Int
)

data class Summary(
    @SerializedName("action") val action: String,
    @SerializedName("reject_prob") val reject_prob: Double,
    @SerializedName("reject_reason") val reject_reason: List<RejectReason>
)

data class RejectReason(
    @SerializedName("id") val id: String,
    @SerializedName("text") val text: String
)
