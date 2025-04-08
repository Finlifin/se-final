//package fin.phoenix.flix.ui.message
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.util.Log
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.Button
//import androidx.compose.material3.Card
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TopAppBar
//import androidx.compose.material3.TopAppBarDefaults
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewModelScope
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import fin.phoenix.flix.api.PhoenixMessageClient
//import fin.phoenix.flix.ui.colors.RoseRed
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.launch
//import org.json.JSONObject
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
///**
// * This is a test screen for the WebSocket functionality
// * The actual messaging implementation uses the MessageViewModel and related components
// */
//class TestMessageViewModel(@SuppressLint("StaticFieldLeak") val context: Context) : ViewModel() {
//    private val TAG = "TestMessageViewModel"
//    private val client = PhoenixMessageClient(context)
//
//    private val _messages = mutableStateListOf<MessageItem>()
//    val messages: List<MessageItem> = _messages
//
//    var isConnected by mutableStateOf(false)
//        private set
//
//    var isJoined by mutableStateOf(false)
//        private set
//
//    var connectionStatus by mutableStateOf("")
//        private set
//
//    private var userId: String? = null
//    private val channelName: String
//        get() = "user:$userId"
//
//    init {
//        // Get user ID from preferences
//        val sharedPref = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
//        userId = sharedPref.getString("user_id", null)
//
//        // Start collecting messages
//        setupMessageCollection()
//    }
//
//    /**
//     * 设置消息收集
//     */
//    private fun setupMessageCollection() {
//        client.messageFlow
//            .onEach { message ->
//                Log.d(TAG, "Received message: $message")
//                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//                val newMessage = MessageItem(
//                    topic = message.topic,
//                    event = message.event,
//                    payload = message.payload.toString(),
//                    timestamp = timestamp
//                )
//                _messages.add(0, newMessage)
//            }
//            .catch { e ->
//                Log.e(TAG, "Error collecting messages", e)
//                connectionStatus = "消息收集错误：${e.message}"
//            }
//            .launchIn(viewModelScope)
//    }
//
//    /**
//     * 连接到 WebSocket
//     */
//    fun connectToSocket() {
//        Log.d(TAG, "Connecting to Phoenix socket...")
//        // Get auth token if available
//        val sharedPref = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
//        val authToken = sharedPref.getString("auth_token", null)
//        if (authToken != null) {
//            Log.d(TAG, "Using stored auth token for connection")
//        } else {
//            Log.d(TAG, "No auth token available")
//        }
//
//        client.connect(authToken)
//        isConnected = true
//        connectionStatus = "Socket connected"
//    }
//
//    /**
//     * 加入频道
//     */
//    fun joinChannel() {
//        if (!isConnected || userId == null) {
//            connectionStatus = "Cannot join: not connected or no user ID"
//            Log.e(TAG, connectionStatus)
//            return
//        }
//
//        // Get auth token for channel authentication
//        val sharedPref = context.getSharedPreferences("flix_prefs", Context.MODE_PRIVATE)
//        val authToken = sharedPref.getString("auth_token", null)
//
//        Log.d(TAG, "Joining channel: $channelName")
//
//        viewModelScope.launch {
//            try {
//                val response = client.joinChannel(channelName, authToken)
//                Log.d(TAG, "Successfully joined channel: $response")
//                isJoined = true
//                connectionStatus = "Channel joined"
//
//                // Add a system message
//                val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
//                val newMessage = MessageItem(
//                    topic = channelName,
//                    event = "system",
//                    payload = "Channel joined successfully",
//                    timestamp = timestamp
//                )
//                _messages.add(0, newMessage)
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to join channel", e)
//                connectionStatus = "Failed to join: ${e.message}"
//            }
//        }
//    }
//
//    /**
//     * 发送消息
//     */
//    fun sendMessage(event: String, text: String) {
//        if (!isJoined) {
//            connectionStatus = "Cannot send: not joined to channel"
//            Log.e(TAG, connectionStatus)
//            return
//        }
//
//        val payload = JSONObject().apply {
//            put("message", text)
//        }
//
//        Log.d(TAG, "Sending message: $event - $payload")
//        viewModelScope.launch {
//            try {
//                val response = client.push(channelName, event, payload)
//                Log.d(TAG, "Message sent successfully: $response")
//                connectionStatus = "Message sent"
//            } catch (e: Exception) {
//                Log.e(TAG, "Failed to send message", e)
//                connectionStatus = "Send failed: ${e.message}"
//            }
//        }
//    }
//
//    /**
//     * 离开频道
//     */
//    fun leaveChannel() {
//        if (isJoined) {
//            viewModelScope.launch {
//                try {
//                    val response = client.leaveChannel(channelName)
//                    Log.d(TAG, "Successfully left channel: $response")
//                    isJoined = false
//                    connectionStatus = "Channel left"
//                } catch (e: Exception) {
//                    Log.e(TAG, "Failed to leave channel", e)
//                    connectionStatus = "Leave failed: ${e.message}"
//                }
//            }
//        }
//    }
//
//    /**
//     * 断开连接
//     */
//    fun disconnect() {
//        leaveChannel()
//        client.disconnect()
//        isConnected = false
//        connectionStatus = "Disconnected"
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        disconnect()
//    }
//}
//
//// Test message data class
//data class MessageItem(
//    val topic: String, val event: String, val payload: String, val timestamp: String
//)
//
//// Factory for TestMessageViewModel
//class TestMessageViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(TestMessageViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST") return TestMessageViewModel(context) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MessageTestScreen(navController: NavController) {
//    val context = LocalContext.current
//    val viewModel: TestMessageViewModel = viewModel(
//        factory = TestMessageViewModelFactory(context)
//    )
//    val coroutineScope = rememberCoroutineScope()
//
//    var messageText by remember { mutableStateOf("") }
//
//    DisposableEffect(Unit) {
//        onDispose {
//            viewModel.disconnect()
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Phoenix WebSocket 测试") },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = Color.White
//                )
//            )
//        }) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            // Connection status card
//            Card(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp)
//                ) {
//                    Text(
//                        "套接字状态: ${if (viewModel.isConnected) "已连接" else "未连接"}",
//                        style = MaterialTheme.typography.titleMedium
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        "频道状态: ${if (viewModel.isJoined) "已加入" else "未加入"}",
//                        style = MaterialTheme.typography.titleMedium
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Text(
//                        "状态消息: ${viewModel.connectionStatus}",
//                        style = MaterialTheme.typography.bodyMedium
//                    )
//                }
//            }
//
//            // Connection controls
//            Row(
//                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                Button(
//                    onClick = { viewModel.connectToSocket() }, enabled = !viewModel.isConnected
//                ) {
//                    Text("连接")
//                }
//
//                Button(
//                    onClick = { viewModel.joinChannel() },
//                    enabled = viewModel.isConnected && !viewModel.isJoined
//                ) {
//                    Text("加入频道")
//                }
//
//                Button(
//                    onClick = { viewModel.leaveChannel() }, enabled = viewModel.isJoined
//                ) {
//                    Text("离开频道")
//                }
//            }
//
//            // Message input
//            OutlinedTextField(
//                value = messageText,
//                onValueChange = { messageText = it },
//                label = { Text("消息内容") },
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            // Send controls
//            Row(
//                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                Button(
//                    onClick = {
//                        viewModel.sendMessage("new_msg", messageText)
//                        messageText = ""
//                    }, enabled = viewModel.isJoined && messageText.isNotEmpty()
//                ) {
//                    Text("发送消息")
//                }
//
//                Button(
//                    onClick = {
//                        viewModel.sendMessage("ping", "ping")
//                    }, enabled = viewModel.isJoined
//                ) {
//                    Text("发送 Ping")
//                }
//            }
//
//            // Message list header
//            Text(
//                "消息列表 (${viewModel.messages.size}):",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.padding(vertical = 8.dp)
//            )
//
//            // Messages
//            LazyColumn(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .weight(1f)
//                    .background(Color.LightGray.copy(alpha = 0.2f))
//            ) {
//                items(viewModel.messages) { message ->
//                    Card(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(8.dp)
//                    ) {
//                        Column(modifier = Modifier.padding(12.dp)) {
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                horizontalArrangement = Arrangement.SpaceBetween
//                            ) {
//                                Text(
//                                    text = "[${message.event}]",
//                                    style = MaterialTheme.typography.bodyLarge,
//                                    color = RoseRed
//                                )
//                                Text(
//                                    text = message.timestamp,
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = Color.Gray
//                                )
//                            }
//
//                            Spacer(modifier = Modifier.height(4.dp))
//
//                            Text(
//                                text = "Topic: ${message.topic}",
//                                style = MaterialTheme.typography.bodyMedium
//                            )
//
//                            Spacer(modifier = Modifier.height(4.dp))
//
//                            Text(
//                                text = "Payload: ${message.payload}",
//                                style = MaterialTheme.typography.bodyMedium
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
