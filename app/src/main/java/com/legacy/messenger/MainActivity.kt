package com.legacy.messenger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_navigation)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_chats -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, ChatsFragment())
                        .commit()
                    true
                }
                R.id.navigation_contacts -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, ContactsFragment())
                        .commit()
                    true
                }
                R.id.navigation_settings -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, SettingsFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
        bottomNav.selectedItemId = R.id.navigation_chats
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }
}