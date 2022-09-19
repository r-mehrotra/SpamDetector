package com.microsoft.spamdetector

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.spamdetector.databinding.LayoutMessageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessageListAdapter(val list: ArrayList<MessageUIModel>) : RecyclerView.Adapter<BaseVH>() {

    fun updateList(newList: List<MessageUIModel>) {
        CoroutineScope(Dispatchers.Main).launch {
            val diff = withContext(Dispatchers.IO) {
                DiffUtil.calculateDiff(
                    DiffCallback(
                        list,
                        newList
                    )
                )
            }
            list.clear()
            list.addAll(newList)
            diff.dispatchUpdatesTo(this@MessageListAdapter)
            for (item in newList) {
                Log.d("MessageListAdapter", item.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVH {
        val binding =
            LayoutMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageVH(binding)
    }

    override fun onBindViewHolder(holder: BaseVH, position: Int) {
        holder.bindData(position, list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class MessageVH(val binding: LayoutMessageBinding) : BaseVH(binding.root) {
        override fun bindData(position: Int, data: Any) {
            binding.modelData = data as MessageUIModel
        }
    }

    class DiffCallback(val oldList: List<MessageUIModel>, val newList: List<MessageUIModel>) :
        DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].hashCode() == newList[newItemPosition].hashCode()
        }

    }
}