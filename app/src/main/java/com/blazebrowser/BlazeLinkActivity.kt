package com.blazebrowser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blazebrowser.bridge.BlazeLinkManager
import com.google.android.material.materialswitch.MaterialSwitch

class BlazeLinkActivity : AppCompatActivity() {

    private lateinit var blazeLinkManager: BlazeLinkManager
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blaze_link)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Blaze Link"
        toolbar.setNavigationOnClickListener { finish() }

        blazeLinkManager = BlazeLinkManager(this)
        recyclerView = findViewById(R.id.recyclerSites)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val sites = blazeLinkManager.getSites()
        recyclerView.adapter = SiteAdapter(sites,
            onToggle = { site, enabled ->
                blazeLinkManager.setEnabled(site.domain, enabled)
            },
            onLaunch = { site ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("url", "https://${site.domain}")
                intent.putExtra("blaze_link", true)
                startActivity(intent)
            }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    inner class SiteAdapter(
        private val items: List<com.blazebrowser.bridge.BlazeLinkSite>,
        private val onToggle: (com.blazebrowser.bridge.BlazeLinkSite, Boolean) -> Unit,
        private val onLaunch: (com.blazebrowser.bridge.BlazeLinkSite) -> Unit
    ) : RecyclerView.Adapter<SiteAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.textSiteName)
            val domain: TextView = view.findViewById(R.id.textSiteDomain)
            val toggle: MaterialSwitch = view.findViewById(R.id.switchEnable)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_blaze_link_site, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.name.text = item.displayName
            holder.domain.text = item.domain
            holder.toggle.isChecked = blazeLinkManager.isEnabled(item.domain)
            holder.toggle.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item, isChecked)
            }
            holder.itemView.setOnClickListener { onLaunch(item) }
        }

        override fun getItemCount() = items.size
    }
}
