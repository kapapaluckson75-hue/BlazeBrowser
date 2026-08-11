package com.blazebrowser

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blazebrowser.network.TempMailManager
import com.blazebrowser.network.TempMailProfile
import com.blazebrowser.network.TempMailMessage
import com.google.android.material.floatingactionbutton.FloatingActionButton

class InboxActivity : AppCompatActivity() {

    private lateinit var tempMailManager: TempMailManager
    private lateinit var profile: TempMailProfile
    private lateinit var recyclerView: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var textProfileEmail: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

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

        recyclerView = findViewById(R.id.recyclerMessages)
        textEmpty = findViewById(R.id.textEmpty)
        textProfileEmail = findViewById(R.id.textProfileEmail)
        recyclerView.layoutManager = LinearLayoutManager(this)
        textProfileEmail.text = profile.email

        val fabRefresh = findViewById<FloatingActionButton>(R.id.fabRefresh)
        fabRefresh.setOnClickListener { loadMessages() }

        loadMessages()
    }

    private fun loadMessages() {
        Thread {
            val result = tempMailManager.getMessages(profile)
            handler.post {
                if (result.isSuccess) {
                    val messages = result.getOrThrow()
                    if (messages.isEmpty()) {
                        textEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        textEmpty.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        messageAdapter = MessageAdapter(messages, onClick = { openMessage(it) })
                        recyclerView.adapter = messageAdapter
                    }
                } else {
                    Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun openMessage(message: TempMailMessage) {
        val intent = Intent(this, MessageDetailActivity::class.java)
        intent.putExtra("profile_email", profile.email)
        intent.putExtra("message_id", message.id)
        intent.putExtra("message_subject", message.subject)
        intent.putExtra("message_from", message.from)
        intent.putExtra("message_date", message.date)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    inner class MessageAdapter(
        private val items: List<TempMailMessage>,
        private val onClick: (TempMailMessage) -> Unit
    ) : RecyclerView.Adapter<MessageAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val subject: TextView = view.findViewById(R.id.textSubject)
            val from: TextView = view.findViewById(R.id.textFrom)
            val date: TextView = view.findViewById(R.id.textDate)
            val preview: TextView = view.findViewById(R.id.textPreview)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.subject.text = item.subject
            holder.from.text = item.from
            holder.date.text = item.date.take(16)
            holder.preview.text = item.body
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}