package com.blazebrowser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.blazebrowser.ai.AiConfig
import com.blazebrowser.ai.AiProviders
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AiConfigActivity : AppCompatActivity() {

    private lateinit var aiConfig: AiConfig
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_config)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "BlazeAI Settings"
        toolbar.setNavigationOnClickListener { finish() }

        aiConfig = AiConfig(this)

        val spinnerProvider = findViewById<MaterialAutoCompleteTextView>(R.id.spinnerProvider)
        val editApiKey = findViewById<TextInputEditText>(R.id.editApiKey)
        val editCustomEndpoint = findViewById<TextInputEditText>(R.id.editCustomEndpoint)
        val editModel = findViewById<TextInputEditText>(R.id.editModel)
        val layoutCustomEndpoint = findViewById<TextInputLayout>(R.id.layoutCustomEndpoint)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val textStatus = findViewById<TextView>(R.id.textStatus)

        // Load existing config
        val providers = AiProviders.providers.map { it.displayName }.toTypedArray()
        spinnerProvider.setSimpleItems(providers)
        val currentProvider = AiProviders.providers.find { it.id == aiConfig.providerId }
        if (currentProvider != null) {
            spinnerProvider.setText(currentProvider.displayName, false)
        }
        editApiKey.setText(aiConfig.apiKey)
        editCustomEndpoint.setText(aiConfig.customEndpoint)
        editModel.setText(aiConfig.model)

        // Show/hide custom endpoint based on selection
        spinnerProvider.setOnItemClickListener { _, _, position, _ ->
            val selectedProvider = AiProviders.providers[position]
            layoutCustomEndpoint.visibility = if (selectedProvider.id == "custom") View.VISIBLE else View.GONE
            if (aiConfig.model.isBlank()) {
                editModel.hint = "Default: ${selectedProvider.defaultModel}"
            }
        }

        // Set initial visibility
        if (aiConfig.providerId == "custom") {
            layoutCustomEndpoint.visibility = View.VISIBLE
        }

        btnSave.setOnClickListener {
            val providerPosition = providers.indexOf(spinnerProvider.text.toString())
            if (providerPosition < 0) {
                Toast.makeText(this, "Please select a provider", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedProvider = AiProviders.providers[providerPosition]
            val apiKey = editApiKey.text.toString()
            val customEndpoint = editCustomEndpoint.text.toString()
            val model = editModel.text.toString()

            if (apiKey.isBlank()) {
                Toast.makeText(this, "API Key is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedProvider.id == "custom" && customEndpoint.isBlank()) {
                Toast.makeText(this, "Custom endpoint is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            aiConfig.providerId = selectedProvider.id
            aiConfig.apiKey = apiKey
            aiConfig.customEndpoint = customEndpoint
            aiConfig.model = model

            textStatus.text = "Configuration saved!"
            textStatus.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}