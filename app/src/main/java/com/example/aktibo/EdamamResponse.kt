package com.example.aktibo

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class EdamamResponse(
    @SerializedName("text") val text: String,
    @SerializedName("parsed") val parsed: List<ParsedItem>
)

data class ParsedItem(
    @SerializedName("food") val food: Food,
    @SerializedName("quantity") val quantity: Float,
    @SerializedName("measure") val measure: Measure?
)

data class Food(
    @SerializedName("foodId") val foodId: String,
    @SerializedName("label") val label: String,
    @SerializedName("knownAs") val knownAs: String?,
    @SerializedName("nutrients") val nutrients: Nutrients,
    @SerializedName("category") val category: String,
    @SerializedName("categoryLabel") val categoryLabel: String,
    @SerializedName("image") val image: String?
)

data class Nutrients(
    @SerializedName("ENERC_KCAL") val ENERC_KCAL: Double?,
    @SerializedName("PROCNT") val PROCNT: Double?,
    @SerializedName("FAT") val FAT: Double?,
    @SerializedName("CHOCDF") val CHOCDF: Double?,
    @SerializedName("FIBTG") val FIBTG: Double?,
)

data class Measure(
    @SerializedName("uri") val uri: String,
    @SerializedName("label") val label: String,
    @SerializedName("weight") val weight: Float
)