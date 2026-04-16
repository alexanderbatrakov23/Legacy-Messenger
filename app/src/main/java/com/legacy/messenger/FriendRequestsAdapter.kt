package com.legacy.messenger

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class FriendRequestsAdapter(
    private val onAccept: (JSONObject) -> Unit,
    private val onReject: (JSONObject) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder>() {

    private var items = mutableListOf<JSONObject>()

    fun submitList(list: List<JSONObject>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = items[position]
        holder.bind(request, onAccept, onReject)
    }

    override fun getItemCount() = items.size

    class RequestViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvAvatar: TextView = itemView.findViewById(R.id.tvAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvLogin: TextView = itemView.findViewById(R.id.tvLogin)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)

        fun bind(request: JSONObject, onAccept: (JSONObject) -> Unit, onReject: (JSONObject) -> Unit) {
            val name = request.optString("name", "")
            val login = request.optString("login", "")
            val avatar = request.optString("avatar", "🐱")

            tvAvatar.text = avatar
            tvName.text = name
            tvLogin.text = "@$login"

            btnAccept.setOnClickListener {
                onAccept(request)
            }

            btnReject.setOnClickListener {
                onReject(request)
            }
        }
    }
}