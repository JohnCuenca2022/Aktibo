package com.example.aktibo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide

class MomentsItemAdapter(
    private val recyclerView: RecyclerView,
    private val mList: MutableList<MomentsItemModel>,
    private val onLoadMoreListener: OnLoadMoreListener? = null
) : RecyclerView.Adapter<MomentsItemViewHolder>() {

    private var isLoading = false

    init {
        setupScrollListener()
    }

    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!isLoading && !recyclerView.canScrollVertically(1)) {
                    onLoadMoreListener?.onLoadMore()
                    isLoading = true
                }
            }
        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MomentsItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.moments_item, parent, false)
        return MomentsItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MomentsItemViewHolder, position: Int) {
        val data = mList[position]
        // Bind your item data here
        // You can use Glide or any other method as you did before
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    fun setLoaded() {
        isLoading = false
    }

    interface OnLoadMoreListener {
        fun onLoadMore()
    }

    data class MomentsItemModel(
        val userImage: String,
        val userName: String,
        val image: String,
        val caption: String){

    }
}