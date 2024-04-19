package com.example.aktibo

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MomentsItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val userProfileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
    val momentImg: ImageView = itemView.findViewById(R.id.momentImg)
    val username: TextView = itemView.findViewById(R.id.username)
    val momentCaption: TextView = itemView.findViewById(R.id.momentCaption)
}