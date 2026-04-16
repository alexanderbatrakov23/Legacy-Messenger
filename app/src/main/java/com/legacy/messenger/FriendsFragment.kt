package com.legacy.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class FriendsFragment : Fragment() {

    private lateinit var rvFriends: RecyclerView
    private lateinit var rvRequests: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvRequestsLabel: TextView
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var requestsAdapter: FriendRequestsAdapter
    private val API_URL = "http://72.56.13.117:3000/api"
    private val WS_URL = "ws://72.56.13.117:3000"
    private var currentUserId = 0
    private lateinit var webSocket: WebSocket
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_friends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("legacy", android.content.Context.MODE_PRIVATE)
        currentUserId = prefs.getInt("user_id", 0)

        rvFriends = view.findViewById(R.id.rvFriends)
        rvRequests = view.findViewById(R.id.rvRequests)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        tvRequestsLabel = view.findViewById(R.id.tvRequestsLabel)

        rvFriends.layoutManager = LinearLayoutManager(requireContext())
        rvRequests.layoutManager = LinearLayoutManager(requireContext())

        friendsAdapter = FriendsAdapter { friend ->
            val intent = android.content.Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("user_id", friend.getInt("id"))
            intent.putExtra("user_name", friend.getString("name"))
            intent.putExtra("user_avatar", friend.getString("avatar"))
            startActivity(intent)
        }
        rvFriends.adapter = friendsAdapter

        requestsAdapter = FriendRequestsAdapter(
            onAccept = { request ->
                acceptRequest(request)
            },
            onReject = { request ->
                rejectRequest(request)
            }
        )
        rvRequests.adapter = requestsAdapter

        connectWebSocket()
        loadData()
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val authJson = JSONObject().apply {
                    put("type", "auth")
                    put("user_id", currentUserId)
                }
                webSocket.send(authJson.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val data = JSONObject(text)
                if (data.optString("type") == "user_status") {
                    loadFriends()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        loadFriends()
        loadIncomingRequests()
    }

    private fun loadFriends() {
        Thread {
            try {
                val url = URL("$API_URL/friends/$currentUserId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000

                val response = conn.inputStream.bufferedReader().readText()
                val friends = JSONArray(response)

                val friendList = mutableListOf<JSONObject>()
                for (i in 0 until friends.length()) {
                    friendList.add(friends.getJSONObject(i))
                }

                requireActivity().runOnUiThread {
                    if (friendList.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvFriends.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvFriends.visibility = View.VISIBLE
                        friendsAdapter.submitList(friendList)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun loadIncomingRequests() {
        Thread {
            try {
                val url = URL("$API_URL/friend/requests/incoming/$currentUserId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000

                val response = conn.inputStream.bufferedReader().readText()
                val requests = JSONArray(response)

                val requestsList = mutableListOf<JSONObject>()
                for (i in 0 until requests.length()) {
                    requestsList.add(requests.getJSONObject(i))
                }

                requireActivity().runOnUiThread {
                    if (requestsList.isEmpty()) {
                        tvRequestsLabel.visibility = View.GONE
                        rvRequests.visibility = View.GONE
                    } else {
                        tvRequestsLabel.visibility = View.VISIBLE
                        rvRequests.visibility = View.VISIBLE
                        requestsAdapter.submitList(requestsList)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun acceptRequest(request: JSONObject) {
        val requestId = request.getInt("id")

        Thread {
            try {
                val url = URL("$API_URL/friend/accept")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("request_id", requestId)
                    put("user_id", currentUserId)
                }

                conn.outputStream.write(json.toString().toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Заявка принята", Toast.LENGTH_SHORT).show()
                    loadData()
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun rejectRequest(request: JSONObject) {
        val requestId = request.getInt("id")

        Thread {
            try {
                val url = URL("$API_URL/friend/reject")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("request_id", requestId)
                    put("user_id", currentUserId)
                }

                conn.outputStream.write(json.toString().toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()

                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Заявка отклонена", Toast.LENGTH_SHORT).show()
                    loadData()
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Closing")
    }
}