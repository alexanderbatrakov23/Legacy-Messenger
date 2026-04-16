package com.legacy.messenger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private var selectedAvatar = "🐱"
    private lateinit var etLogin: EditText
    private lateinit var etName: EditText
    private lateinit var etSurname: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnGoToLogin: TextView
    private lateinit var tvAvatarLabel: TextView
    private lateinit var rvAvatars: RecyclerView

    private val avatars = listOf("🐱", "🐶", "🦊", "🐼", "🦉", "🐧", "🦄", "🐢")
    private val API_URL = "http://72.56.13.117/api"  // СКРЫТЫЙ ДОМЕН
    private var isLoginMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверка сохраненной сессии
        val prefs = getSharedPreferences("legacy", MODE_PRIVATE)
        val token = prefs.getString("user_token", null)
        val userId = prefs.getInt("user_id", 0)

        if (token != null && userId != 0) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        etLogin = findViewById(R.id.etLogin)
        etName = findViewById(R.id.etName)
        etSurname = findViewById(R.id.etSurname)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        btnGoToLogin = findViewById(R.id.btnGoToLogin)
        tvAvatarLabel = findViewById(R.id.tvAvatarLabel)
        rvAvatars = findViewById(R.id.rvAvatars)

        setupAvatarRecycler()
        updateUIMode()

        btnRegister.setOnClickListener {
            if (isLoginMode) login() else register()
        }

        btnGoToLogin.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUIMode()
        }
    }

    private fun updateUIMode() {
        if (isLoginMode) {
            btnRegister.text = "ВОЙТИ"
            btnGoToLogin.text = "Нет аккаунта? Зарегистрироваться"
            etName.visibility = View.GONE
            etSurname.visibility = View.GONE
            etConfirmPassword.visibility = View.GONE
            tvAvatarLabel.visibility = View.GONE
            rvAvatars.visibility = View.GONE
        } else {
            btnRegister.text = "ЗАРЕГИСТРИРОВАТЬСЯ"
            btnGoToLogin.text = "Уже есть аккаунт? Войти"
            etName.visibility = View.VISIBLE
            etSurname.visibility = View.VISIBLE
            etConfirmPassword.visibility = View.VISIBLE
            tvAvatarLabel.visibility = View.VISIBLE
            rvAvatars.visibility = View.VISIBLE
        }
    }

    private fun register() {
        val login = etLogin.text.toString().trim()
        val name = etName.text.toString().trim()
        val surname = etSurname.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (login.isEmpty() || name.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 4) {
            Toast.makeText(this, "Пароль минимум 4 символа", Toast.LENGTH_SHORT).show()
            return
        }

        if (!login.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            Toast.makeText(this, "Логин: только латиница, цифры и _", Toast.LENGTH_SHORT).show()
            return
        }

        val progress = AlertDialog.Builder(this)
            .setTitle("Регистрация")
            .setMessage("Создаем аккаунт...")
            .setCancelable(false)
            .create()
        progress.show()

        Thread {
            try {
                val url = URL("$API_URL/register")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val json = JSONObject().apply {
                    put("login", login)
                    put("name", name)
                    put("surname", surname)
                    put("password", password)
                    put("avatar", selectedAvatar)
                }

                val outputStream: OutputStream = conn.outputStream
                outputStream.write(json.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = conn.responseCode
                val response = conn.inputStream.bufferedReader().readText()

                runOnUiThread {
                    progress.dismiss()
                    if (responseCode == 200) {
                        Toast.makeText(this, "Регистрация успешна! Теперь войдите", Toast.LENGTH_LONG).show()
                        isLoginMode = true
                        updateUIMode()
                        etPassword.text.clear()
                        etConfirmPassword.text.clear()
                    } else {
                        val error = JSONObject(response).optString("error", "Ошибка")
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    progress.dismiss()
                    Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun login() {
        val login = etLogin.text.toString().trim()
        val password = etPassword.text.toString()

        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
            return
        }

        val progress = AlertDialog.Builder(this)
            .setTitle("Вход")
            .setMessage("Вход в аккаунт...")
            .setCancelable(false)
            .create()
        progress.show()

        Thread {
            try {
                val url = URL("$API_URL/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val json = JSONObject().apply {
                    put("login", login)
                    put("password", password)
                }

                val outputStream: OutputStream = conn.outputStream
                outputStream.write(json.toString().toByteArray())
                outputStream.flush()
                outputStream.close()

                val responseCode = conn.responseCode
                val response = conn.inputStream.bufferedReader().readText()

                runOnUiThread {
                    progress.dismiss()
                    if (responseCode == 200) {
                        val jsonResponse = JSONObject(response)
                        val token = jsonResponse.getString("token")
                        val user = jsonResponse.getJSONObject("user")

                        val prefs = getSharedPreferences("legacy", MODE_PRIVATE)
                        prefs.edit()
                            .putString("user_token", token)
                            .putInt("user_id", user.getInt("id"))
                            .putString("user_login", user.getString("login"))
                            .putString("user_name", user.getString("name"))
                            .putString("user_surname", user.optString("surname"))
                            .putString("user_avatar", user.getString("avatar"))
                            .apply()

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        val error = JSONObject(response).optString("error", "Неверный логин или пароль")
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    progress.dismiss()
                    Toast.makeText(this, "Нет подключения к интернету", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun setupAvatarRecycler() {
        val adapter = AvatarAdapter(avatars) { avatar ->
            selectedAvatar = avatar
        }
        rvAvatars.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvAvatars.adapter = adapter
    }
}