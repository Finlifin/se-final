# 订单系统API文档

本文档描述了Flix平台订单系统的API接口设计。所有API均需要用户认证。

## 基础信息

- 基础URL: `/api/v1/orders`
- 认证方式: Bearer Token
- 响应格式: JSON

## API端点

### 1. 获取订单列表

**端点**: `GET /api/v1/orders?limit=10&offset=0&role=buyer&status=pending`

**描述**: 获取当前用户的订单列表，支持分页和过滤

**URL参数**:
- `limit`: 每页记录数，默认10
- `offset`: 起始位置，默认0
- `role`: 角色，可选值为`buyer`（买家，默认）或`seller`（卖家）
- `status`: 订单状态，可选值包括`pending`、`payment_pending`、`paid`、`shipping`、`completed`、`cancelled`、`refunded`

**响应**:
```json
{
  "data": {
    "orders": [
      {
        "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
        "buyer_id": "6d8f4c2e-1b9a-4c7d-8e5f-3a2b1c0d9e8f",
        "seller_id": "5c3b2a1d-9e8f-7g6h-5i4j-3k2l1m0n9o8p",
        "product_id": "3f8b7a9c-52d3-4a5e-a45b-3b81faf618d5",
        "order_time": 1682345678,
        "price": 95.5,
        "status": "pending",
        "delivery_method": null,
        "delivery_address": null,
        "delivery_time": null,
        "delivery_fee": null,
        "payment_method": null,
        "payment_time": null
      },
      // 更多订单...
    ],
    "pagination": {
      "total_count": 25,
      "limit": 10,
      "offset": 0
    }
  }
}
```

### 2. 获取订单详情

**端点**: `GET /api/v1/orders/:id`

**描述**: 获取指定订单的详细信息，包括关联的产品和用户信息

**URL参数**:
- `id`: 订单ID

**响应**:
```json
{
  "data": {
    "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
    "buyer_id": "6d8f4c2e-1b9a-4c7d-8e5f-3a2b1c0d9e8f",
    "seller_id": "5c3b2a1d-9e8f-7g6h-5i4j-3k2l1m0n9o8p",
    "product_id": "3f8b7a9c-52d3-4a5e-a45b-3b81faf618d5",
    "order_time": 1682345678,
    "price": 95.5,
    "status": "pending",
    "delivery_method": null,
    "delivery_address": null,
    "delivery_time": null,
    "delivery_fee": null,
    "payment_method": null,
    "payment_time": null,
    "product": {
      "id": "3f8b7a9c-52d3-4a5e-a45b-3b81faf618d5",
      "title": "iPhone 13 Pro",
      "description": "全新未拆封iPhone 13 Pro",
      "price": 95.5,
      "images": ["https://example.com/images/iphone13.jpg"],
      "category": "electronics",
      "condition": "new",
      "location": "北京",
      "tags": ["apple", "smartphone"],
      "available_delivery_methods": ["express", "pickup", "same_day"]
    },
    "buyer": {
      "uid": "6d8f4c2e-1b9a-4c7d-8e5f-3a2b1c0d9e8f",
      "username": "buyer123",
      "avatar": "https://example.com/avatars/buyer123.jpg"
    },
    "seller": {
      "uid": "5c3b2a1d-9e8f-7g6h-5i4j-3k2l1m0n9o8p",
      "username": "seller456",
      "avatar": "https://example.com/avatars/seller456.jpg"
    }
  }
}
```

### 3. 创建订单

**端点**: `POST /api/v1/orders`

**描述**: 创建新订单，商品加入购物车

**请求体**:
```json
{
  "product_id": "3f8b7a9c-52d3-4a5e-a45b-3b81faf618d5"
}
```

**响应**:
```json
{
  "data": {
    "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
    "buyer_id": "6d8f4c2e-1b9a-4c7d-8e5f-3a2b1c0d9e8f",
    "seller_id": "5c3b2a1d-9e8f-7g6h-5i4j-3k2l1m0n9o8p",
    "product_id": "3f8b7a9c-52d3-4a5e-a45b-3b81faf618d5",
    "order_time": 1682345678,
    "price": 95.5,
    "status": "pending"
  }
}
```

**错误响应**:
```json
{
  "error": "商品不存在"
}
```

或者

```json
{
  "error": "商品不可购买"
}
```

或者

```json
{
  "error": "不能购买自己的商品"
}
```

### 4. 更新订单状态

**端点**: `PUT /api/v1/orders/:id/status`

**描述**: 更新订单状态，不同角色有不同的权限

**URL参数**:
- `id`: 订单ID

**请求体**:
```json
{
  "status": "cancelled"
}
```

**响应**:
```json
{
  "data": {
    "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
    "status": "cancelled"
    // 其他订单属性...
  }
}
```

**错误响应**:
```json
{
  "error": "无效的状态变更"
}
```

### 5. 取消订单

**端点**: `DELETE /api/v1/orders/:id`

**描述**: 取消订单，实际上是对订单状态的更新

**URL参数**:
- `id`: 订单ID

**响应**:
```json
{
  "data": {
    "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
    "status": "cancelled"
    // 其他订单属性...
  }
}
```

## 订单状态流程

订单可能的状态及其转换规则：

1. `pending`: 初始状态，订单已创建但未发起支付
2. `payment_pending`: 已选择支付方式，等待支付
3. `paid`: 已支付，等待卖家发货
4. `shipping`: 卖家已发货，等待买家确认收货
5. `completed`: 买家已确认收货，订单完成
6. `cancelled`: 订单已取消
7. `refunded`: 订单已退款

状态转换权限：

- 买家可以将`pending`或`payment_pending`状态的订单改为`cancelled`
- 买家可以将`shipping`状态的订单改为`completed`
- 卖家可以将`paid`状态的订单改为`shipping`
- 卖家可以将`pending`、`payment_pending`或`paid`状态的订单改为`cancelled`
- 卖家可以将`paid`或`shipping`状态的订单改为`refunded`

## 状态码

- `200 OK`: 请求成功
- `201 Created`: 资源创建成功
- `204 No Content`: 请求成功但无返回内容
- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未认证或认证过期
- `403 Forbidden`: 无权限访问该资源
- `404 Not Found`: 请求的资源不存在
- `422 Unprocessable Entity`: 请求格式正确但语义错误

## 使用流程

1. 创建订单（买家）
2. 进入支付流程（使用支付API）
3. 卖家发货（更改状态为shipping）
4. 买家确认收货（更改状态为completed）
5. 如需退款，卖家可将订单改为refunded状态
