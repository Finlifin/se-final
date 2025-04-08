defmodule FlixBackend.Data.OrderStatus do
  use Ecto.Type

 @allowed_values ~w(pending paid shipped received completed cancelled)a

 def type, do: :string

 def cast(value) when value in @allowed_values, do: {:ok, value}
 def cast(_), do: :error

 def load(value) when value in @allowed_values, do: {:ok, value}
 def load(_), do: :error

 def dump(value) when value in @allowed_values, do: {:ok, value}
 def dump(_), do: :error
end
