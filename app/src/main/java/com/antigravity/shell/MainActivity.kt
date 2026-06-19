package com.antigravity.shell

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AntigravityShellApp()
        }
    }
}

// Data models
data class ChatMessage(val content: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())
data class AttachedFile(val name: String, val uri: Uri, val size: String)

// Dark Palette styling for Antigravity
val DarkBackground = Color(0xFF121212)
val SurfaceColor = Color(0xFF1E1E1E)
val PrimaryNeon = Color(0xFF00E676) // Glowing green
val SecondaryText = Color(0xFFB0B0B0)
val UserBubbleColor = Color(0xFF2C2C2C)
val AgentBubbleColor = Color(0xFF1E2A22)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntigravityShellApp() {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val attachedFiles = remember { mutableStateListOf<AttachedFile>() }
    var currentTermuxCommand by remember { mutableStateOf("agy") }
    var serverUrl by remember { mutableStateOf("http://localhost:8080") }
    var isConnected by remember { mutableStateOf(false) }

    // Init with welcome message
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(
                ChatMessage(
                    "Welcome to Antigravity CLI Shell. Connect to your Termux node or send commands via Termux:API RUN_COMMAND.",
                    isUser = false
                )
            )
        }
    }

    // Launchers for files & directories
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val name = getFileName(context, uri)
            val size = getFileSize(context, uri)
            attachedFiles.add(AttachedFile(name, uri, size))
        }
    }

    // Command sender logic
    val sendCommand = {
        if (inputText.isNotBlank()) {
            val userText = inputText
            messages.add(ChatMessage(userText, isUser = true))
            inputText = ""

            // Send command via Local Server or HTTP API
            CoroutineScope(Dispatchers.IO).launch {
                val response = executeCommandOverNetwork(serverUrl, userText, attachedFiles.toList(), context)
                CoroutineScope(Dispatchers.Main).launch {
                    messages.add(ChatMessage(response, isUser = false))
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = SurfaceColor,
            primary = PrimaryNeon
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Antigravity CLI Shell",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isConnected) PrimaryNeon else Color.Red)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isConnected) "Connected" else "Disconnected (Termux Local Server)",
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            // Check status
                            CoroutineScope(Dispatchers.IO).launch {
                                val status = checkServerStatus(serverUrl)
                                CoroutineScope(Dispatchers.Main).launch {
                                    isConnected = status
                                    Toast.makeText(context, if (status) "Connected successfully!" else "Termux local server not running. Start it with 'agy --server'", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh status", tint = PrimaryNeon)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceColor)
                )
            },
            bottomBar = {
                // Bottom input area with file attachments display
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceColor)
                        .padding(8.dp)
                ) {
                    // Attached files list
                    if (attachedFiles.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                                .padding(bottom = 8.dp)
                        ) {
                            items(attachedFiles) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AttachFile, contentDescription = "File", tint = PrimaryNeon, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(file.name, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                    }
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.Red,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { attachedFiles.remove(file) }
                                    )
                                }
                            }
                        }
                    }

                    // Input Textbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Attach Files", tint = PrimaryNeon)
                        }

                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Send prompts, context, or commands...", color = SecondaryText) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF252525),
                                unfocusedContainerColor = Color(0xFF252525),
                                focusedIndicatorColor = PrimaryNeon,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendCommand() })
                        )

                        IconButton(
                            onClick = { sendCommand() },
                            enabled = inputText.isNotBlank() || attachedFiles.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() || attachedFiles.isNotEmpty()) PrimaryNeon else SecondaryText
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DarkBackground)
            ) {
                // Settings Accordion
                var showSettings by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSettings = !showSettings },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Connection & Termux Settings", color = Color.White, fontWeight = FontWeight.Bold)
                            Icon(
                                if (showSettings) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle Settings",
                                tint = Color.White
                            )
                        }

                        if (showSettings) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                label = { Text("Termux Server API Endpoint", color = SecondaryText) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryNeon,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = PrimaryNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "To run the HTTP bridge backend inside Termux:\n" +
                                "$ agy --server --port 8080\n" +
                                "This connects the GUI app directly to your active CLI context.",
                                color = SecondaryText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Chat Messages Window
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    reverseLayout = false
                ) {
                    items(messages) { message ->
                        ChatBubble(message)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isUser) UserBubbleColor else AgentBubbleColor
    val textColor = Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bubbleColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}

// Utility functions to read file metadata
fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Unknown"
}

fun getFileSize(context: Context, uri: Uri): String {
    var fileSize: Long = 0
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index != -1) {
                    fileSize = cursor.getLong(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    return if (fileSize > 0) "${fileSize / 1024} KB" else "Unknown"
}

// Network requests to communicate with the Python/Node HTTP Server in Termux running Antigravity CLI
private fun checkServerStatus(urlStr: String): Boolean {
    return try {
        val url = URL("$urlStr/status")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        val responseCode = connection.responseCode
        responseCode == 200
    } catch (e: Exception) {
        false
    }
}

private fun executeCommandOverNetwork(
    serverUrl: String,
    prompt: String,
    files: List<AttachedFile>,
    context: Context
): String {
    return try {
        // Send a POST request to localhost server running in Termux
        val url = URL("$serverUrl/prompt")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        // Build a JSON payload with prompt and file contents if any
        // For files, we could read their contents and embed them
        val fileDataBuilder = StringBuilder()
        files.forEach { file ->
            try {
                val inputStream = context.contentResolver.openInputStream(file.uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                fileDataBuilder.append("\nFile: ${file.name}\n$content\n---")
            } catch (e: Exception) {
                fileDataBuilder.append("\nFailed to read file ${file.name}: ${e.message}\n---")
            }
        }

        val fullPrompt = if (files.isNotEmpty()) {
            "Prompt: $prompt\n\nAttached files context:$fileDataBuilder"
        } else {
            prompt
        }

        // Extremely simple JSON escaping
        val escapedPrompt = fullPrompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        val payload = "{\"prompt\": \"$escapedPrompt\"}"

        connection.outputStream.use { os ->
            os.write(payload.toByteArray(Charsets.UTF_8))
        }

        if (connection.responseCode == 200) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            // Extract the result text from JSON response or use directly
            response
        } else {
            "Server Error: ${connection.responseCode} - ${connection.responseMessage}"
        }
    } catch (e: Exception) {
        "Failed to connect to Antigravity Termux Backend at $serverUrl.\nError: ${e.localizedMessage}\n\nMake sure your server is running in Termux using:\n$ agy --server"
    }
}
