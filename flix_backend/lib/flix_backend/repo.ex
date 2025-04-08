defmodule FlixBackend.Repo do
  use Ecto.Repo,
    otp_app: :flix_backend,
    adapter: Ecto.Adapters.Postgres
end
