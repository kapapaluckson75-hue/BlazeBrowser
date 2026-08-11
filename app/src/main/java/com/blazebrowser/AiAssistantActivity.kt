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
import com.blazebrowser.ai.AiConfig
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
    private var isProcessing = false

    data class ChatMessage(
        val role: String,
        val content: String,
        val toolCalls: List<ToolCall>? = null,
        val toolCallId: String? = null
    )

    data class ToolCall(
        val id: String,
        val name: String,
        val arguments: JSONObject
    )

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
            startActivity(Intent(this, AiConfigActivity::class.java))
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerChat)
        editMessage = findViewById(R.id.editMessage)
        btnSend = findViewById(R.id.btnSend)

        chatAdapter = ChatAdapter(messages)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = chatAdapter

        btnSend.setOnClickListener { sendMessage() }

        messages.add(ChatMessage("assistant", "Hello! I'm BlazeAI. I can help you browse the web, search for information, and more. What would you like to do?"))
        chatAdapter.notifyDataSetChanged()
    }

    private fun sendMessage() {
        val text = editMessage.text.toString().trim()
        if (text.isEmpty() || isProcessing) return

        isProcessing = true
        messages.add(ChatMessage("user", text))
        chatAdapter.notifyDataSetChanged()
        editMessage.setText("")

        executor.execute {
            try {
                processWithTools(messages)
            } catch (e: Exception) {
                handler.post {
                    messages.add(ChatMessage("assistant", "Error: ${e.message}"))
                    chatAdapter.notifyDataSetChanged()
                    isProcessing = false
                }
            }
        }
    }

    private fun processWithTools(initialMessages: List<ChatMessage>) {
        var currentMessages = initialMessages.toMutableList()
        var toolCalls: List<ToolCall>? = null

        while (true) {
            handler.post {
                messages.add(ChatMessage("assistant", "⚡ Thinking..."))
                chatAdapter.notifyDataSetChanged()
            }

            val response = callAiApi(currentMessages)
            handler.post {
                if (messages.isNotEmpty()) {
                    messages.removeAt(messages.size - 1)
                }
                chatAdapter.notifyDataSetChanged()
            }

            // Parse response for tool calls
            val parsed = parseResponseWithTools(response)
            toolCalls = parsed.toolCalls

            if (toolCalls.isNullOrEmpty()) {
                // No tool calls, just text response
                handler.post {
                    messages.add(ChatMessage("assistant", parsed.content))
                    chatAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messages.size - 1)
                    isProcessing = false
                }
                break
            }

            // Add assistant message with tool calls
            currentMessages.add(ChatMessage("assistant", parsed.content, toolCalls))
            handler.post {
                messages.add(ChatMessage("assistant", "🔧 Using tools: ${toolCalls.joinToString(", ") { it.name }}"))
                chatAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messages.size - 1)
            }

            // Execute each tool call
            for (toolCall in toolCalls) {
                val result = executeTool(toolCall)
                currentMessages.add(ChatMessage("tool", result, null, toolCall.id))
                handler.post {
                    messages.add(ChatMessage("tool", "✓ ${toolCall.name}: $result"))
                    chatAdapter.notifyDataSetChanged()
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun executeTool(toolCall: ToolCall): String {
        return when (toolCall.name) {
            "browse_url" -> {
                val url = toolCall.arguments.optString("url", "")
                if (url.isBlank()) "Error: No URL provided"
                else browseUrl(url)
            }
            "search_web" -> {
                val query = toolCall.arguments.optString("query", "")
                if (query.isBlank()) "Error: No query provided"
                else searchWeb(query)
            }
            "read_page" -> {
                val url = toolCall.arguments.optString("url", "")
                if (url.isBlank()) "Error: No URL provided"
                else readPage(url)
            }
            else -> "Error: Unknown tool '${toolCall.name}'"
        }
    }

    private fun browseUrl(url: String): String {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val content = reader.readText()
                reader.close()
                conn.disconnect()

                // Extract title
                val title = extractTag(content, "title")
                val description = extractMetaTag(content, "description")

                "Successfully browsed: $url\nTitle: $title\nDescription: $description\nContent preview: ${content.take(500)}"
            } else {
                conn.disconnect()
                "Error: HTTP $responseCode for $url"
            }
        } catch (e: Exception) {
            "Error browsing $url: ${e.message}"
        }
    }

    private fun searchWeb(query: String): String {
        return try {
            val encodedQuery = query.replace(" ", "+")
            val searchUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"
            val conn = URL(searchUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val content = reader.readText()
                reader.close()
                conn.disconnect()

                // Extract search results
                val results = extractSearchResults(content)
                if (results.isEmpty()) {
                    "No results found for: $query"
                } else {
                    "Search results for '$query':\n\n$results"
                }
            } else {
                conn.disconnect()
                "Error: HTTP $responseCode"
            }
        } catch (e: Exception) {
            "Error searching: ${e.message}"
        }
    }

    private fun readPage(url: String): String {
        return browseUrl(url)
    }

    private fun extractTag(html: String, tag: String): String {
        val regex = "<$tag[^>]*>([^<]+)</$tag>".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.get(1)?.trim() ?: "N/A"
    }

    private fun extractMetaTag(html: String, name: String): String {
        val regex = "<meta[^>]*name=[\"']$name[\"'][^>]*content=[\"']([^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.get(1)?.trim() ?: "N/A"
    }

    private fun extractSearchResults(html: String): String {
        val results = mutableListOf<String>()
        // DDG HTML results
        val regex = "<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>".toRegex(RegexOption.IGNORE_CASE)
        regex.findAll(html).take(5).forEach { match ->
            results.add("${results.size + 1}. ${match.groupValues[2].trim()} - ${match.groupValues[1]}")
        }
        return results.joinToString("\n")
    }

    private fun callAiApi(messages: List<ChatMessage>): String {
        val url = URL(aiConfig.getEndpoint())
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${aiConfig.apiKey}")
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 60000

        val body = buildRequestBody(messages)
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            conn.disconnect()
            return response
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
            if (msg.toolCalls != null) {
                val toolCallsArray = JSONArray()
                msg.toolCalls.forEach { tc ->
                    val tcObj = JSONObject()
                    tcObj.put("id", tc.id)
                    tcObj.put("type", "function")
                    tcObj.put("function", JSONObject().apply {
                        put("name", tc.name)
                        put("arguments", tc.arguments.toString())
                    })
                    toolCallsArray.put(tcObj)
                }
                msgObj.put("tool_calls", toolCallsArray)
            }
            if (msg.toolCallId != null) {
                msgObj.put("tool_call_id", msg.toolCallId)
            }
            messagesArray.put(msgObj)
        }
        body.put("messages", messagesArray)
        body.put("max_tokens", 4096)

        // Add tools
        val tools = JSONArray()

        val browseTool = JSONObject()
        browseTool.put("type", "function")
        browseTool.put("function", JSONObject().apply {
            put("name", "browse_url")
            put("description", "Navigate to a URL and get the page content. Use to visit websites, read articles, get information from the web.")
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("url", JSONObject().apply {
                        put("type", "string")
                        put("description", "The full URL to navigate to (e.g., https://example.com)")
                    })
                })
                put("required", JSONArray().put("url"))
            })
        })
        tools.put(browseTool)

        val searchTool = JSONObject()
        searchTool.put("type", "function")
        searchTool.put("function", JSONObject().apply {
            put("name", "search_web")
            put("description", "Search the web for information. Returns top search results.")
            put("parameters", JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "string")
                        put("description", "The search query")
                    })
                })
                put("required", JSONArray().put("query"))
            })
        })
        tools.put(searchTool)

        body.put("tools", tools)

        return body
    }

    data class ParsedResponse(val content: String, val toolCalls: List<ToolCall>?)

    private fun parseResponseWithTools(response: String): ParsedResponse {
        val json = JSONObject(response)
        val choices = json.getJSONArray("choices")
        if (choices.length() > 0) {
            val choice = choices.getJSONObject(0)
            val message = choice.getJSONObject("message")
            val content = message.optString("content", "")

            // Check for tool calls
            val toolCallsArray = if (message.has("tool_calls")) message.getJSONArray("tool_calls") else null
            val toolCalls = mutableListOf<ToolCall>()

            if (toolCallsArray != null) {
                for (i in 0 until toolCallsArray.length()) {
                    val tc = toolCallsArray.getJSONObject(i)
                    val id = tc.getString("id")
                    val function = tc.getJSONObject("function")
                    val name = function.getString("name")
                    val argsStr = function.optString("arguments", "{}")
                    val args = try { JSONObject(argsStr) } catch (e: Exception) { JSONObject() }
                    toolCalls.add(ToolCall(id, name, args))
                }
            }

            return ParsedResponse(content, if (toolCalls.isEmpty()) null else toolCalls)
        }
        return ParsedResponse("No response", null)
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
            holder.role.text = when (item.role) {
                "user" -> "You"
                "assistant" -> "BlazeAI"
                "tool" -> "Tool Result"
                else -> item.role
            }
            holder.role.setTextColor(when (item.role) {
                "user" -> getColor(android.R.color.holo_blue_light)
                "assistant" -> getColor(R.color.blaze_orange)
                "tool" -> getColor(android.R.color.holo_green_light)
                else -> getColor(R.color.blaze_orange)
            })
            holder.content.text = item.content
        }

        override fun getItemCount() = items.size
    }
}