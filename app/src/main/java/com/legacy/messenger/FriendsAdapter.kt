package com.legacy.messenger

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class FriendsAdapter(private val onItemClick: (JSONObject) -> Unit) : RecyclerView.Adapter<FriendsAdapter.FriendsViewHolder>() {

    private var items = mutableListOf<JSONObject>()

    fun submitList(list: List<JSONObject>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendsViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendsViewHolder, position: Int) {
        val friend = items[position]
        holder.bind(friend, onItemClick)
    }

    override fun getItemCount() = items.size

    class FriendsViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvAvatar: TextView = itemView.findViewById(R.id.tvAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(friend: JSONObject, onItemClick: (JSONObject) -> Unit) {
            tvAvatar.text = friend.optString("avatar", "🐱")
            tvName.text = friend.optString("name", "")

            val online = friend.optBoolean("online", false)
            tvStatus.text = if (online) "🟢 онлайн" else "⚫ офлайн"

            itemView.setOnClickListener {
                onItemClick(friend)
            }
        }
    }
}