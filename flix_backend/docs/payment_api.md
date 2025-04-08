# 支付系统API文档

本文档描述了Flix平台支付系统的API接口设计。所有API均需要用户认证。

## 基础信息

- 基础URL: `/api/v1/payment`
- 认证方式: Bearer Token
- 响应格式: JSON

## API端点

### 1. 创建支付订单

**端点**: `POST /api/v1/payment/create`

**描述**: 根据订单ID创建支付订单，设置支付方式和配送信息

**请求体**:
```json
{
  "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
  "payment_method": "alipay",
  "delivery_method": "express",
  "delivery_address": "北京市海淀区中关村123号"
}
```

**响应**:
```json
{
  "data": {
    "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
    "amount": 105.5,
    "payment_method": "alipay",
    "payment_url": "https://example.com/pay/3a7acb88-52d3-4a5e-a45b-3b81faf618d5"
  }
}
```

**错误响应**:
```json
{
  "error": "订单不存在或不属于当前用户"
}
```

### 2. 获取支付订单状态

**端点**: `GET /api/v1/payment/:order_id/status`

**描述**: 获取指定订单的支付状态和信息

**URL参数**:
- `order_id`: 订单ID

**响应**:
```json
{
  "data": {
    "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
    "status": "payment_pending",
    "payment_method": "alipay",
    "payment_time": null,
    "total_amount": 105.5
  }
}
```

### 3. 支付成功回调

**端点**: `POST /api/v1/payment/callback`

**描述**: 支付平台回调接口，通知支付结果

**请求体**:
```json
{
  "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
  "payment_status": "success",
  "transaction_id": "pay_12345678"
}
```

**响应**:
```json
{
  "data": {
    "message": "支付成功"
  }
}
```

### 4. 获取支付方式列表

**端点**: `GET /api/v1/payment/methods`

**描述**: 获取系统支持的支付方式列表

**响应**:
```json
{
  "data": [
    {
      "id": "alipay",
      "name": "支付宝",
      "icon": "/images/alipay.png"
    },
    {
      "id": "wechat",
      "name": "微信支付",
      "icon": "/images/wechat.png"
    },
    {
      "id": "card",
      "name": "银行卡",
      "icon": "/images/card.png"
    },
    {
      "id": "wallet",
      "name": "钱包余额",
      "icon": "/images/wallet.png"
    }
  ]
}
```

### 5. 获取配送方式列表

**端点**: `GET /api/v1/payment/delivery_methods?product_id=:product_id`

**描述**: 获取指定商品支持的配送方式列表

**URL参数**:
- `product_id`: 商品ID

**响应**:
```json
{
  "data": [
    {
      "id": "express",
      "name": "快递配送",
      "icon": "/images/express.png",
      "base_fee": 10.0
    },
    {
      "id": "pickup",
      "name": "自提",
      "icon": "/images/pickup.png",
      "base_fee": 0.0
    },
    {
      "id": "same_day",
      "name": "当日达",
      "icon": "/images/same_day.png",
      "base_fee": 15.0
    }
  ]
}
```

### 6. 计算配送费用

**端点**: `POST /api/v1/payment/calculate_delivery_fee`

**描述**: 根据商品、配送方式和地址计算配送费用

**请求体**:
```json
{
  "product_id": "3f8b7a9c-52d3-4a5e-a45b-3b81faf618d5",
  "delivery_method": "express",
  "address": "北京市海淀区中关村123号"
}
```

**响应**:
```json
{
  "data": {
    "delivery_fee": 10.0
  }
}
```

### 7. 获取支付历史

**端点**: `GET /api/v1/payment/history?limit=10&offset=0`

**描述**: 获取用户的支付历史记录

**URL参数**:
- `limit`: 每页记录数，默认10
- `offset`: 起始位置，默认0

**响应**:
```json
{
  "data": {
    "orders": [
      {
        "order_id": "3a7acb88-52d3-4a5e-a45b-3b81faf618d5",
        "price": 95.5,
        "delivery_fee": 10.0,
        "payment_method": "alipay",
        "payment_time": 1682345678,
        "status": "paid"
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

### 8. 取消支付

**端点**: `DELETE /api/v1/payment/:order_id`

**描述**: 取消订单的支付流程

**URL参数**:
- `order_id`: 订单ID

**响应**:
```json
{
  "data": {
    "message": "支付已取消"
  }
}
```

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

1. 创建订单（使用订单API）
2. 创建支付订单（设置支付和配送信息）
3. 根据返回的支付URL进行支付
4. 支付完成后平台接收回调，更新订单状态
5. 前端轮询支付状态或通过WebSocket接收状态更新
