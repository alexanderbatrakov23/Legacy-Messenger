package com.legacy.messenger

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvLogin: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var btnLogout: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvName = view.findViewById(R.id.tvName)
        tvLogin = view.findViewById(R.id.tvLogin)
        tvAvatar = view.findViewById(R.id.tvAvatar)
        btnLogout = view.findViewById(R.id.btnLogout)

        val prefs = requireContext().getSharedPreferences("legacy", android.content.Context.MODE_PRIVATE)
        val name = prefs.getString("user_name", "")
        val login = prefs.getString("user_login", "")
        val avatar = prefs.getString("user_avatar", "🐱")

        tvName.text = name
        tvLogin.text = "@$login"
        tvAvatar.text = avatar

        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }
}