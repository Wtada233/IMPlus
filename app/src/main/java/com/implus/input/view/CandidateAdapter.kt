package com.implus.input.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CandidateAdapter(private val onSelect: (String) -> Unit) : 
    RecyclerView.Adapter<CandidateAdapter.ViewHolder>() {

    private var candidates: List<String> = emptyList()

    fun setCandidates(list: List<String>) {
        candidates = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tv = TextView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            gravity = android.view.Gravity.CENTER
            setPadding(32, 0, 32, 0)
            textSize = 18f
        }
        return ViewHolder(tv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text = candidates[position]
        (holder.itemView as TextView).text = text
        holder.itemView.setOnClickListener { onSelect(text) }
    }

    override fun getItemCount(): Int = candidates.size

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
