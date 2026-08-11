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
import com.google.android.material.button.MaterialButton

class TempMailActivity : AppCompatActivity() {

    private lateinit var tempMailManager: TempMailManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var profileAdapter: ProfileAdapter
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp_mail)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tempMailManager = TempMailManager(this)
        recyclerView = findViewById(R.id.recyclerProfiles)
        textEmpty = findViewById(R.id.textEmpty)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val btnCreateProfile = findViewById<MaterialButton>(R.id.btnCreateProfile)
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
            onClick = { openInbox(it) },
            onDelete = { deleteProfile(it) },
            onCopy = { copyToClipboard(it) }
        )
        recyclerView.adapter = profileAdapter
        textEmpty.visibility = if (profiles.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (profiles.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun openInbox(profile: TempMailProfile) {
        val intent = Intent(this, InboxActivity::class.java)
        intent.putExtra("profile_email", profile.email)
        startActivity(intent)
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

    inner class ProfileAdapter(
        private val items: List<TempMailProfile>,
        private val onClick: (TempMailProfile) -> Unit,
        private val onDelete: (TempMailProfile) -> Unit,
        private val onCopy: (TempMailProfile) -> Unit
    ) : RecyclerView.Adapter<ProfileAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val email: TextView = view.findViewById(R.id.textEmail)
            val created: TextView = view.findViewById(R.id.textCreated)
            val copyBtn: View = view.findViewById(R.id.btnCopy)
            val openBtn: View = view.findViewById(R.id.btnOpenInbox)
            val deleteBtn: View = view.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.email.text = item.email
            holder.created.text = "Created: " + android.text.format.DateFormat.getDateFormat(this@TempMailActivity).format(java.util.Date(item.createdAt))
            holder.itemView.setOnClickListener { onClick(item) }
            holder.copyBtn.setOnClickListener { onCopy(item) }
            holder.openBtn.setOnClickListener { onClick(item) }
            holder.deleteBtn.setOnClickListener { onDelete(item) }
        }

        override fun getItemCount() = items.size
    }
}