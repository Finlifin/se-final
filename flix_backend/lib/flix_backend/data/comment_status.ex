defmodule FlixBackend.Data.CommentStatus do
  use EctoEnum, type: :comment_status, enums: [:active, :deleted, :hidden]
end
