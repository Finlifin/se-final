defmodule FlixBackend.Data.MessageContentType do
  use EctoEnum,
    type: :message_content_type,
    enums: [
      :text,        # 文本消息
      :image,       # 图片消息
      :product,     # 商品信息
      :order,       # 订单信息
      :comment,     # 评论信息
      :like,        # 点赞信息
      :favorite,    # 收藏信息
      :system       # 系统消息
    ]
end
