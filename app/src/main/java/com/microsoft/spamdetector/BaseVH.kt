package com.microsoft.spamdetector

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseVH(root: View) : RecyclerView.ViewHolder(root) {

    abstract fun bindData(position: Int, data: Any)
}