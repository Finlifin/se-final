defmodule FlixBackend.Data.MessageType do
  @moduledoc """
  消息类型枚举。

  定义了系统中所有可能的消息类型：
  - :system - 系统通知和系统公告
  - :notification - 通知类消息，如互动通知
  - :chat - 聊天消息，用户之间的私信
  - :order - 订单相关消息
  - :payment - 支付相关消息

  所有非用户发送的消息（sender 为 nil）均为服务器消息。
  """

  use EctoEnum,
    type: :message_type,
    enums: [
      :system,          # 系统通知
      :notification,    # 通知消息
      :chat,            # 聊天消息
      :order,           # 订单相关消息
      :payment          # 支付相关消息
    ]
end
