package com.blazebrowser

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
import com.blazebrowser.ai.AiConfig
import com.blazebrowser.ai.AiProviders
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class AiAssistantActivity : AppCompatActivity() {

    private lateinit var aiConfig: AiConfig
    private lateinit var recyclerView: RecyclerView
    private lateinit var editMessage: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    data class ChatMessage(val role: String, val content: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        aiConfig = AiConfig(this)

        if (!aiConfig.isConfigured()) {
            Toast.makeText(this, "Please configure BlazeAI first", Toast.LENGTH_LONG).show()
            startActivity(android.content.Intent(this, AiConfigActivity::class.java))
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerChat)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)

        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        btnSend.setOnClickListener { sendMessage() }

        messages.add(ChatMessage("assistant", "Hello! I'm BlazeAI. I can help you browse the web, search for information, and more. What would you like to do?"))
        chatAdapter.notifyDataSetChanged()
    }

    private fun sendMessage() {
        val text = editMessage.text.toString().trim()
        if (text.isEmpty()) return

        messages.add(ChatMessage("user", text))
        chatAdapter.notifyDataSetChanged()
        editMessage.setText("")

        executor.execute {
            try {
                val response = callAiApi(messages)
                handler.post {
                    messages.add(ChatMessage("assistant", response))
                    chatAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                handler.post {
                    messages.add(ChatMessage("assistant", "Error: ${e.message}"))
                    chatAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun callAiApi(messages: List<ChatMessage>): String {
        val url = URL(aiConfig.getEndpoint())
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${aiConfig.apiKey}")
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 30000

        val body = buildRequestBody(messages)
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()
            return parseResponse(response)
        } else {
            val errorReader = BufferedReader(InputStreamReader(conn.errorStream))
            val error = errorReader.readText()
            errorReader.close()
            conn.disconnect()
            throw Exception("HTTP $responseCode: $error")
        }
    }

    private fun buildRequestBody(messages: List<ChatMessage>): JSONObject {
        val body = JSONObject()
        body.put("model", aiConfig.getEffectiveModel())

        val messagesArray = JSONArray()
        messages.forEach { msg ->
            val msgObj = JSONObject()
            msgObj.put("role", msg.role)
            msgObj.put("content", msg.content)
            messagesArray.put(msgObj)
        }
        body.put("messages", messagesArray)
        body.put("max_tokens", 4096)

        // Add tools for the AI to use
        val tools = JSONArray()

        val browseTool = JSONObject()
        browseTool.put("name", "browse_url")
        browseTool.put("description", "Navigate to a URL and get the page content")
        browseTool.put("input_schema", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("url", JSONObject().apply {
                    put("type", "string")
                    put("description", "The URL to navigate to")
                })
            })
            put("required", JSONArray().put("url"))
        })
        tools.put(browseTool)

        val searchTool = JSONObject()
        searchTool.put("name", "search_web")
        searchTool.put("description", "Search the web for information")
        searchTool.put("input_schema", JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("query", JSONObject().apply {
                    put("type", "string")
                    put("description", "The search query")
                })
            })
            put("required", JSONArray().put("query"))
        })
        tools.put(searchTool)

        body.put("tools", tools)

        return body
    }

    private fun parseResponse(response: String): String {
        val json = JSONObject(response)
        val choices = json.getJSONArray("choices")
        if (choices.length() > 0) {
            val choice = choices.getJSONObject(0)
            val message = choice.getJSONObject("message")
            return message.optString("content", "No response")
        }
        return "No response"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    inner class ChatAdapter(private val items: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val role: TextView = view.findViewById(R.id.textRole)
            val content: TextView = view.findViewById(R.id.textContent)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.role.text = if (item.role == "user") "You" else "BlazeAI"
            holder.role.setTextColor(if (item.role == "user") getColor(android.R.color.holo_blue_light) else getColor(R.color.blaze_orange))
            holder.content.text = item.content
        }

        override fun getItemCount() = items.size
    }
}