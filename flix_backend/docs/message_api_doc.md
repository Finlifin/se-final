# 即时通信系统 API 文档

## 基础信息

- **基础URL**: `/api/v1`
- **认证方式**: Bearer Token
- **响应格式**: JSON
- **版本**: 1.0.0

## 概述

本文档详细描述了Flix平台即时通信系统的API接口设计。该系统支持私信、系统通知、消息撤回、已读确认等功能，并通过WebSocket实现实时消息推送。

## 数据模型

### 消息(Message)

```json
{
  "id": "uuid",
  "client_message_id": "uuid-client-generated",
  "conversation_id": "string",
  "sender_id": "uuid",
  "receiver_id": "uuid",
  "content": {
    "text": "消息内容",
    "image_urls": ["url1", "url2"],
    "title": "可选标题",
    "deep_link": "app://some_target"
  },
  "content_type": "text | image | product | order | comment | like | favorite | system",
  "message_type": "system_notification | system_announcement | interaction | private_message",
  "status": "sending | sent | unread | read | withdrawn | deleted",
  "reference_id": "uuid",
  "server_timestamp": "2025-04-10T10:30:00.000Z",
  "client_timestamp": "2025-04-10T10:29:58.000Z",
  "inserted_at": "2025-04-10T10:30:00.000Z",
  "updated_at": "2025-04-10T10:30:00.000Z"
}
```

### 会话(Conversation)

```json
{
  "id": "uuid",
  "conversation_id": "string",
  "type": "private | group | system",
  "participant_ids": ["uuid1", "uuid2"],
  "last_message_id": "uuid",
  "last_message_content": "最后一条消息预览",
  "last_message_timestamp": "2025-04-10T10:30:00.000Z",
  "inserted_at": "2025-04-10T10:30:00.000Z",
  "updated_at": "2025-04-10T10:30:00.000Z"
}
```

### 用户会话关系(UserConversation)

```json
{
  "id": "uuid",
  "user_id": "uuid",
  "conversation_id": "string",
  "last_read_message_id": "uuid",
  "unread_count": 5,
  "is_pinned": false,
  "is_muted": false,
  "draft": "草稿内容",
  "inserted_at": "2025-04-10T10:30:00.000Z",
  "updated_at": "2025-04-10T10:30:00.000Z"
}
```

### 事件(Event)

```json
{
  "id": "uuid",
  "event_type": "new_message | message_status_update | message_recalled",
  "payload": {}, // 事件具体内容，根据事件类型不同而不同
  "event_timestamp": "2025-04-10T10:30:00.000Z",
  "target_user_id": "uuid",
  "inserted_at": "2025-04-10T10:30:00.000Z",
  "updated_at": "2025-04-10T10:30:00.000Z"
}
```

## HTTP API 端点

### 1. 获取消息列表

**端点**: `GET /messages`

**描述**: 获取当前用户的消息列表，支持分页、消息类型和状态过滤

**认证**: 必需

**参数**:
- `limit`: 每页记录数，默认50
- `offset`: 起始位置，默认0
- `message_type`: 消息类型过滤，可选值包括`system_notification`、`system_announcement`、`interaction`、`private_message`
- `status`: 消息状态过滤，可选值包括`unread`、`read`、`withdrawn`、`deleted`

**响应**:
```json
{
  "success": true,
  "message": "获取消息列表成功",
  "data": [
    {
      "id": "uuid",
      "client_message_id": "uuid-client-generated",
      "conversation_id": "string",
      "sender_id": "uuid",
      "receiver_id": "uuid",
      "content": {
        "text": "消息内容示例"
      },
      "content_type": "text",
      "message_type": "private_message",
      "status": "unread",
      "server_timestamp": "2025-04-10T10:30:00.000Z",
      "client_timestamp": "2025-04-10T10:29:58.000Z"
    }
    // 更多消息...
  ]
}
```

### 2. 获取单个消息

**端点**: `GET /messages/{id}`

**描述**: 获取指定ID的消息详情

**认证**: 必需

**参数**:
- `id`: 消息ID (路径参数)

**响应**:
```json
{
  "success": true,
  "message": "获取消息成功",
  "data": {
    "id": "uuid",
    "client_message_id": "uuid-client-generated",
    "conversation_id": "string",
    "sender_id": "uuid",
    "receiver_id": "uuid",
    "content": {
      "text": "消息内容示例"
    },
    "content_type": "text",
    "message_type": "private_message",
    "status": "unread",
    "server_timestamp": "2025-04-10T10:30:00.000Z",
    "client_timestamp": "2025-04-10T10:29:58.000Z"
  }
}
```

### 3. 创建消息

**端点**: `POST /messages`

**描述**: 创建新消息

**认证**: 必需

**请求体**:
```json
{
  "receiver_id": "uuid",
  "content": {
    "text": "Hello, this is a test message!"
  },
  "content_type": "text",
  "message_type": "private_message",
  "reference_id": "uuid" // 可选
}
```

**响应**:
```json
{
  "success": true,
  "message": "消息创建成功",
  "data": {
    "id": "uuid",
    "client_message_id": "uuid-generated-by-server",
    "conversation_id": "string",
    "sender_id": "uuid",
    "receiver_id": "uuid",
    "content": {
      "text": "Hello, this is a test message!"
    },
    "content_type": "text",
    "message_type": "private_message",
    "status": "sent",
    "server_timestamp": "2025-04-10T10:30:00.000Z"
  }
}
```

### 4. 标记消息为已读

**端点**: `PUT /messages/{id}/read`

**描述**: 标记单条消息为已读状态

**认证**: 必需

**参数**:
- `id`: 消息ID (路径参数)

**响应**:
```json
{
  "success": true,
  "message": "消息已标记为已读",
  "data": {
    "id": "uuid",
    "status": "read",
    "updated_at": "2025-04-10T10:31:00.000Z"
  }
}
```

### 5. 批量标记消息为已读

**端点**: `PUT /messages/batch_read`

**描述**: 批量标记多条消息为已读状态

**认证**: 必需

**请求体**:
```json
{
  "message_ids": ["uuid1", "uuid2", "uuid3"]
}
```

**响应**:
```json
{
  "success": true,
  "message": "批量标记成功",
  "data": {
    "marked_count": 3
  }
}
```

### 6. 标记所有消息为已读

**端点**: `PUT /messages/read_all`

**描述**: 标记当前用户的所有未读消息为已读状态

**认证**: 必需

**响应**:
```json
{
  "success": true,
  "message": "所有消息已标记为已读",
  "data": {
    "marked_count": 10
  }
}
```

### 7. 获取未读消息数量

**端点**: `GET /messages/unread_count`

**描述**: 获取当前用户未读消息数量，按消息类型分类

**认证**: 必需

**响应**:
```json
{
  "success": true,
  "message": "获取未读消息数量成功",
  "data": {
    "total": 8,
    "system_notification": 2,
    "system_announcement": 1,
    "interaction": 3,
    "private_message": 2
  }
}
```

### 8. 删除消息

**端点**: `DELETE /messages/{id}`

**描述**: 删除指定的消息

**认证**: 必需

**参数**:
- `id`: 消息ID (路径参数)

**响应**:
```json
{
  "success": true,
  "message": "消息删除成功"
}
```

### 9. 消息同步

**端点**: `GET /messages/sync`

**描述**: 同步指定时间后的消息，用于客户端离线后重连时获取最新消息

**认证**: 必需

**参数**:
- `last_sync_time`: 上次同步时间，ISO8601格式
- `user_id`: 用户ID

**响应**:
```json
{
  "success": true,
  "message": "消息同步成功",
  "data": {
    "messages": [
      // 消息列表，同Message对象格式
    ],
    "sync_time": "2025-04-10T10:35:00.000Z"
  }
}
```

## WebSocket API

WebSocket API 使用 Phoenix Channels 实现，提供实时消息推送和状态更新功能。

### 连接

**URL**: `/socket`

**参数**:
- `token`: 用户认证令牌

### 加入频道

**频道**: `user:{user_id}`

**参数**:
- `token`: 用户认证令牌

**示例**:
```javascript
socket.connect()
channel = socket.channel("user:123e4567-e89b-12d3-a456-426614174000", {token: authToken})
channel.join()
  .receive("ok", resp => { console.log("成功加入频道", resp) })
  .receive("error", resp => { console.log("加入频道失败", resp) })
```

### 消息事件

#### 1. 同步请求

**事件名**: `sync`

**数据**:
```json
{
  "last_sync_timestamp": "2025-04-10T10:30:00.000Z"
}
```

**响应**:
```json
{
  "events": [
    // 事件列表
  ],
  "new_last_sync_timestamp": "2025-04-10T10:35:00.000Z"
}
```

#### 2. 发送消息

**事件名**: `send_message`

**数据**:
```json
{
  "client_message_id": "uuid-client-generated",
  "conversation_id": "string",
  "content": {
    "text": "消息内容"
  },
  "message_type": "private_message",
  "content_type": "text",
  "client_timestamp": "2025-04-10T10:30:00.000Z"
}
```

**响应**:
```json
{
  "client_message_id": "uuid-client-generated",
  "message_id": "uuid-server-generated",
  "server_timestamp": "2025-04-10T10:30:01.000Z",
  "status": "sent"
}
```

#### 3. 标记消息已读

**事件名**: `mark_read`

**数据**:
```json
{
  "conversation_id": "string",
  "last_read_message_id": "uuid"
}
```

**响应**:
```json
{
  "updated_count": 5
}
```

#### 4. 撤回消息

**事件名**: `withdraw_message`

**数据**:
```json
{
  "message_id": "uuid"
}
```

**响应**:
```json
{
  "status": "withdrawn"
}
```

#### 5. 获取会话历史消息

**事件名**: `get_conversation_history`

**数据**:
```json
{
  "conversation_id": "string",
  "limit": 20,
  "before": "2025-04-10T10:30:00.000Z" // 可选
}
```

**响应**:
```json
{
  "messages": [
    // 消息列表
  ]
}
```

#### 6. 发起会话
**事件名**: `create_conversation`

**数据**:
```json
{
  "participant_ids": ["uuid1", "uuid2"],
  "type": "private | group"
}
```

**响应**:
```json
{
  "conversation_id": "string",
}
```

#### 7. 获取用户所有会话
**事件名**: `get_conversations`

**数据**:
```json
{
  "limit": 20,
  "offset": 0
}
```
<!-- %{
        conversation: c,
        unread_count: uc.unread_count,
        is_pinned: uc.is_pinned,
        is_muted: uc.is_muted,
        last_read_message_id: uc.last_read_message_id
      } -->
**响应**:
```json
{
  "conversations": [
    {
        "conversation": {
          "id": "uuid",
          "conversation_id": "string",
          "type": "private | group | system",
          "participant_ids": ["uuid1", "uuid2"],
          "last_message_id": "uuid",
          "last_message_content": "最后一条消息预览",
          "last_message_timestamp": "2025-04-10T10:30:00.000Z"
        },
        "unread_count": 5,
        "is_pinned": false,
        "is_muted": false,
        "last_read_message_id": "uuid"
    }
  ]
}
```



### 接收事件

#### 1. 新消息事件

**事件名**: `event`

**负载类型**: `new_message`

```json
{
  "id": "uuid",
  "event_type": "new_message",
  "payload": {
    // 消息对象
  },
  "event_timestamp": "2025-04-10T10:30:00.000Z",
  "target_user_id": "uuid"
}
```

#### 2. 消息状态变更事件

**事件名**: `event`

**负载类型**: `message_status_update`

```json
{
  "id": "uuid",
  "event_type": "message_status_update",
  "payload": {
    "message_id": "uuid",
    "conversation_id": "string",
    "status": "read",
    "updated_at": "2025-04-10T10:31:00.000Z"
  },
  "event_timestamp": "2025-04-10T10:31:00.000Z",
  "target_user_id": "uuid"
}
```

#### 3. 消息撤回事件

**事件名**: `event`

**负载类型**: `message_recalled`

```json
{
  "id": "uuid",
  "event_type": "message_recalled",
  "payload": {
    "message_id": "uuid",
    "conversation_id": "string",
    "status": "withdrawn",
    "updated_at": "2025-04-10T10:32:00.000Z"
  },
  "event_timestamp": "2025-04-10T10:32:00.000Z",
  "target_user_id": "uuid"
}
```

## 客户端实现指南

### 消息同步流程

1. **初始化**:
   - 客户端应在本地存储一个`last_sync_timestamp`，初始为空或Unix起始时间

2. **连接时同步**:
   - 客户端连接WebSocket后，发送`sync`事件，携带`last_sync_timestamp`
   - 服务端返回期间的所有事件和新的`new_last_sync_timestamp`
   - 客户端处理事件并更新本地`last_sync_timestamp`

3. **实时消息处理**:
   - 当客户端收到WebSocket事件时，应立即处理并更新UI
   - 发送消息后应乐观地更新UI，并在收到确认后更新消息状态

4. **离线重连**:
   - 当客户端重新上线时，执行同步流程获取离线期间的消息
   - 如果同步失败，应当使用HTTP API获取最近消息

### 消息状态管理

客户端需要正确处理以下消息状态转换:

1. **sending**: 消息正在发送中（仅客户端状态）
2. **sent**: 服务器已接收消息
3. **unread**: 消息未被接收者阅读
4. **read**: 消息已被接收者阅读
5. **withdrawn**: 消息已被撤回
6. **deleted**: 消息已被删除

### 注意事项

1. **消息ID管理**:
   - 客户端负责生成`client_message_id`并跟踪发送状态
   - 服务器生成`message_id`作为唯一标识

2. **会话管理**:
   - `conversation_id`格式:
     - 私聊: `private:{user1_id}:{user2_id}`（用户ID按字典序排列）
     - 系统通知: `system:notification:{user_id}`
     - 系统公告: `system:announcement:{user_id}`
     - 互动消息: `interaction:{user_id}`

3. **离线消息推送**:
   - 当目标用户不在线时，系统会通过外部推送服务发送通知
   - 客户端收到推送通知后应当触发同步流程

4. **消息撤回限制**:
   - 消息撤回有时间限制，目前为消息发送后2分钟内
   - 只有消息发送者可以撤回自己的消息
