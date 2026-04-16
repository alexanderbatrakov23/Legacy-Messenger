package com.legacy.messenger

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnVoice: ImageButton
    private lateinit var tvContactName: TextView
    private lateinit var tvOnlineStatus: TextView

    private lateinit var messagesAdapter: MessagesAdapter
    private val messagesList = mutableListOf<JSONObject>()

    private var currentUserId = 0
    private var otherUserId = 0
    private var otherUserName = ""

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false

    private val API_URL = "http://72.56.13.117:3000/api"
    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    companion object {
        private const val REQUEST_RECORD_AUDIO = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val prefs = getSharedPreferences("legacy", MODE_PRIVATE)
        currentUserId = prefs.getInt("user_id", 0)

        otherUserId = intent.getIntExtra("user_id", 0)
        otherUserName = intent.getStringExtra("user_name") ?: ""

        initViews()
        setupRecyclerView()
        loadMessages()
        startAutoRefresh()

        btnSend.setOnClickListener { sendMessage() }
        btnVoice.setOnClickListener { toggleRecording() }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = otherUserName
    }

    private fun startAutoRefresh() {
        updateRunnable = object : Runnable {
            override fun run() {
                loadMessages()
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnVoice = findViewById(R.id.btnVoice)
        tvContactName = findViewById(R.id.tvContactName)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)

        tvContactName.text = otherUserName
        tvOnlineStatus.text = "🟢 онлайн"
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = messagesAdapter
    }

    private fun loadMessages() {
        Thread {
            try {
                val url = URL("$API_URL/messages/$currentUserId/$otherUserId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val messages = JSONArray(response)

                    runOnUiThread {
                        val oldSize = messagesList.size
                        messagesList.clear()
                        for (i in 0 until messages.length()) {
                            messagesList.add(messages.getJSONObject(i))
                        }
                        messagesAdapter.submitList(messagesList.toList())
                        if (messagesList.size != oldSize && messagesList.isNotEmpty()) {
                            rvMessages.scrollToPosition(messagesList.size - 1)
                        }
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        etMessage.text.clear()

        Thread {
            try {
                val url = URL("$API_URL/message")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val json = JSONObject().apply {
                    put("from_user_id", currentUserId)
                    put("to_user_id", otherUserId)
                    put("text", text)
                }

                val outputStream: OutputStream = conn.outputStream
                outputStream.write(json.toString().toByteArray())
                outputStream.flush()
                outputStream.close()
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            if (checkPermissions()) {
                startRecording()
            } else {
                requestPermissions()
            }
        }
    }

    private fun startRecording() {
        try {
            val audioDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "legacy_audio")
            if (!audioDir.exists()) audioDir.mkdirs()

            audioFile = File(audioDir, "audio_${System.currentTimeMillis()}.3gp")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            btnVoice.setImageResource(android.R.drawable.ic_media_pause)
            Toast.makeText(this, "Запись...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка записи", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            btnVoice.setImageResource(android.R.drawable.ic_btn_speak_now)
            Toast.makeText(this, "Запись сохранена", Toast.LENGTH_SHORT).show()
            sendAudioFile(audioFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendAudioFile(file: File?) {
        if (file == null) return

        Thread {
            try {
                val bytes = file.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)

                val url = URL("$API_URL/message")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("from_user_id", currentUserId)
                    put("to_user_id", otherUserId)
                    put("text", "🎤 Голосовое сообщение")
                    put("file_data", base64)
                    put("file_type", "audio")
                }

                conn.outputStream.write(json.toString().toByteArray())
                conn.outputStream.flush()
                conn.outputStream.close()
                conn.disconnect()
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        updateRunnable?.let { handler.removeCallbacks(it) }
        mediaRecorder?.release()
    }
}