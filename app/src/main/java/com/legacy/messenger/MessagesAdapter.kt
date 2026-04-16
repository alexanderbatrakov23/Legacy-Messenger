package com.legacy.messenger

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class MessagesAdapter(private val currentUserId: Int) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    private var items = mutableListOf<JSONObject>()

    fun submitList(list: List<JSONObject>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 0) R.layout.item_message_outgoing else R.layout.item_message_incoming
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return if (items[position].getInt("from_user_id") == currentUserId) 0 else 1
    }

    inner class MessageViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvMessageText)
        private val tvTime: TextView = itemView.findViewById(R.id.tvMessageTime)

        fun bind(message: JSONObject) {
            val text = message.optString("text", "")
            tvText.text = if (text.isEmpty()) "📎 Вложение" else text

            val sentAt = message.optString("sent_at", "")
            val timeStr = if (sentAt.length >= 16) sentAt.substring(11, 16) else ""
            tvTime.text = timeStr
        }
    }
}