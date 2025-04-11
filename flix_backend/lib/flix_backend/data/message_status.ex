defmodule FlixBackend.Data.MessageStatus do
  use EctoEnum,
    type: :message_status,
    enums: [
      :sending,     # 发送中（仅客户端）
      :sent,        # 已发送
      :unread,      # 未读
      :read,        # 已读
      :withdrawn,   # 已撤回
      :deleted      # 已删除
    ]
end
