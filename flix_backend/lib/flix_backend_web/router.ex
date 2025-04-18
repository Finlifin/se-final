defmodule FlixBackendWeb.Router do
  use FlixBackendWeb, :router

  pipeline :browser do
    plug :accepts, ["html"]
    plug :fetch_session
    plug :fetch_live_flash
    plug :put_root_layout, html: {FlixBackendWeb.Layouts, :root}
    plug :protect_from_forgery
    plug :put_secure_browser_headers
  end

  pipeline :api do
    plug :accepts, ["json"]
  end

  # Guardian 认证管道
  pipeline :auth do
    plug FlixBackend.Guardian.AuthPipeline
  end

  scope "/", FlixBackendWeb do
    pipe_through :browser

    get "/", PageController, :home
  end

  # 公开API，不需要认证
  scope "/api/v1", FlixBackendWeb do
    pipe_through :api

    # 认证相关路由
    scope "/auth" do
      post "/send_sms", AuthController, :send_sms_code
      post "/verify_token", AuthController, :verify_token
      post "/login/sms", AuthController, :login_with_sms
      post "/login/password", AuthController, :login_with_password
    end

    scope "/products" do
      get "/", ProductController, :index
      get "/:id", ProductController, :show
    end

    # 消息相关公开API
    scope "/messages" do
      post "/sync", MessageController, :sync_messages
    end

    # 用户资料相关路由 - 公开API
    scope "/profile" do
      get "/abstract/:userId", ProfileController, :get_user_abstract
      get "/seller/:userId", ProfileController, :get_seller_profile
      get "/popular", ProfileController, :get_popular_sellers
      put "/update", ProfileController, :update_user_profile
      post "/recharge", ProfileController, :recharge_balance
      post "/avatar", ProfileController, :update_avatar
      get "/:userId/products", ProfileController, :get_user_products
      get "/:userId/sold", ProfileController, :get_user_sold_products
      get "/:userId/purchased", ProfileController, :get_user_purchased_products
      get "/:userId/favorites", ProfileController, :get_user_favorites
    end

    # 学校与校区相关公开API
    scope "/schools" do
      get "/", SchoolController, :index
      get "/search", SchoolController, :search # 新增搜索路由
      get "/:id", SchoolController, :show
      get "/:id/campuses", SchoolController, :list_campuses
    end

    scope "/campuses" do
      get "/", CampusController, :index # 公开获取所有校区列表
      get "/:id", CampusController, :show # 公开获取单个校区详情
    end
  end

  # 需要认证的API
  scope "/api/v1", FlixBackendWeb do
    pipe_through [:api, :auth]

    # 这里放置需要认证的资源路由
    scope "/products" do
      post "/publish", ProductController, :publish
      put "/:id", ProductController, :update
      delete "/:id", ProductController, :delete
      post "/:id/favorite", ProductController, :favorite
      delete "/:id/favorite", ProductController, :unfavorite
      get "/:id/is_favorite", ProductController, :is_favorite
      get "/favorites", ProductController, :favorites
    end

    scope "/profile" do
      get "/user/:userId", ProfileController, :get_user_profile
    end

    # 学校与校区相关需认证API
    scope "/schools" do
      post "/", SchoolController, :create
      put "/:id", SchoolController, :update
      delete "/:id", SchoolController, :delete
      post "/:id/campuses", SchoolController, :add_campus
    end

    scope "/campuses" do
      put "/:id", CampusController, :update
      delete "/:id", CampusController, :delete
    end

    # 会话相关路由
    scope "/conversations" do
      # 获取所有会话列表
      get "/", ConversationController, :index
      # 获取指定会话详情
      get "/:id", ConversationController, :show
      # 创建新会话
      post "/", ConversationController, :create
      # 更新会话设置（用户级别：置顶、静音等）
      put "/:id", ConversationController, :update
      # 删除会话（对当前用户）
      delete "/:id", ConversationController, :delete
      # 标记会话为已读
      put "/:id/read", ConversationController, :mark_read
    end

    # 消息相关路由
    scope "/messages" do
      # 查询消息列表（支持分页、过滤消息类型和状态）
      get "/", MessageController, :index
      # 获取未读消息数统计（总数及各类型）
      get "/unread_count", MessageController, :unread_count
      # 获取指定消息详情
      get "/:id", MessageController, :show
      # 发送新消息
      post "/", MessageController, :create
      # 标记单条消息为已读
      put "/:id/read", MessageController, :mark_as_read
      # 批量标记消息为已读
      put "/batch_read", MessageController, :batch_read
      # 标记所有消息为已读
      put "/read_all", MessageController, :mark_all_as_read
      # 删除消息
      delete "/:id", MessageController, :delete
    end

    # 系统公告路由
    scope "/announcements" do
      # 获取系统公告列表
      get "/", AnnouncementController, :index
      # 获取单条系统公告详情
      get "/:id", AnnouncementController, :show
    end

    # 支付相关路由
    scope "/payment" do
      # 创建支付订单
      post "/create", PaymentController, :create_payment
      # 获取支付订单状态
      get "/:order_id/status", PaymentController, :payment_status
      # 支付成功回调
      post "/callback", PaymentController, :payment_callback
      # 获取支付方式列表
      get "/methods", PaymentController, :list_payment_methods
      # 获取配送方式列表
      get "/delivery_methods", PaymentController, :list_delivery_methods
      # 计算配送费用
      post "/calculate_delivery_fee", PaymentController, :calculate_delivery_fee
      # 获取用户支付历史
      get "/history", PaymentController, :payment_history
      # 取消支付
      delete "/:order_id", PaymentController, :cancel_payment
    end

    # 订单相关路由
    scope "/orders" do
      # 获取订单列表
      get "/", OrderController, :index
      # 获取订单详情
      get "/:id", OrderController, :show
      # 创建订单
      post "/", OrderController, :create
      # 更新订单状态
      put "/:id/status", OrderController, :update_status
      # 取消订单
      delete "/:id", OrderController, :cancel
    end
  end

  # Other scopes may use custom stacks.
  # scope "/api", FlixBackendWeb do
  #   pipe_through :api
  # end

  # Enable LiveDashboard and Swoosh mailbox preview in development
  if Application.compile_env(:flix_backend, :dev_routes) do
    # If you want to use the LiveDashboard in production, you should put
    # it behind authentication and allow only admins to access it.
    # If your application does not have an admins-only section yet,
    # you can use Plug.BasicAuth to set up some basic authentication
    # as long as you are also using SSL (which you should anyway).
    import Phoenix.LiveDashboard.Router

    scope "/dev" do
      pipe_through :browser

      live_dashboard "/dashboard", metrics: FlixBackendWeb.Telemetry
      forward "/mailbox", Plug.Swoosh.MailboxPreview
    end
  end
end
