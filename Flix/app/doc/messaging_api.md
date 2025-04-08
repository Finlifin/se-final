# 二手交易平台信箱系统 API 文档

本文档详细描述了二手交易平台信箱系统的 REST API 接口和 WebSocket 通讯协议，供 Jetpack Compose 客户端开发团队参考实现。

## 目录

1. 认证方式
2. REST API 接口
3. WebSocket 实时通信
4. 数据模型
5. 错误处理
6. 集成示例

## 1. 认证方式

所有需要认证的 API 请求必须在 HTTP Header 中包含以下认证信息：

```
Authorization: Bearer {token}
```

其中 `{token}` 是用户登录后获得的 JWT Token。

## 2. REST API 接口

### 2.1 获取消息列表

获取当前用户的消息列表，支持分页、消息类型和状态过滤。

**请求**

```
GET /api/v1/messages
```

**查询参数**

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| limit | Integer | 否 | 每页数量，默认 50 |
| offset | Integer | 否 | 偏移量，默认 0 |
| message_type | String | 否 | 消息类型过滤：`system_notification`、`system_announcement`、`interaction`、`private_message` |
| status | String | 否 | 消息状态过滤：`unread`、`read` |

**响应**

```json
{
  "data": [
    {
      "id": "message-uuid-1",
      "sender_id": "user-uuid-1",
      "receiver_id": "current-user-uuid",
      "content": {
        "text": "消息内容",
        "image_urls": ["https://example.com/image1.jpg"],
        "item_id": "product-uuid-1",
        "title": "新消息标题",
        "deep_link": "app://products/product-uuid-1"
      },
      "content_type": "text",
      "message_type": "private_message",
      "status": "unread",
      "reference_id": "optional-reference-uuid",
      "inserted_at": "2023-12-01T10:00:00Z",
      "updated_at": "2023-12-01T10:00:00Z"
    },
    // ...更多消息
  ]
}
```

### 2.2 获取单个消息详情

**请求**

```
GET /api/v1/messages/{id}
```

**路径参数**

| 参数 | 类型 | 描述 |
|------|------|------|
| id | String | 消息 UUID |

**响应**

```json
{
  "data": {
    "id": "message-uuid-1",
    "sender_id": "user-uuid-1",
    "receiver_id": "current-user-uuid",
    "content": {
      "text": "消息内容",
      "image_urls": ["https://example.com/image1.jpg"],
      "item_id": "product-uuid-1",
      "title": "消息标题",
      "deep_link": "app://products/product-uuid-1"
    },
    "content_type": "text",
    "message_type": "private_message",
    "status": "unread",
    "reference_id": "optional-reference-uuid",
    "inserted_at": "2023-12-01T10:00:00Z",
    "updated_at": "2023-12-01T10:00:00Z"
  }
}
```

### 2.3 创建新消息

发送一条新消息给指定用户。

**请求**

```
POST /api/v1/messages
```

**请求体**

```json
{
  "receiver_id": "target-user-uuid",
  "content": {
    "text": "Hello, I'm interested in your product",
    "item_id": "product-uuid-1"
  },
  "content_type": "text",
  "message_type": "private_message",
  "reference_id": "optional-reference-uuid"
}
```

**响应**

```json
{
  "data": {
    "id": "new-message-uuid",
    "sender_id": "current-user-uuid",
    "receiver_id": "target-user-uuid",
    "content": {
      "text": "Hello, I'm interested in your product",
      "item_id": "product-uuid-1"
    },
    "content_type": "text",
    "message_type": "private_message",
    "status": "unread",
    "reference_id": "optional-reference-uuid",
    "inserted_at": "2023-12-01T10:05:00Z",
    "updated_at": "2023-12-01T10:05:00Z"
  }
}
```

### 2.4 标记单条消息为已读

**请求**

```
PUT /api/v1/messages/{id}/read
```

**路径参数**

| 参数 | 类型 | 描述 |
|------|------|------|
| id | String | 消息 UUID |

**响应**

```json
{
  "data": {
    "id": "message-uuid-1",
    "sender_id": "user-uuid-1",
    "receiver_id": "current-user-uuid",
    "content": {
      "text": "消息内容"
    },
    "content_type": "text",
    "message_type": "private_message",
    "status": "read",
    "reference_id": "optional-reference-uuid",
    "inserted_at": "2023-12-01T10:00:00Z",
    "updated_at": "2023-12-01T10:10:00Z"
  }
}
```

### 2.5 批量标记消息为已读

**请求**

```
PUT /api/v1/messages/batch_read
```

**请求体**

```json
{
  "message_ids": ["message-uuid-1", "message-uuid-2", "message-uuid-3"]
}
```

**响应**

```json
{
  "data": {
    "marked_count": 3
  }
}
```

### 2.6 标记所有消息为已读

**请求**

```
PUT /api/v1/messages/read_all
```

**响应**

```json
{
  "data": {
    "marked_count": 25
  }
}
```

### 2.7 获取未读消息统计

获取当前用户的未读消息数量，按类型分类。

**请求**

```
GET /api/v1/messages/unread_count
```

**响应**

```json
{
  "data": {
    "total": 15,
    "system_notification": 3,
    "system_announcement": 1,
    "interaction": 5,
    "private_message": 6
  }
}
```

### 2.8 删除消息

**请求**

```
DELETE /api/v1/messages/{id}
```

**路径参数**

| 参数 | 类型 | 描述 |
|------|------|------|
| id | String | 消息 UUID |

**响应**

成功时返回状态码 204 (No Content)，无响应体。

### 2.9 消息同步

同步指定时间之后的消息更新。

**请求**

```
POST /api/v1/messages/sync
```

**请求体**

```json
{
  "last_sync_time": "2023-12-01T10:00:00Z",
  "user_id": "current-user-uuid"
}
```

**响应**

```json
{
  "data": {
    "messages": [
      {
        "id": "message-uuid-1",
        "sender_id": "user-uuid-1",
        "receiver_id": "current-user-uuid",
        "content": {
          "text": "新消息内容"
        },
        "content_type": "text",
        "message_type": "private_message",
        "status": "unread",
        "reference_id": "optional-reference-uuid",
        "inserted_at": "2023-12-01T10:05:00Z",
        "updated_at": "2023-12-01T10:05:00Z"
      },
      // ...更多消息
    ],
    "sync_time": "2023-12-01T10:15:00Z"
  }
}
```

### 2.10 获取系统公告列表

**请求**

```
GET /api/v1/announcements
```

**查询参数**

| 参数 | 类型 | 必填 | 描述 |
|------|------|------|------|
| limit | Integer | 否 | 每页数量，默认 10 |
| offset | Integer | 否 | 偏移量，默认 0 |

**响应**

```json
{
  "data": [
    {
      "id": "announcement-uuid-1",
      "title": "系统维护通知",
      "content": "系统将于周末进行维护升级...",
      "created_at": "2023-12-01T09:00:00Z"
    },
    // ...更多公告
  ]
}
```

### 2.11 获取单个系统公告

**请求**

```
GET /api/v1/announcements/{id}
```

**路径参数**

| 参数 | 类型 | 描述 |
|------|------|------|
| id | String | 公告 UUID |

**响应**

```json
{
  "data": {
    "id": "announcement-uuid-1",
    "title": "系统维护通知",
    "content": "系统将于周末进行维护升级，届时平台将暂停服务4小时...",
    "created_at": "2023-12-01T09:00:00Z",
    "updated_at": "2023-12-01T09:00:00Z"
  }
}
```

## 3. WebSocket 实时通信

### 3.1 连接与认证

客户端需要建立 WebSocket 连接并加入用户专属频道：

```
ws://your-api-domain.com/socket/websocket
```

**连接参数**

```javascript
{
  "token": "用户JWT令牌"
}
```

**加入用户频道**

```javascript
// 发送
{
  "topic": "user:user-uuid",
  "event": "phx_join",
  "payload": {
    "token": "用户JWT令牌"
  },
  "ref": "1"
}

// 成功响应
{
  "topic": "user:user-uuid",
  "ref": "1",
  "payload": {},
  "status": "ok"
}
```

### 3.2 发送私信

**客户端发送**

```javascript
{
  "topic": "user:user-uuid",
  "event": "send_private_message",
  "payload": {
    "receiver_id": "target-user-uuid",
    "content": {
      "text": "你好，我对您的商品感兴趣",
      "image_urls": ["https://example.com/image1.jpg"],
      "item_id": "product-uuid-1",
      "title": "询问商品",
      "deep_link": "app://products/product-uuid-1"
    },
    "content_type": "text",
    "reference_id": "optional-reference-uuid"
  },
  "ref": "2"
}
```

**服务器响应**

```javascript
{
  "topic": "user:user-uuid",
  "ref": "2",
  "payload": {
    "message": {
      "id": "new-message-uuid",
      "sender_id": "user-uuid",
      "receiver_id": "target-user-uuid",
      "content": {
        "text": "你好，我对您的商品感兴趣",
        "image_urls": ["https://example.com/image1.jpg"],
        "item_id": "product-uuid-1",
        "title": "询问商品",
        "deep_link": "app://products/product-uuid-1"
      },
      "content_type": "text",
      "message_type": "private_message",
      "status": "unread",
      "reference_id": "optional-reference-uuid",
      "inserted_at": "2023-12-01T10:20:00Z",
      "updated_at": "2023-12-01T10:20:00Z"
    }
  },
  "status": "ok"
}
```

### 3.3 确认消息已读

**单条消息确认**

```javascript
// 发送
{
  "topic": "user:user-uuid",
  "event": "ack_message",
  "payload": {
    "message_id": "message-uuid-1"
  },
  "ref": "3"
}

// 响应
{
  "topic": "user:user-uuid",
  "ref": "3",
  "status": "ok"
}
```

**批量消息确认**

```javascript
// 发送
{
  "topic": "user:user-uuid",
  "event": "ack_messages",
  "payload": {
    "message_ids": ["message-uuid-1", "message-uuid-2", "message-uuid-3"]
  },
  "ref": "4"
}

// 响应
{
  "topic": "user:user-uuid",
  "ref": "4",
  "payload": {
    "status": "success",
    "count": 3
  },
  "status": "ok"
}
```

### 3.4 获取历史消息

```javascript
// 发送
{
  "topic": "user:user-uuid",
  "event": "get_history",
  "payload": {
    "before": "2023-12-01T10:00:00Z", // 可选，获取此时间之前的消息
    "limit": 20, // 可选，默认20
    "message_type": "private_message" // 可选，消息类型过滤
  },
  "ref": "5"
}

// 响应
{
  "topic": "user:user-uuid",
  "ref": "5",
  "payload": {
    "messages": [
      // 消息列表，格式同上
    ]
  },
  "status": "ok"
}
```

### 3.5 同步离线消息

```javascript
// 发送
{
  "topic": "user:user-uuid",
  "event": "sync_messages",
  "payload": {
    "since": "2023-12-01T10:00:00Z"
  },
  "ref": "6"
}

// 响应
{
  "topic": "user:user-uuid",
  "ref": "6",
  "payload": {
    "messages": [
      // 消息列表，格式同上
    ],
    "sync_time": "2023-12-01T10:25:00Z"
  },
  "status": "ok"
}
```

### 3.6 服务器推送事件

**新消息推送**

```javascript
{
  "topic": "user:user-uuid",
  "event": "new_message",
  "payload": {
    "message": {
      "id": "message-uuid-1",
      "sender_id": "other-user-uuid",
      "receiver_id": "user-uuid",
      "content": {
        "text": "新消息内容"
      },
      "content_type": "text",
      "message_type": "private_message",
      "status": "unread",
      "reference_id": null,
      "inserted_at": "2023-12-01T10:30:00Z",
      "updated_at": "2023-12-01T10:30:00Z"
    }
  }
}
```

**消息状态变更推送**

```javascript
{
  "topic": "user:user-uuid",
  "event": "message_status_changed",
  "payload": {
    "message_id": "message-uuid-1",
    "status": "read"
  }
}
```

**批量消息标记已读推送**

```javascript
{
  "topic": "user:user-uuid",
  "event": "messages_marked_read",
  "payload": {
    "message_ids": ["message-uuid-1", "message-uuid-2"]
  }
}
```

**未读数更新推送**

```javascript
{
  "topic": "user:user-uuid",
  "event": "unread_count_update",
  "payload": {
    "counts": {
      "total": 10,
      "system_notification": 2,
      "system_announcement": 1,
      "interaction": 3,
      "private_message": 4
    }
  }
}
```

## 4. 数据模型

### 4.1 消息 (Message)

| 字段 | 类型 | 描述 |
|------|------|------|
| id | UUID | 消息唯一标识符 |
| sender_id | UUID | 发送者ID，可为空（系统消息） |
| receiver_id | UUID | 接收者ID |
| content | Map | 消息内容，根据消息类型有不同的字段 |
| content_type | Enum | 内容类型：`text`, `image`, `product`, `order`, `comment`, `like`, `favorite`, `system` |
| message_type | Enum | 消息类型：`system_notification`, `system_announcement`, `interaction`, `private_message` |
| status | Enum | 消息状态：`unread`, `read`, `deleted` |
| reference_id | UUID | 关联ID，如订单ID、商品ID等 |
| inserted_at | DateTime | 创建时间 |
| updated_at | DateTime | 更新时间 |

### 4.2 内容类型示例

**私信文本消息**
```json
{
  "text": "你好，我对你的商品感兴趣",
  "title": "商品询问", // 可选
  "deep_link": "app://chat/user-uuid-1" // 可选，应用内跳转链接
}
```

**商品互动消息**
```json
{
  "text": "有人对你的商品发表了评论",
  "title": "新评论通知",
  "item_id": "product-uuid-1",
  "comment_id": "comment-uuid-1",
  "interaction_type": "new_comment",
  "deep_link": "app://products/product-uuid-1/comments"
}
```

**系统通知**
```json
{
  "text": "您的账户已通过实名认证",
  "title": "账户认证成功",
  "deep_link": "app://settings/account"
}
```

**系统公告**
```json
{
  "text": "平台将于12月25日进行系统升级，届时将暂停服务4小时",
  "title": "系统维护通知",
  "deep_link": "app://announcements/announcement-uuid-1"
}
```

## 5. 错误处理

### 5.1 HTTP 状态码

| 状态码 | 描述 |
|------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 无内容（删除成功） |
| 400 | 请求参数错误 |
| 401 | 认证失败 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 422 | 验证错误 |
| 500 | 服务器内部错误 |

### 5.2 错误响应格式

```json
{
  "error": "错误描述信息",
  "details": {
    // 可选，详细错误信息
  }
}
```

### 5.3 WebSocket 错误响应

```javascript
{
  "topic": "user:user-uuid",
  "ref": "123",
  "payload": {
    "reason": "错误原因",
    "details": {
      // 详细错误信息
    }
  },
  "status": "error"
}
```

## 6. 集成示例

### 6.1 初始化 WebSocket 连接

```kotlin
// 使用适当的库，如 Scarlet 或 OkHttp WebSocket
val socket = PhoenixSocket(
    "ws://your-api-domain.com/socket/websocket",
    mapOf("token" to userToken)
)

socket.connect()

// 加入用户频道
val userChannel = socket.channel("user:${userId}")
userChannel.join()
    .receive("ok") { response ->
        Log.d("WebSocket", "成功连接到用户频道")
    }
    .receive("error") { response ->
        Log.e("WebSocket", "连接用户频道失败: $response")
    }
```

### 6.2 监听新消息

```kotlin
userChannel.on("new_message") { response ->
    val message = response.payload.getJSONObject("message")
    // 处理新消息，例如显示通知、更新UI等
}

userChannel.on("unread_count_update") { response ->
    val counts = response.payload.getJSONObject("counts")
    // 更新未读消息计数UI
}
```

### 6.3 发送私信

```kotlin
val payload = mapOf(
    "receiver_id" to targetUserId,
    "content" to mapOf(
        "text" to "你好，我对您的商品感兴趣",
        "item_id" to productId
    ),
    "content_type" to "text"
)

userChannel.push("send_private_message", payload)
    .receive("ok") { response ->
        // 消息发送成功
    }
    .receive("error") { error ->
        // 消息发送失败
    }
```

### 6.4 标记消息已读

```kotlin
// 单条消息标记已读
userChannel.push("ack_message", mapOf("message_id" to messageId))

// 批量标记已读
userChannel.push("ack_messages", mapOf("message_ids" to listOf(messageId1, messageId2)))
```

### 6.5 获取消息列表

```kotlin
// 使用适当的HTTP客户端库，如Retrofit
interface MessageApi {
    @GET("api/v1/messages")
    suspend fun getMessages(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("message_type") messageType: String? = null,
        @Query("status") status: String? = null
    ): Response<MessageListResponse>
}

// 示例调用
viewModelScope.launch {
    try {
        val response = messageApi.getMessages(
            limit = 20,
            offset = 0,
            messageType = "private_message"
        )
        if (response.isSuccessful) {
            val messages = response.body()?.data
            // 更新UI显示消息列表
        }
    } catch (e: Exception) {
        // 处理错误
    }
}
```

### 6.6 消息同步流程

**客户端启动时**:
1. 从本地存储获取上次同步时间 `lastSyncTime`
2. 调用REST API的同步端点或使用WebSocket的sync_messages事件获取离线消息
3. 更新本地消息缓存
4. 更新上次同步时间

```kotlin
// 示例同步流程
fun syncMessages() {
    val lastSyncTime = getLastSyncTimeFromPreferences()
    
    viewModelScope.launch {
        try {
            val response = messageApi.syncMessages(
                lastSyncTime = lastSyncTime,
                userId = currentUserId
            )
            
            if (response.isSuccessful) {
                // 处理新消息
                val newMessages = response.body()?.data?.messages
                processNewMessages(newMessages)
                
                // 更新同步时间
                val newSyncTime = response.body()?.data?.sync_time
                if (newSyncTime != null) {
                    saveLastSyncTimeToPreferences(newSyncTime)
                }
            }
        } catch (e: Exception) {
            // 处理错误
        }
    }
}
```

## 注意事项

1. 所有需要认证的请求必须携带有效的JWT令牌
2. WebSocket连接应在用户登录后建立，登出后断开
3. 客户端应保存上次同步时间，以便在重新连接时获取离线消息
4. 消息状态变更应同步到所有已登录的客户端设备
5. 处理发送失败的消息时，考虑重试机制或显示发送状态

---

此API文档涵盖了二手交易平台信箱系统的核心功能，Jetpack Compose客户端开发团队可以基于此实现完整的消息收发、通知管理等功能。如有疑问或需要进一步说明，请与后端团队联系。