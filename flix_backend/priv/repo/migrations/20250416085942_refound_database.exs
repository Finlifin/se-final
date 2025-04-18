defmodule FlixBackend.Repo.Migrations.RefoundDatabase do
  use Ecto.Migration

  def change do
    # 创建产品状态枚举
    execute """
    CREATE TYPE product_status AS ENUM ('available', 'sold', 'deleted', 'removed')
    """, "DROP TYPE product_status"

    # 创建订单状态枚举
    execute """
    CREATE TYPE order_status AS ENUM ('pending', 'payment_pending', 'paid', 'shipping', 'completed', 'cancelled', 'deleted', 'refunded')
    """, "DROP TYPE order_status"

    # 创建消息状态枚举
    execute """
    CREATE TYPE message_status AS ENUM ('sending', 'sent', 'unread', 'read', 'withdrawn', 'deleted')
    """, "DROP TYPE message_status"

    # 创建消息类型枚举
    execute """
    CREATE TYPE message_type AS ENUM ('system_notification', 'system_announcement', 'interaction', 'private_message')
    """, "DROP TYPE message_type"

    # 创建消息内容类型枚举
    execute """
    CREATE TYPE message_content_type AS ENUM ('text', 'image', 'product', 'order', 'comment', 'like', 'favorite', 'system')
    """, "DROP TYPE message_content_type"

    # 创建学校表
    create table(:schools, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :name, :string, null: false
      add :code, :string, null: false

      timestamps()
    end

    create unique_index(:schools, [:code])

    # 创建校区表
    create table(:campuses, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :name, :string, null: false
      add :address, :string, null: false
      add :school_id, references(:schools, type: :binary_id, on_delete: :delete_all), null: false

      timestamps()
    end

    create index(:campuses, [:school_id])

    # 创建用户表
    create table(:users, primary_key: false) do
      add :uid, :binary_id, primary_key: true
      add :phone_number, :string, null: false
      add :user_name, :string, null: false
      add :avatar_url, :string
      add :addresses, {:array, :string}, default: []
      add :current_address, :string
      add :balance, :integer, default: 0
      add :published_product_ids, {:array, :binary_id}, default: []
      add :sold_product_ids, {:array, :binary_id}, default: []
      add :purchased_product_ids, {:array, :binary_id}, default: []
      add :favorite_product_ids, {:array, :binary_id}, default: []

      # 学校和校区关联
      add :school_id, references(:schools, type: :binary_id, on_delete: :nilify_all)
      add :campus_id, references(:campuses, type: :binary_id, on_delete: :nilify_all)

      timestamps()
    end

    create unique_index(:users, [:phone_number])
    create index(:users, [:school_id])
    create index(:users, [:campus_id])

    # 创建账户表
    create table(:accounts, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :phone_number, :string, null: false
      add :hashed_password, :string
      add :role, :string, default: "user"
      add :user_id, references(:users, column: :uid, type: :binary_id, on_delete: :delete_all), null: false

      timestamps()
    end

    create unique_index(:accounts, [:phone_number])
    create index(:accounts, [:user_id])

    # 创建商品表
    create table(:products, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :seller_id, references(:users, column: :uid, type: :binary_id, on_delete: :delete_all), null: false
      add :title, :string, null: false
      add :description, :string
      add :price, :float, null: false
      add :images, {:array, :string}
      add :category, :string
      add :condition, :string
      add :location, :string
      add :post_time, :integer
      add :status, :product_status, default: "available"
      add :view_count, :integer, default: 0
      add :favorite_count, :integer, default: 0
      add :tags, {:array, :string}, default: []
      add :available_delivery_methods, {:array, :string}, default: []

      timestamps()
    end

    create index(:products, [:seller_id])
    create index(:products, [:category])
    create index(:products, [:status])

    # 创建订单表
    create table(:orders, primary_key: false) do
      add :order_id, :binary_id, primary_key: true
      add :buyer_id, references(:users, column: :uid, type: :binary_id, on_delete: :nilify_all), null: false
      add :seller_id, references(:users, column: :uid, type: :binary_id, on_delete: :nilify_all), null: false
      add :product_id, references(:products, type: :binary_id, on_delete: :nilify_all), null: false
      add :order_time, :integer
      add :price, :float, null: false
      add :status, :order_status, default: "pending"
      add :delivery_method, :string
      add :delivery_address, :string
      add :delivery_time, :integer
      add :delivery_fee, :float
      add :payment_method, :string
      add :payment_time, :integer

      timestamps()
    end

    create index(:orders, [:buyer_id])
    create index(:orders, [:seller_id])
    create index(:orders, [:product_id])
    create index(:orders, [:status])

    # 创建收藏表
    create table(:favorites, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :user_id, references(:users, column: :uid, type: :binary_id, on_delete: :delete_all), null: false
      add :product_id, references(:products, type: :binary_id, on_delete: :delete_all), null: false

      timestamps()
    end

    create unique_index(:favorites, [:user_id, :product_id], name: :favorites_user_id_product_id_index)
    create index(:favorites, [:user_id])
    create index(:favorites, [:product_id])

    # 创建会话表
    create table(:conversations, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :conversation_id, :string, null: false
      add :type, :string, null: false  # private, group
      add :participant_ids, {:array, :binary_id}, null: false
      add :last_message_id, :binary_id
      add :last_message_content, :string
      add :last_message_timestamp, :utc_datetime_usec

      timestamps()
    end

    create unique_index(:conversations, [:conversation_id])
    create index(:conversations, [:participant_ids], using: "GIN")

    # 创建用户会话关联表
    create table(:user_conversations, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :user_id, :binary_id, null: false
      add :conversation_id, :string, null: false
      add :last_read_message_id, :binary_id
      add :unread_count, :integer, default: 0
      add :is_pinned, :boolean, default: false
      add :is_muted, :boolean, default: false
      add :draft, :string

      timestamps()
    end

    create unique_index(:user_conversations, [:user_id, :conversation_id])
    create index(:user_conversations, [:conversation_id])

    # 创建消息表
    create table(:messages, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :client_message_id, :string, null: false
      add :conversation_id, :string, null: false
      add :sender_id, :binary_id, null: false
      add :receiver_id, :binary_id
      add :content, {:array, :map}, default: []
      add :message_type, :message_type, null: false
      add :status, :message_status, default: "unread"
      add :reference_id, :binary_id
      add :server_timestamp, :utc_datetime_usec
      add :client_timestamp, :utc_datetime_usec

      timestamps()
    end

    create index(:messages, [:conversation_id])
    create index(:messages, [:sender_id])
    create index(:messages, [:receiver_id])
    create index(:messages, [:status])
    create index(:messages, [:message_type])

    # 创建事件表
    create table(:events, primary_key: false) do
      add :id, :binary_id, primary_key: true
      add :event_type, :string, null: false
      add :payload, :map, null: false
      add :event_timestamp, :utc_datetime_usec, null: false
      add :target_user_id, :binary_id, null: false

      timestamps()
    end

    create index(:events, [:target_user_id])
    create index(:events, [:event_type])
    create index(:events, [:event_timestamp])
  end
end
