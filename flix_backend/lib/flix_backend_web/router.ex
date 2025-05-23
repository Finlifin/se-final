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
      post "/reset_password", AuthController, :reset_password  # 添加重置密码路由（不需要认证）
      post "/set_password", AuthController, :set_initial_password  # 添加设置初始密码路由（不需要认证，通过验证码验证）
    end

    scope "/products" do
      post "/", ProductController, :index
      get "/:id", ProductController, :show
    end

    # 评论相关公开API
    scope "/comments" do
      get "/product/:product_id", CommentController, :list_product_comments
      get "/:id/replies", CommentController, :list_comment_replies
      get "/:id", CommentController, :get_comment
      get "/:id/context", CommentController, :get_comment_with_context
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
      get "/:userId/products", ProfileController, :get_user_products
      get "/:userId/sold", ProfileController, :get_user_sold_products
      get "/:userId/purchased", ProfileController, :get_user_purchased_products
      get "/:userId/favorites", ProfileController, :get_user_favorites
    end

    # 学校与校区相关公开API
    scope "/schools" do
      get "/", SchoolController, :index
      get "/search", SchoolController, :search
      get "/:id", SchoolController, :show
      get "/:school_id/campuses", SchoolController, :get_campuses
    end

    scope "/campuses" do
      get "/", CampusController, :index
      get "/:id", CampusController, :show
    end
  end

  # 需要认证的API
  scope "/api/v1", FlixBackendWeb do
    pipe_through [:api, :auth]

    # 认证相关需要登录的接口
    scope "/auth" do
      post "/update_password", AuthController, :update_password  # 添加修改密码路由（需要认证）
      post "/set_password", AuthController, :set_initial_password  # 添加设置初始密码路由（已登录状态）
      get "/check_password", AuthController, :check_password_set  # 检查当前用户是否已设置密码
    end

    # 这里放置需要认证的资源路由
    scope "/products" do
      post "/publish", ProductController, :publish
      put "/:id", ProductController, :update
      delete "/:id", ProductController, :delete
      post "/:id/favorite", ProductController, :favorite
      delete "/:id/favorite", ProductController, :unfavorite
      get "/:id/is_favorite", ProductController, :is_favorite
    end

    # 评论相关需要认证的API
    scope "/comments" do
      post "/product/:product_id", CommentController, :create_comment
      post "/:comment_id/reply", CommentController, :reply_to_comment
      post "/:id/like", CommentController, :like_comment
      delete "/:id/like", CommentController, :unlike_comment
      get "/:id/liked", CommentController, :is_comment_liked
      delete "/:id", CommentController, :delete_comment
    end

    scope "/profile" do
      post "/recharge", ProfileController, :recharge_balance
      put "/update", ProfileController, :update_user_profile
      post "/avatar", ProfileController, :update_avatar
      get "/user/:userId", ProfileController, :get_user_profile
      get "/favorites", ProductController, :favorites
    end

    # 学校与校区相关需认证API（管理员操作）
    scope "/schools" do
      post "/", SchoolController, :create
      put "/:id", SchoolController, :update
      delete "/:id", SchoolController, :delete
    end

    scope "/campuses" do
      post "/", CampusController, :create
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
