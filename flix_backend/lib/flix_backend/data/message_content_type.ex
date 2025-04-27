defmodule FlixBackend.Data.MessageContentType do
  use EctoEnum,
    type: :message_content_type,
    enums: [
      :text,        # 文本消息, {type: String, payload: String}
      :image,       # 图片消息, {type: String, payload: Url}
      :product,     # 商品信息, {type: String, payload: Product}
      :video,       # 视频消息, {type: String, payload: ""}, not used
      :audio,       # 音频消息, {type: String, payload: ""}, not used
      :order,       # 订单信息, {type: String, payload: {order: Order, status: OrderStatus}}
      :comment,     # 评论信息, {type: String, payload: ""}, not used
      :like,        # 点赞信息, {type: String, payload: ""}, not used
      :favorite,    # 收藏信息, {type: String, payload: { user: UserAbstract, product: Product }}
      :system       # 系统消息, {type: String, payload: { user: UserAbstract, product: Product }}
    ]
end
