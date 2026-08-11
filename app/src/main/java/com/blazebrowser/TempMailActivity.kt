package com.blazebrowser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blazebrowser.network.TempMailManager
import com.blazebrowser.network.TempMailProfile
import com.blazebrowser.network.TempMailMessage

class TempMailActivity : AppCompatActivity() {

    private lateinit var tempMailManager: TempMailManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var profileAdapter: ProfileAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp_mail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Temp Mail"

        tempMailManager = TempMailManager(this)
        recyclerView = findViewById(R.id.recyclerProfiles)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val btnCreateProfile = findViewById<Button>(R.id.btnCreateProfile)
        btnCreateProfile.setOnClickListener { createNewProfile() }

        refreshProfiles()
    }

    private fun createNewProfile() {
        Thread {
            val result = tempMailManager.createProfile()
            handler.post {
                if (result.isSuccess) {
                    val profile = result.getOrThrow()
                    Toast.makeText(this, "Created: ${profile.email}", Toast.LENGTH_LONG).show()
                    refreshProfiles()
                } else {
                    Toast.makeText(this, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun refreshProfiles() {
        val profiles = tempMailManager.getProfiles()
        profileAdapter = ProfileAdapter(profiles,
            onClick = { showProfileMessages(it) },
            onDelete = { deleteProfile(it) },
            onCopy = { copyToClipboard(it) }
        )
        recyclerView.adapter = profileAdapter
    }

    private fun showProfileMessages(profile: TempMailProfile) {
        Thread {
            val result = tempMailManager.getMessages(profile)
            handler.post {
                if (result.isSuccess) {
                    val messages = result.getOrThrow()
                    if (messages.isEmpty()) {
                        Toast.makeText(this, "No messages yet", Toast.LENGTH_SHORT).show()
                    } else {
                        showMessagesDialog(profile, messages)
                    }
                } else {
                    Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun showMessagesDialog(profile: TempMailProfile, messages: List<TempMailMessage>) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MessageAdapter(messages, onClick = {
            showMessageDetail(profile, it)
        })
        layout.addView(recyclerView)

        AlertDialog.Builder(this)
            .setTitle(profile.email)
            .setView(layout)
            .setPositiveButton("Refresh") { _, _ -> showProfileMessages(profile) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showMessageDetail(profile: TempMailProfile, message: TempMailMessage) {
        Thread {
            val result = tempMailManager.getMessage(profile, message.id)
            handler.post {
                val body = if (result.isSuccess) result.getOrThrow().body else "Failed to load"
                AlertDialog.Builder(this)
                    .setTitle(message.subject)
                    .setMessage("From: ${message.from}\n\n$body")
                    .setPositiveButton("Close", null)
                    .show()
            }
        }.start()
    }

    private fun deleteProfile(profile: TempMailProfile) {
        tempMailManager.deleteProfile(profile)
        refreshProfiles()
        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
    }

    private fun copyToClipboard(profile: TempMailProfile) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("email", profile.email))
        Toast.makeText(this, "Copied: ${profile.email}", Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        refreshRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }

    // Adapters
    inner class ProfileAdapter(
        private val items: List<TempMailProfile>,
        private val onClick: (TempMailProfile) -> Unit,
        private val onDelete: (TempMailProfile) -> Unit,
        private val onCopy: (TempMailProfile) -> Unit
    ) : RecyclerView.Adapter<ProfileAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val email: TextView = view.findViewById(R.id.textEmail)
            val created: TextView = view.findViewById(R.id.textCreated)
            val btnCopy: Button = view.findViewById(R.id.btnCopy)
            val btnDelete: Button = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.email.text = item.email
            holder.created.text = android.text.format.DateFormat.getDateFormat(this@TempMailActivity).format(java.util.Date(item.createdAt))
            holder.itemView.setOnClickListener { onClick(item) }
            holder.btnCopy.setOnClickListener { onCopy(item) }
            holder.btnDelete.setOnClickListener { onDelete(item) }
        }

        override fun getItemCount() = items.size
    }

    inner class MessageAdapter(
        private val items: List<TempMailMessage>,
        private val onClick: (TempMailMessage) -> Unit
    ) : RecyclerView.Adapter<MessageAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val subject: TextView = view.findViewById(R.id.textSubject)
            val from: TextView = view.findViewById(R.id.textFrom)
            val date: TextView = view.findViewById(R.id.textDate)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.subject.text = item.subject
            holder.from.text = item.from
            holder.date.text = item.date
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}