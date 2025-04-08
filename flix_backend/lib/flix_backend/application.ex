defmodule FlixBackend.Application do
  # See https://hexdocs.pm/elixir/Application.html
  # for more information on OTP Applications
  @moduledoc false

  use Application

  @impl true
  def start(_type, _args) do
    children = [
      FlixBackendWeb.Telemetry,
      FlixBackend.Repo,
      {DNSCluster, query: Application.get_env(:flix_backend, :dns_cluster_query) || :ignore},
      {Phoenix.PubSub, name: FlixBackend.PubSub},
      # Start the Finch HTTP client for sending emails
      {Finch, name: FlixBackend.Finch},
      # Start a worker by calling: FlixBackend.Worker.start_link(arg)
      # {FlixBackend.Worker, arg},
      # Start to serve requests, typically the last entry
      FlixBackendWeb.Endpoint
    ]

    # See https://hexdocs.pm/elixir/Supervisor.html
    # for other strategies and supported options
    opts = [strategy: :one_for_one, name: FlixBackend.Supervisor]
    Supervisor.start_link(children, opts)
  end

  # Tell Phoenix to update the endpoint configuration
  # whenever the application is updated.
  @impl true
  def config_change(changed, _new, removed) do
    FlixBackendWeb.Endpoint.config_change(changed, removed)
    :ok
  end
end
