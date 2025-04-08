defmodule FlixBackend.Data.MessageStatus do
  use EctoEnum,
    type: :message_status,
    enums: [
      :unread,      # 未读
      :read,        # 已读
      :deleted      # 已删除
    ]
end
