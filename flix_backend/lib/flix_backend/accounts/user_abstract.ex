defmodule FlixBackend.Accounts.UserAbstract do
  @derive {Jason.Encoder, only: [:uid, :user_name, :avatar_url]}

  defstruct [:uid, :user_name, :avatar_url]

  @doc """
  从完整的用户对象创建用户摘要对象
  """
  def from_user(user) do
    %__MODULE__{
      uid: user.uid,
      user_name: user.user_name,
      avatar_url: user.avatar_url
    }
  end
end
