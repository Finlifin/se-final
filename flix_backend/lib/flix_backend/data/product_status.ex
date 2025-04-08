defmodule FlixBackend.Data.ProductStatus do
  use Ecto.Type
  @allowed_values ~w(available sold deleted removed)a

  def type, do: :string

  # 从应用程序值转换为数据库值
  def cast(value) when is_atom(value) and value in @allowed_values, do: {:ok, value}
  def cast(value) when is_binary(value) do
    # 尝试将字符串转换为原子，如果是有效值
    try do
      atom_value = String.to_existing_atom(value)
      if atom_value in @allowed_values, do: {:ok, atom_value}, else: :error
    rescue
      ArgumentError -> :error
    end
  end
  def cast(_), do: :error

  # 从数据库值加载到应用程序值
  def load(value) when is_binary(value) do
    # 将存储的字符串值转换为原子
    try do
      atom_value = String.to_existing_atom(value)
      if atom_value in @allowed_values, do: {:ok, atom_value}, else: :error
    rescue
      ArgumentError -> :error
    end
  end
  def load(_), do: :error

  # 从应用程序值保存到数据库值
  def dump(value) when is_atom(value) and value in @allowed_values do
    # 将原子转换为字符串存储
    {:ok, Atom.to_string(value)}
  end
  def dump(value) when is_binary(value) do
    # 如果传入的是字符串，确保它对应于允许的值
    try do
      atom_value = String.to_existing_atom(value)
      if atom_value in @allowed_values, do: {:ok, value}, else: :error
    rescue
      ArgumentError -> :error
    end
  end
  def dump(_), do: :error
end
