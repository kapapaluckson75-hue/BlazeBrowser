package com.blazebrowser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.blazebrowser.network.TempMailManager
import com.blazebrowser.network.TempMailProfile

class MessageDetailActivity : AppCompatActivity() {

    private lateinit var tempMailManager: TempMailManager
    private lateinit var profile: TempMailProfile
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_detail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tempMailManager = TempMailManager(this)

        val profileEmail = intent.getStringExtra("profile_email")
        val profiles = tempMailManager.getProfiles()
        profile = profiles.find { it.email == profileEmail } ?: run {
            Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val subject = intent.getStringExtra("message_subject") ?: "No subject"
        val from = intent.getStringExtra("message_from") ?: "Unknown"
        val date = intent.getStringExtra("message_date") ?: ""
        val messageId = intent.getStringExtra("message_id") ?: ""

        findViewById<TextView>(R.id.textSubject).text = subject
        findViewById<TextView>(R.id.textFrom).text = "From: $from"
        findViewById<TextView>(R.id.textDate).text = date

        val textBody = findViewById<TextView>(R.id.textBody)
        textBody.text = "Loading..."

        loadMessage(messageId)
    }

    private fun loadMessage(messageId: String) {
        Thread {
            val result = tempMailManager.getMessage(profile, messageId)
            handler.post {
                val textBody = findViewById<TextView>(R.id.textBody)
                if (result.isSuccess) {
                    textBody.text = result.getOrThrow().body
                } else {
                    textBody.text = "Failed to load message"
                    Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}