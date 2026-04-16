package com.legacy.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class PeopleFragment : Fragment() {

    private lateinit var rvPeople: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var tvEmpty: TextView
    private lateinit var peopleAdapter: PeopleAdapter
    private val API_URL = "http://72.56.13.117:3000/api"
    private var currentUserId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_people, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("legacy", android.content.Context.MODE_PRIVATE)
        currentUserId = prefs.getInt("user_id", 0)

        rvPeople = view.findViewById(R.id.rvPeople)
        etSearch = view.findViewById(R.id.etSearch)
        btnSearch = view.findViewById(R.id.btnSearch)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        rvPeople.layoutManager = LinearLayoutManager(requireContext())
        peopleAdapter = PeopleAdapter { user ->
            showUserDialog(user)
        }
        rvPeople.adapter = peopleAdapter

        loadAllUsers()

        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                searchUsers(query)
            } else {
                loadAllUsers()
            }
        }
    }

    private fun loadAllUsers() {
        Thread {
            try {
                val url = URL("$API_URL/users/all/$currentUserId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000

                val response = conn.inputStream.bufferedReader().readText()
                val users = JSONArray(response)

                val userList = mutableListOf<JSONObject>()
                for (i in 0 until users.length()) {
                    userList.add(users.getJSONObject(i))
                }

                requireActivity().runOnUiThread {
                    if (userList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvPeople.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvPeople.visibility = View.VISIBLE
                        peopleAdapter.submitList(userList)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun searchUsers(query: String) {
        Thread {
            try {
                val url = URL("$API_URL/users/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&user_id=$currentUserId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000

                val response = conn.inputStream.bufferedReader().readText()
                val users = JSONArray(response)

                val userList = mutableListOf<JSONObject>()
                for (i in 0 until users.length()) {
                    userList.add(users.getJSONObject(i))
                }

                requireActivity().runOnUiThread {
                    if (userList.isEmpty()) {
                        tvEmpty.text = "Никого не найдено"
                        tvEmpty.visibility = View.VISIBLE
                        rvPeople.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvPeople.visibility = View.VISIBLE
                        peopleAdapter.submitList(userList)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun sendFriendRequest(toUserId: Int) {
        Thread {
            try {
                val url = URL("$API_URL/friend/request")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                // ИСПРАВЛЕНО: имена полей должны совпадать с сервером
                val json = JSONObject().apply {
                    put("from_user_id", currentUserId)
                    put("to_user_id", toUserId)
                }

                val outputStream: OutputStream = conn.outputStream
                outputStream.write(json.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = conn.responseCode
                val response = if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "No error body"
                }

                requireActivity().runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(requireContext(), "Заявка отправлена", Toast.LENGTH_SHORT).show()
                    } else {
                        val error = try {
                            JSONObject(response).optString("error", "Ошибка")
                        } catch (e: Exception) {
                            "Ошибка сервера"
                        }
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showUserDialog(user: JSONObject) {
        val name = user.optString("name", "")
        val login = user.optString("login", "")
        val avatar = user.optString("avatar", "🐱")
        val userId = user.getInt("id")

        val view = layoutInflater.inflate(R.layout.dialog_user_info, null)
        val tvName = view.findViewById<TextView>(R.id.tvUserName)
        val tvLogin = view.findViewById<TextView>(R.id.tvUserLogin)
        val tvAvatar = view.findViewById<TextView>(R.id.tvUserAvatar)
        val btnAdd = view.findViewById<Button>(R.id.btnAddFriend)

        tvName.text = name
        tvLogin.text = "@$login"
        tvAvatar.text = avatar

        AlertDialog.Builder(requireContext())
            .setTitle("Пользователь")
            .setView(view)
            .setPositiveButton("Закрыть", null)
            .show()

        btnAdd.setOnClickListener {
            sendFriendRequest(userId)
        }
    }
}