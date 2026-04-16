package com.legacy.messenger

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PeopleAdapter(private val onItemClick: (JSONObject) -> Unit) : RecyclerView.Adapter<PeopleAdapter.PeopleViewHolder>() {

    private var items = mutableListOf<JSONObject>()

    fun submitList(list: List<JSONObject>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeopleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_people, parent, false)
        return PeopleViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeopleViewHolder, position: Int) {
        val user = items[position]
        holder.bind(user, onItemClick)
    }

    override fun getItemCount() = items.size

    class PeopleViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvAvatar: TextView = itemView.findViewById(R.id.tvAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvLogin: TextView = itemView.findViewById(R.id.tvLogin)

        fun bind(user: JSONObject, onItemClick: (JSONObject) -> Unit) {
            tvAvatar.text = user.optString("avatar", "🐱")
            tvName.text = user.optString("name", "")
            tvLogin.text = "@${user.optString("login", "")}"

            itemView.setOnClickListener {
                onItemClick(user)
            }
        }
    }
}