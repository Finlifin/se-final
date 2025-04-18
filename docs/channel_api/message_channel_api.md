# Flix 消息系统 API 文档

本文档详细介绍了Flix应用的消息系统API，供Android端使用Jetpack Compose开发的前端团队参考。

## 目录

1. [连接建立](#1-连接建立)
2. [消息格式](#2-消息格式)
3. [事件类型](#3-事件类型)
4. [API接口](#4-api接口)
5. [数据模型](#5-数据模型)
6. [错误处理](#6-错误处理)
7. [最佳实践](#7-最佳实践)
8. [示例代码](#8-示例代码)

## 1. 连接建立

### WebSocket连接

消息系统基于Phoenix Channels构建，使用WebSocket协议进行通信。

```
ws://api.example.com/socket/websocket
```

### 身份验证

连接时需要提供JWT令牌进行身份验证：

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 加入频道

每个用户需要加入自己的专属频道：

```
user:{user_id}
```

示例：加入ID为"cbd3dc78-c1cb-4b9b-a126-592c235d25c1"的用户频道

```json
{
  "topic": "user:cbd3dc78-c1cb-4b9b-a126-592c235d25c1",
  "event": "phx_join",
  "payload": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "ref": "1"
}
```

## 2. 消息格式

所有消息遵循以下格式：

```json
{
  "topic": "频道名称",
  "event": "事件名称",
  "payload": {
    // 事件相关数据
  },
  "ref": "消息引用ID"
}
```

## 3. 事件类型

### 服务器事件

服务器可能推送以下事件：

| 事件名称 | 描述 |
|---------|------|
| `event` | 新事件（新消息、状态更新等） |
| `phx_reply` | 对客户端请求的回复 |
| `phx_error` | 连接错误 |
| `phx_close` | 连接关闭 |

### 客户端事件

客户端可以发送以下事件：

| 事件名称 | 描述 |
|---------|------|
| `phx_join` | 加入频道 |
| `phx_leave` | 离开频道 |
| `send_message` | 发送消息 |
| `sync` | 同步消息和事件 |
| `mark_read` | 标记已读 |
| `withdraw_message` | 撤回消息 |
| `get_conversation_history` | 获取会话历史 |
| `create_conversation` | 创建会话 |
| `get_conversations` | 获取会话列表 |

## 4. API接口

### 4.1 发送消息

**请求：**

```json
{
  "topic": "user:{user_id}",
  "event": "send_message",
  "payload": {
    "client_message_id": "336ab20a-f534-408e-ac06-61c5816f92b0",
    "client_timestamp": "2025-04-10T10:30:00.000Z",
    "conversation_id": "10718b6a-6741-4441-ace5-7ce83fa0f9df",
    "content": [
      {
        "type": "text",
        "payload": "你好，这是一条测试消息"
      }
    ],
    "message_type": "private_message"
  },
  "ref": "1"
}
```

**响应：**

```json
{
  "topic": "user:{user_id}",
  "event": "phx_reply",
  "payload": {
    "status": "ok",
    "response": {
      "client_message_id": "336ab20a-f534-408e-ac06-61c5816f92b0",
      "message_id": "dcb68178-25f0-4ec0-9be4-da0c84985c8d",
      "server_timestamp": "2025-04-16T09:46:39.575558Z",
      "status": "sent"
    }
  },
  "ref": "1"
}
```

### 4.2 同步数据

**请求：**

```json
{
  "topic": "user:{user_id}",
  "event": "sync",
  "payload": {
    "last_sync_timestamp": "2025-04-15T00:00:00.000Z"
  },
  "ref": "2"
}
```

**响应：**

```json
{
  "topic": "user:{user_id}",
  "event": "phx_reply",
  "payload": {
    "status": "ok",
    "response": {
      "events": [
        {
          "id": "67f50bcf-03b5-42d6-94e7-38086869570b",
          "event_type": "new_message",
          "payload": {
            // 消息数据
          },
          "event_timestamp": "2025-04-16T09:46:39.575558Z",
          "target_user_id": "cbd3dc78-c1cb-4b9b-a126-592c235d25c1",
          "inserted_at": "2025-04-16T09:46:39"
        }
      ],
      "new_last_sync_timestamp": "2025-04-16T09:46:39.575558Z"
    }
  },
  "ref": "2"
}
```

### 4.3 标记消息已读

**请求：**

```json
{
  "topic": "user:{user_id}",
  "event": "mark_read",
  "payload": {
    "conversation_id": "10718b6a-6741-4441-ace5-7ce83fa0f9df",
    "last_read_message_id": "dcb68178-25f0-4ec0-9be4-da0c84985c8d"
  },
  "ref": "3"
}
```

**响应：**

```json
{
  "topic": "user:{user_id}",
  "event": "phx_reply",
  "payload": {
    "status": "ok",
    "response": {
      "updated_count": 5
    }
  },
  "ref": "3"
}
```

### 4.4 撤回消息

**请求：**

```json
{
  "topic": "user:{user_id}",
  "event": "withdraw_message",
  "payload": {
    "message_id": "dcb68178-25f0-4ec0-9be4-da0c84985c8d"
  },
  "ref": "4"
}
```

**响应：**

```json
{
  "topic": "user:{user_id}",
  "event": "phx_reply",
  "payload": {
    "status": "ok",
    "response": {
      "status": "withdrawn"
    }
  },
  "ref": "4"
}
```

### 4.5 获取会话历史

**请求：**

```json
{
  "topic": "user:{user_id}",
  "event": "get_conversation_history",
  "payload": {
    "conversation_id": "10718b6a-6741-4441-ace5-7ce83fa0f9df",
    "limit": 20,
    "before": "2025-04-16T00:00:00.000Z"
  },
  "ref": "5"
}
```

**响应：**

```json
{
  "topic": "user:{user_id}",
  "event": "phx_reply",
  "payload": {
    "status": "ok",
    "response": {
      "messages": [
        // 消息列表
      ]
    }
  },
  "ref": "5"
}
```

### 4.6 创建会话

**请求：**

```json
{
  "topic": "user:{user_id}",
  "event": "create_conversation",
  "payload": {
    "type": "private",
    "participant_ids": ["cbd3dc78-c1cb-4b9b-a126-592c235d25c1"]
  },
  "ref": "6"
}
```

**响应：**

```json
{
  "topic": "user:{user_id}",
  "event": "phx_reply",
  "payload": {
    "status": "ok",
    "response": {
      "conversation": {
        "id": "a0a12345-1234-1234-1234-123456789abc",
        "conversation_id": "private:cbd3dc78-c1cb-4b9b-a126-592c235d25c1:def45678-5678-5678-5678-5678abcdef12",
        "type": "private",
        "participant_ids": [
          "cbd3dc78-c1cb-4b9b-a126-592c235d25c1",
          "def45678-5678-5678-5678-5678abcdef12"
        ],
        "updated_at": "2025-04-16T09:46:39.575558Z",
        "inserted_at": "2025-04-16T09:46:39.575558Z"
      }
    }
  },
  "ref": "6"
}
```

### 4.7 获取会话列表

**请求：**

```json
{
  "topic": "user:{user_id}",
  "event": "get_conversations",
  "payload": {
    "limit": 20
  },
  "ref": "7"
}
```

**响应：**

```json
{
  "topic": "user:{user_id}",
  "event": "phx_reply",
  "payload": {
    "status": "ok",
    "response": [
      {
        "conversation": {
          "id": "a0a12345-1234-1234-1234-123456789abc",
          "conversation_id": "private:cbd3dc78-c1cb-4b9b-a126-592c235d25c1:def45678-5678-5678-5678-5678abcdef12",
          "type": "private",
          "participant_ids": [
            "cbd3dc78-c1cb-4b9b-a126-592c235d25c1",
            "def45678-5678-5678-5678-5678abcdef12"
          ],
          "last_message_id": "dcb68178-25f0-4ec0-9be4-da0c84985c8d",
          "last_message_content": "你好，这是一条测试消息",
          "last_message_timestamp": "2025-04-16T09:46:39.575558Z",
          "updated_at": "2025-04-16T09:46:39.575558Z",
          "inserted_at": "2025-04-16T09:46:39.575558Z"
        },
        "unread_count": 3,
        "is_pinned": false,
        "is_muted": false,
        "last_read_message_id": "abcde123-1234-1234-1234-123456789abc"
      }
    ]
  },
  "ref": "7"
}
```

## 5. 数据模型

### 5.1 消息内容格式

消息内容采用数组格式，每个元素包含类型与载荷：

```json
[
  {
    "type": "text",
    "payload": "文本内容"
  },
  {
    "type": "image",
    "payload": "https://example.com/image.jpg"
  }
]
```

支持的内容类型：

| 类型 | 描述 | 载荷格式 |
|------|------|---------|
| `text` | 文本消息 | 字符串 |
| `image` | 图片消息 | 图片URL |
| `product` | 商品信息 | 商品对象 |
| `order` | 订单信息 | 订单对象 |
| `comment` | 评论信息 | 评论对象 |
| `like` | 点赞信息 | 点赞对象 |
| `favorite` | 收藏信息 | 收藏对象 |
| `system` | 系统消息 | 系统消息对象 |

### 5.2 消息状态

| 状态 | 描述 |
|------|------|
| `sending` | 发送中（仅客户端） |
| `sent` | 已发送 |
| `unread` | 未读 |
| `read` | 已读 |
| `withdrawn` | 已撤回 |
| `deleted` | 已删除 |

### 5.3 消息类型

| 类型 | 描述 |
|------|------|
| `system_notification` | 系统通知 |
| `system_announcement` | 系统公告 |
| `interaction` | 互动消息 |
| `private_message` | 私信 |

## 6. 错误处理

### 6.1 错误响应格式

```json
{
  "topic": "user:{user_id}",
  "event": "phx_reply",
  "payload": {
    "status": "error",
    "response": {
      "reason": "错误原因",
      "errors": {
        // 具体错误字段
      }
    }
  },
  "ref": "1"
}
```

### 6.2 常见错误码

| 错误原因 | 描述 |
|---------|------|
| `unauthorized` | 未授权或Token无效 |
| `not_found` | 资源不存在 |
| `invalid_format` | 请求格式无效 |
| `time_expired` | 操作超时（如撤回消息） |
| `missing_required_parameters` | 缺少必要参数 |

## 7. 最佳实践

### 7.1 消息同步策略

1. **应用启动同步**：
   - 应用启动时，发送`sync`请求，使用上次同步时间戳
   - 处理返回的所有事件
   - 更新本地同步时间戳

2. **定期同步**：
   - 建议每60秒执行一次同步
   - 每次网络恢复后也应执行同步

3. **推送通知处理**：
   - 收到推送通知时，执行一次同步

### 7.2 消息发送与状态管理

1. **乐观更新**：
   - 发送消息前，先在UI中显示"正在发送"状态
   - 生成客户端消息ID用于跟踪

2. **重试机制**：
   - 发送失败时，自动重试3次
   - 每次重试间隔递增（2秒、5秒、10秒）

3. **离线消息队列**：
   - 离线时将消息加入队列
   - 恢复连接后自动发送

### 7.3 UTF-8编码处理

所有消息内容必须使用有效的UTF-8编码，服务器会验证并清理无效字符。注意处理包含特殊字符或emoji的文本消息。

## 8. 示例代码

### 8.1 Kotlin中使用Phoenix Socket

```kotlin
import org.phoenixframework.Socket
import org.phoenixframework.Channel

// 初始化Socket连接
val socket = Socket("ws://api.example.com/socket/websocket")
socket.connect()

// 加入用户频道
val userChannel = socket.channel("user:$userId")

val params = mapOf("token" to authToken)
userChannel.join(params)
    .receive("ok") { response ->
        Log.d("Socket", "Successfully joined")
    }
    .receive("error") { response ->
        Log.e("Socket", "Failed to join", response.toString())
    }

// 监听事件
userChannel.on("event") { message ->
    val eventType = message.payload.get("event_type") as String
    when (eventType) {
        "new_message" -> handleNewMessage(message)
        "message_status_update" -> handleStatusUpdate(message)
        // 其他事件处理...
    }
}

// 发送消息
fun sendMessage(conversationId: String, content: String) {
    val clientMessageId = UUID.randomUUID().toString()
    val payload = mapOf(
        "client_message_id" to clientMessageId,
        "client_timestamp" to ISO8601DateFormat.format(Date()),
        "conversation_id" to conversationId,
        "content" to listOf(
            mapOf(
                "type" to "text",
                "payload" to content
            )
        ),
        "message_type" to "private_message"
    )
    
    userChannel.push("send_message", payload)
        .receive("ok") { response ->
            // 消息发送成功处理
        }
        .receive("error") { error ->
            // 错误处理
        }
}
```

### 8.2 Compose UI中的消息列表实现

```kotlin
@Composable
fun ConversationScreen(viewModel: ConversationViewModel) {
    val messages by viewModel.messages.collectAsState()
    val scrollState = rememberLazyListState()
    
    Column {
        // 消息列表
        LazyColumn(
            state = scrollState,
            reverseLayout = true
        ) {
            items(messages) { message ->
                MessageItem(
                    message = message,
                    isOwnMessage = message.senderId == viewModel.currentUserId,
                    onLongPress = { viewModel.showMessageOptions(message) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        // 消息输入框
        MessageComposer(
            onMessageSent = { content ->
                viewModel.sendMessage(content)
            }
        )
    }
}

@Composable
fun MessageItem(
    message: Message,
    isOwnMessage: Boolean,
    onLongPress: () -> Unit
) {
    val backgroundColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val alignment = if (isOwnMessage) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }
    
    Box(
        contentAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = 280.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                }
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                when (message.content.firstOrNull()?.type) {
                    "text" -> Text(
                        text = message.content.first().payload as String,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    "image" -> {
                        AsyncImage(
                            model = message.content.first().payload as String,
                            contentDescription = "Image message",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // 其他类型处理...
                }
                
                Text(
                    text = formatTimestamp(message.clientTimestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
```

## 安全注意事项

1. 确保所有消息内容进行适当的转义和验证，防止XSS攻击
2. 所有敏感数据在传输前加密
3. 定期刷新Token
4. 实现消息本地加密存储
5. 实现安全的错误处理，避免泄露敏感信息

## 性能优化

1. 使用分页加载历史消息
2. 实现高效的本地缓存机制
3. 合理设置同步频率，避免过度请求
4. 图片等大型媒体文件使用延迟加载
5. 使用增量同步减少数据传输

如有任何问题，请联系后端团队 `backend@flix.example.com`。