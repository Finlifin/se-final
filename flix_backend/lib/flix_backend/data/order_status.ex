defmodule FlixBackend.Data.OrderStatus do
  use EctoEnum.Postgres,
    type: :order_status,
    enums: [
      :pending,
      :payment_pending,
      :paid,
      :shipping,
      :completed,
      :cancelled,
      :deleted,
      :refunded
    ]
end
