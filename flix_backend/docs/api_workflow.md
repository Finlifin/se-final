# 电商平台支付与订单流程指南

本文档为前端开发人员提供完整的支付与订单流程指导，帮助理解整个业务流程及API调用顺序。

## 完整流程图

```
买家                           系统                           卖家
  |                             |                             |
  |   浏览商品                    |                             |
  |--------------------------->|                             |
  |                             |       发布商品                |
  |                             |<----------------------------|
  |                             |                             |
  |   创建订单                    |                             |
  |--------------------------->|                             |
  |   POST /api/v1/orders      |                             |
  |                             |                             |
  |   选择支付和配送方式            |                             |
  |--------------------------->|                             |
  |   POST /api/v1/payment/create |                          |
  |                             |                             |
  |   用户支付                    |                             |
  |--------------------------->|                             |
  |                             |     支付结果回调               |
  |                             |<---------------------------|
  |                             |                             |
  |   查询支付状态                 |                             |
  |--------------------------->|                             |
  |   GET /payment/:id/status  |                             |
  |                             |                             |
  |                             |       查看待发货订单            |
  |                             |<---------------------------|
  |                             |       GET /orders?status=paid |
  |                             |                             |
  |                             |       确认发货                |
  |                             |<---------------------------|
  |                             |       PUT /orders/:id/status |
  |                             |       {status: "shipping"}  |
  |                             |                             |
  |   查看订单状态                 |                             |
  |--------------------------->|                             |
  |   GET /orders/:id          |                             |
  |                             |                             |
  |   确认收货                    |                             |
  |--------------------------->|                             |
  |   PUT /orders/:id/status   |                             |
  |   {status: "completed"}    |                             |
  |                             |                             |
```

## 主要业务流程

### 1. 购买商品流程

1. **浏览商品**
   - `GET /api/v1/products`：获取商品列表
   - `GET /api/v1/products/:id`：获取商品详情

2. **创建订单**
   - `POST /api/v1/orders`：创建订单
   - 请求体需包含商品ID：`{ "product_id": "xxx" }`
   - 系统返回订单信息，包括订单ID

3. **选择支付方式和配送方式**
   - `GET /api/v1/payment/methods`：获取支持的支付方式
   - `GET /api/v1/payment/delivery_methods?product_id=xxx`：获取支持的配送方式
   - `POST /api/v1/payment/calculate_delivery_fee`：计算配送费用
   - `POST /api/v1/payment/create`：创建支付订单
   - 请求体需包含订单ID、支付方式、配送方式和地址

4. **用户支付**
   - 根据支付创建响应中的`payment_url`引导用户完成支付
   - 调用`GET /api/v1/payment/:order_id/status`轮询支付状态或使用WebSocket接收状态更新

5. **查看订单状态**
   - `GET /api/v1/orders/:id`：获取订单详情
   - `GET /api/v1/orders?limit=10&offset=0`：获取订单列表

6. **确认收货**
   - `PUT /api/v1/orders/:id/status`：更新订单状态为completed
   - 请求体需包含：`{ "status": "completed" }`

### 2. 销售商品流程

1. **发布商品**
   - `POST /api/v1/products/publish`：发布新商品
   - 设置`available_delivery_methods`字段指定支持的配送方式

2. **查看待发货订单**
   - `GET /api/v1/orders?role=seller&status=paid`：获取已支付待发货的订单

3. **确认发货**
   - `PUT /api/v1/orders/:id/status`：更新订单状态为shipping
   - 请求体需包含：`{ "status": "shipping" }`

4. **处理退款请求**
   - `PUT /api/v1/orders/:id/status`：更新订单状态为refunded
   - 请求体需包含：`{ "status": "refunded" }`

### 3. 订单取消流程

1. **买家取消未支付订单**
   - `DELETE /api/v1/orders/:id`：取消订单
   - 或使用 `PUT /api/v1/orders/:id/status` 将状态更新为cancelled

2. **买家取消待支付订单**
   - `DELETE /api/v1/payment/:order_id`：取消支付

3. **卖家取消订单**
   - `PUT /api/v1/orders/:id/status`：更新订单状态为cancelled
   - 请求体需包含：`{ "status": "cancelled" }`

## 注意事项

1. **认证要求**
   - 除浏览商品外，所有API都需要认证
   - 需在请求头中添加 `Authorization: Bearer <token>`

2. **订单状态转换规则**
   - 买家只能将订单状态从shipping改为completed，从pending或payment_pending改为cancelled
   - 卖家只能将订单状态从paid改为shipping，从pending/payment_pending/paid改为cancelled，从paid/shipping改为refunded

3. **支付超时处理**
   - 前端应实现支付超时处理逻辑，超时后可调用取消支付API

4. **错误处理**
   - 所有API调用应包含适当的错误处理逻辑
   - 检查HTTP状态码和响应中的错误信息

## 测试流程

建议按以下顺序进行端到端测试：

1. 卖家发布商品
2. 买家浏览并创建订单
3. 买家选择支付和配送方式
4. 模拟支付过程和回调
5. 卖家确认发货
6. 买家确认收货
7. 验证订单状态变更为completed

同时测试各种异常情况：
- 取消订单
- 支付失败
- 退款处理
