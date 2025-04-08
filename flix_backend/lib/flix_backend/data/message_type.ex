defmodule FlixBackend.Data.MessageType do
  use EctoEnum,
    type: :message_type,
    enums: [
      :system_notification,   # 系统通知
      :system_announcement,   # 系统公告
      :interaction,           # 互动消息
      :private_message        # 私信
    ]
end
