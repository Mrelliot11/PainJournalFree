package com.example.paintrackerfree.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.paintrackerfree.data.model.PainEntry
import com.example.paintrackerfree.databinding.ItemDateHeaderBinding
import com.example.paintrackerfree.databinding.ItemPainEntryBinding
import com.example.paintrackerfree.util.DateUtils

class HistoryAdapter(private val onEntryClick: (PainEntry) -> Unit) :
    ListAdapter<HistoryItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ENTRY = 1

        private val DIFF = object : DiffUtil.ItemCallback<HistoryItem>() {
            override fun areItemsTheSame(a: HistoryItem, b: HistoryItem): Boolean = when {
                a is HistoryItem.Header && b is HistoryItem.Header -> a.dateLabel == b.dateLabel
                a is HistoryItem.Entry && b is HistoryItem.Entry -> a.entry.id == b.entry.id
                else -> false
            }
            override fun areContentsTheSame(a: HistoryItem, b: HistoryItem) = a == b
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is HistoryItem.Header -> TYPE_HEADER
        is HistoryItem.Entry -> TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(ItemDateHeaderBinding.inflate(inflater, parent, false))
        } else {
            EntryVH(ItemPainEntryBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryItem.Header -> (holder as HeaderVH).bind(item)
            is HistoryItem.Entry -> (holder as EntryVH).bind(item.entry)
        }
    }

    fun getEntryAt(position: Int): PainEntry? =
        (getItem(position) as? HistoryItem.Entry)?.entry

    class HeaderVH(private val b: ItemDateHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: HistoryItem.Header) { b.tvDateHeader.text = item.dateLabel }
    }

    inner class EntryVH(private val b: ItemPainEntryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(entry: PainEntry) {
            b.tvTime.text = DateUtils.formatTime(entry.timestamp)
            b.tvPainLevel.text = entry.painLevel.toString()
            b.tvPainLevel.setBackgroundResource(painLevelColor(entry.painLevel))
            b.tvLocations.text = entry.locations.ifBlank { "No location" }
            b.tvNotePreview.text = entry.notes.ifBlank { "" }
            b.root.setOnClickListener { onEntryClick(entry) }
        }

        private fun painLevelColor(level: Int) = when {
            level <= 3 -> com.example.paintrackerfree.R.drawable.bg_pain_low
            level <= 6 -> com.example.paintrackerfree.R.drawable.bg_pain_mid
            else -> com.example.paintrackerfree.R.drawable.bg_pain_high
        }
    }
}
