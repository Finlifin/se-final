# API文档

## 概述

Flix API是符合OpenAPI规范的RESTful API，为客户端提供数据访问和业务操作能力。本文档详细描述了API的结构、认证机制、请求/响应格式以及各端点的功能和用法。

## 基础信息

- **基础URL**: `https://api.flix.example.com`
- **版本**: v1
- **内容类型**: `application/json`

## 认证

大多数API端点需要认证才能访问。认证通过Bearer令牌（JWT）实现。

### 获取令牌

```
POST /api/auth/login
```

请求体:
```json
{
  "phone": "11451419191",
  "password": "password123"
}
```

或使用验证码登录:
```json
{
  "phone": "11451419191",
  "code": "123456"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbG...",
    "user": {
      "id": "12345",
      "username": "张三",
      "phone": "11451419191",
      "avatar": "avatar.jpg"
    }
  }
}
```

### 使用认证

在后续请求中，将令牌添加到HTTP请求头:

```
Authorization: Bearer eyJhbG...
```

## 通用响应格式

所有API响应都采用统一格式:

### 成功响应

```json
{
  "success": true,
  "data": { /* 响应数据 */ },
  "meta": { /* 元数据，如分页信息 */ }
}
```

### 错误响应

```json
{
  "success": false,
  "error": {
    "code": "error_code",
    "message": "错误描述信息"
  }
}
```

### 常见错误代码

- `unauthorized`: 未授权（401）
- `forbidden`: 禁止访问（403）
- `not_found`: 资源不存在（404）
- `validation_error`: 验证错误（422）
- `internal_error`: 内部服务器错误（500）

## API端点

### 用户认证

#### 注册

```
POST /api/auth/register
```

请求体:
```json
{
  "phone": "11451419191",
  "code": "123456",
  "password": "password123",
  "username": "张三"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbG...",
    "user": {
      "id": "12345",
      "username": "张三",
      "phone": "11451419191"
    }
  }
}
```

#### 发送验证码

```
POST /api/auth/verify_code
```

请求体:
```json
{
  "phone": "11451419191"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "message": "验证码已发送",
    "expires_in": 300
  }
}
```

### 用户资料

#### 获取当前用户资料

```
GET /api/profile
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "12345",
    "username": "张三",
    "phone": "11451419191",
    "avatar": "avatar.jpg",
    "balance": 100.5,
    "addresses": ["北京市海淀区"],
    "school_id": "10001",
    "created_at": "2023-01-01T12:00:00Z"
  }
}
```

#### 更新用户资料

```
PUT /api/profile
```

请求体:
```json
{
  "username": "李四",
  "avatar": "new_avatar.jpg",
  "addresses": ["北京市海淀区", "北京市朝阳区"]
}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "12345",
    "username": "李四",
    "avatar": "new_avatar.jpg",
    "addresses": ["北京市海淀区", "北京市朝阳区"]
  }
}
```

#### 获取其他用户资料

```
GET /api/users/{id}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "12346",
    "username": "王五",
    "avatar": "avatar2.jpg",
    "products_count": 10,
    "followers_count": 5,
    "following": false,
    "rating": 4.5
  }
}
```

### 商品管理

#### 获取商品列表

```
GET /api/products
```

查询参数:
- `page`: 页码 (默认 1)
- `limit`: 每页数量 (默认 20)
- `category`: 商品分类
- `search`: 搜索关键词
- `seller_id`: 卖家ID
- `min_price`: 最低价格
- `max_price`: 最高价格
- `sort_by`: 排序字段 (price, created_at)
- `sort_order`: 排序方向 (asc, desc)
- `status`: 商品状态 (available, sold, reserved)

响应:
```json
{
  "success": true,
  "data": [
    {
      "id": "product1",
      "title": "iPhone 12",
      "description": "二手苹果手机",
      "price": 3800,
      "images": ["image1.jpg", "image2.jpg"],
      "category": "电子产品",
      "status": "available",
      "seller": {
        "id": "user1",
        "username": "张三",
        "avatar": "avatar.jpg"
      },
      "location": "北京市海淀区",
      "created_at": "2023-01-01T12:00:00Z",
      "views": 150,
      "favorites_count": 10
    },
    // 更多商品...
  ],
  "meta": {
    "page": 1,
    "limit": 20,
    "total": 50,
    "total_pages": 3
  }
}
```

#### 获取单个商品

```
GET /api/products/{id}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "product1",
    "title": "iPhone 12",
    "description": "二手苹果手机，九成新，已激活两个月。\n\n64GB 黑色，无划痕。\n\n送原装充电器和保护壳。",
    "price": 3800,
    "images": ["image1.jpg", "image2.jpg", "image3.jpg"],
    "category": "电子产品",
    "status": "available",
    "seller": {
      "id": "user1",
      "username": "张三",
      "avatar": "avatar.jpg",
      "rating": 4.8,
      "products_count": 15
    },
    "location": "北京市海淀区",
    "created_at": "2023-01-01T12:00:00Z",
    "views": 150,
    "favorites_count": 10
  }
}
```

#### 创建商品

```
POST /api/products
```

请求体:
```json
{
  "title": "MacBook Pro 2019",
  "description": "二手笔记本电脑，i5处理器，16GB内存",
  "price": 5800,
  "images": ["image1.jpg", "image2.jpg"],
  "category": "电子产品",
  "location": "北京市海淀区"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "product2",
    "title": "MacBook Pro 2019",
    "description": "二手笔记本电脑，i5处理器，16GB内存",
    "price": 5800,
    "images": ["image1.jpg", "image2.jpg"],
    "category": "电子产品",
    "status": "available",
    "seller": {
      "id": "user1",
      "username": "张三",
      "avatar": "avatar.jpg"
    },
    "location": "北京市海淀区",
    "created_at": "2023-01-10T14:30:00Z",
    "views": 0,
    "favorites_count": 0
  }
}
```

#### 更新商品

```
PUT /api/products/{id}
```

请求体:
```json
{
  "title": "MacBook Pro 2019 (降价)",
  "price": 5500,
  "status": "available"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "product2",
    "title": "MacBook Pro 2019 (降价)",
    "price": 5500,
    "status": "available",
    // 其他字段...
  }
}
```

#### 删除商品

```
DELETE /api/products/{id}
```

响应:
```json
{
  "success": true,
  "data": {
    "message": "商品已删除"
  }
}
```

### 收藏管理

#### 添加收藏

```
POST /api/favorites
```

请求体:
```json
{
  "product_id": "product1"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "favorite1",
    "product_id": "product1",
    "created_at": "2023-01-15T09:20:00Z"
  }
}
```

#### 获取收藏列表

```
GET /api/favorites
```

响应:
```json
{
  "success": true,
  "data": [
    {
      "id": "favorite1",
      "created_at": "2023-01-15T09:20:00Z",
      "product": {
        "id": "product1",
        "title": "iPhone 12",
        "price": 3800,
        "images": ["image1.jpg"],
        "status": "available"
      }
    },
    // 更多收藏...
  ],
  "meta": {
    "page": 1,
    "limit": 20,
    "total": 5,
    "total_pages": 1
  }
}
```

#### 删除收藏

```
DELETE /api/favorites/{product_id}
```

响应:
```json
{
  "success": true,
  "data": {
    "message": "收藏已删除"
  }
}
```

### 订单管理

#### 创建订单

```
POST /api/orders
```

请求体:
```json
{
  "product_id": "product1",
  "shipping_address": "北京市海淀区",
  "shipping_method": "face_to_face",
  "note": "下午三点后可面交"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "order1",
    "product_id": "product1",
    "product": {
      "id": "product1",
      "title": "iPhone 12",
      "price": 3800,
      "image": "image1.jpg"
    },
    "seller": {
      "id": "user1",
      "username": "张三"
    },
    "amount": 3800,
    "status": "pending",
    "shipping_address": "北京市海淀区",
    "shipping_method": "face_to_face",
    "note": "下午三点后可面交",
    "created_at": "2023-01-20T11:30:00Z"
  }
}
```

#### 获取订单列表

```
GET /api/orders
```

查询参数:
- `page`: 页码
- `limit`: 每页数量
- `role`: 角色 (buyer, seller)
- `status`: 订单状态

响应:
```json
{
  "success": true,
  "data": [
    {
      "id": "order1",
      "product": {
        "id": "product1",
        "title": "iPhone 12",
        "image": "image1.jpg"
      },
      "amount": 3800,
      "status": "pending",
      "created_at": "2023-01-20T11:30:00Z",
      "counterparty": {
        "id": "user1",
        "username": "张三",
        "avatar": "avatar.jpg"
      }
    },
    // 更多订单...
  ],
  "meta": {
    "page": 1,
    "limit": 20,
    "total": 3,
    "total_pages": 1
  }
}
```

#### 获取订单详情

```
GET /api/orders/{id}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "order1",
    "product": {
      "id": "product1",
      "title": "iPhone 12",
      "description": "二手苹果手机",
      "price": 3800,
      "image": "image1.jpg"
    },
    "seller": {
      "id": "user1",
      "username": "张三",
      "avatar": "avatar.jpg",
      "phone": "11451419191"
    },
    "buyer": {
      "id": "user2",
      "username": "李四",
      "avatar": "avatar2.jpg",
      "phone": "13812345678"
    },
    "amount": 3800,
    "status": "pending",
    "shipping_address": "北京市海淀区",
    "shipping_method": "face_to_face",
    "note": "下午三点后可面交",
    "payment_method": null,
    "payment_status": null,
    "created_at": "2023-01-20T11:30:00Z",
    "updated_at": "2023-01-20T11:30:00Z"
  }
}
```

#### 更新订单状态

```
PUT /api/orders/{id}/status
```

请求体:
```json
{
  "status": "shipped"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "order1",
    "status": "shipped",
    "updated_at": "2023-01-21T10:15:00Z"
  }
}
```

### 支付系统

#### 创建支付

```
POST /api/payments
```

请求体:
```json
{
  "order_id": "order1",
  "payment_method": "alipay"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "payment1",
    "order_id": "order1",
    "amount": 3800,
    "status": "pending",
    "payment_method": "alipay",
    "payment_url": "https://payment.example.com/pay?id=xyz",
    "created_at": "2023-01-20T14:20:00Z"
  }
}
```

#### 获取支付状态

```
GET /api/payments/{id}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "payment1",
    "order_id": "order1",
    "amount": 3800,
    "status": "paid",
    "payment_method": "alipay",
    "paid_at": "2023-01-20T14:25:00Z"
  }
}
```

### 消息系统

#### 获取会话列表

```
GET /api/conversations
```

查询参数:
- `message_type`: 消息类型 (private_message, system_notification, system_announcement)

响应:
```json
{
  "success": true,
  "data": [
    {
      "id": "conv1",
      "counterparty": {
        "id": "user3",
        "username": "王五",
        "avatar": "avatar3.jpg"
      },
      "last_message": {
        "content": "请问什么时候可以交易？",
        "sender_id": "user3",
        "created_at": "2023-01-22T09:15:00Z"
      },
      "unread_count": 2
    },
    // 更多会话...
  ]
}
```

#### 获取消息列表

```
GET /api/conversations/{id}/messages
```

查询参数:
- `before`: 消息ID，获取此ID之前的消息
- `limit`: 每次获取的消息数量

响应:
```json
{
  "success": true,
  "data": [
    {
      "id": "msg3",
      "content": "请问什么时候可以交易？",
      "content_type": "text",
      "sender_id": "user3",
      "is_read": false,
      "created_at": "2023-01-22T09:15:00Z"
    },
    {
      "id": "msg2",
      "content": "好的，可以详细介绍一下商品状况吗？",
      "content_type": "text",
      "sender_id": "user3",
      "is_read": true,
      "created_at": "2023-01-22T09:10:00Z"
    },
    {
      "id": "msg1",
      "content": "您好，我对您发布的iPhone 12感兴趣",
      "content_type": "text",
      "sender_id": "user3",
      "is_read": true,
      "created_at": "2023-01-22T09:05:00Z"
    }
  ],
  "meta": {
    "has_more": false
  }
}
```

#### 发送消息

```
POST /api/conversations/{id}/messages
```

请求体:
```json
{
  "content": "可以的，手机屏幕没有划痕，电池健康度95%",
  "content_type": "text"
}
```

响应:
```json
{
  "success": true,
  "data": {
    "id": "msg4",
    "content": "可以的，手机屏幕没有划痕，电池健康度95%",
    "content_type": "text",
    "sender_id": "user1",
    "is_read": false,
    "created_at": "2023-01-22T09:20:00Z"
  }
}
```

#### 标记为已读

```
PUT /api/conversations/{id}/read
```

响应:
```json
{
  "success": true,
  "data": {
    "unread_count": 0
  }
}
```

#### 获取未读消息计数

```
GET /api/messages/unread_counts
```

响应:
```json
{
  "success": true,
  "data": {
    "private_message": 2,
    "system_notification": 1,
    "system_announcement": 0,
    "total": 3
  }
}
```

### 文件上传

#### 上传图片

```
POST /api/upload/image
```

请求体:
- 使用multipart/form-data格式
- 字段名: `image`

响应:
```json
{
  "success": true,
  "data": {
    "url": "1f0b02c8a4a3547d870f4624e16fb92f3e56f16d061b0115d4eb4053063f9021.jpg",
    "mime_type": "image/jpeg",
    "size": 256789
  }
}
```

## WebSocket API

除了RESTful API外，系统还提供WebSocket实时消息API。

### 连接

```
ws://api.flix.example.com/socket
```

连接参数:
- `token`: 认证令牌（与RESTful API相同）

### 频道

用户连接后可以加入用户专属频道:

```
"user:{user_id}"
```

### 事件

#### 接收新消息

```json
{
  "event": "new_message",
  "payload": {
    "id": "msg5",
    "content": "明天下午可以面交吗？",
    "content_type": "text",
    "sender_id": "user3",
    "conversation_id": "conv1",
    "created_at": "2023-01-22T10:30:00Z"
  }
}
```

#### 订单状态更新

```json
{
  "event": "order_updated",
  "payload": {
    "id": "order1",
    "status": "paid",
    "updated_at": "2023-01-22T11:00:00Z"
  }
}
```

#### 系统通知

```json
{
  "event": "system_notification",
  "payload": {
    "id": "notif1",
    "title": "交易提醒",
    "content": "您的商品「iPhone 12」已售出",
    "created_at": "2023-01-22T11:05:00Z"
  }
}
```

## 错误处理

API可能返回以下HTTP状态码:

- `200 OK`: 请求成功
- `201 Created`: 资源创建成功
- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未授权
- `403 Forbidden`: 禁止访问
- `404 Not Found`: 资源不存在
- `422 Unprocessable Entity`: 请求格式正确但语义错误
- `429 Too Many Requests`: 请求频率超限
- `500 Internal Server Error`: 服务器错误

## API变更管理

API遵循语义化版本控制:

- 向下兼容的修改（如新增字段）：小版本更新
- 不兼容的修改（如更改字段类型）：主版本更新

版本变更将提前通知并提供迁移指南。