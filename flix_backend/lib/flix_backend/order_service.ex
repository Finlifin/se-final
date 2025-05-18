defmodule FlixBackend.OrderService do
  import Ecto.Query
  alias FlixBackend.Repo
  alias FlixBackend.Data.{Order, Product, User}
  alias FlixBackend.Messaging

  @doc """
  获取订单列表
  """
  def list_orders(user_id, params) do
    # 分页参数
    limit = Map.get(params, :limit, 10)
    offset = Map.get(params, :offset, 0)

    # 过滤参数
    status = Map.get(params, :status)
    # buyer或seller
    role = Map.get(params, :role, "buyer")

    # 构建查询
    query =
      if role == "seller" do
        from o in Order,
          where: o.seller_id == ^user_id,
          order_by: [desc: o.inserted_at]
      else
        from o in Order,
          where: o.buyer_id == ^user_id,
          order_by: [desc: o.inserted_at]
      end

    # 添加状态过滤
    query =
      if status do
        status_atom = if is_atom(status), do: status, else: String.to_atom(status)
        from o in query, where: o.status == ^status_atom
      else
        query
      end

    # 添加分页
    query =
      from o in query,
        limit: ^limit,
        offset: ^offset

    # 执行查询
    orders = Repo.all(query)

    # 获取总数
    count_query =
      if role == "seller" do
        from o in Order, where: o.seller_id == ^user_id
      else
        from o in Order, where: o.buyer_id == ^user_id
      end

    count_query =
      if status do
        status_atom = if is_atom(status), do: status, else: String.to_atom(status)
        from o in count_query, where: o.status == ^status_atom
      else
        count_query
      end

    total_count = Repo.aggregate(count_query, :count, :order_id)

    # 构造响应数据
    pagination = %{
      total_count: total_count,
      limit: limit,
      offset: offset
    }

    {:ok, orders, pagination}
  end

  @doc """
  获取订单详情
  """
  def get_order(user_id, order_id) do
    # 查询订单，确保只能查看自己的订单（买家或卖家）
    query =
      from o in Order,
        where: o.order_id == ^order_id and (o.buyer_id == ^user_id or o.seller_id == ^user_id)

    case Repo.one(query) do
      nil ->
        {:error, :not_found, "订单不存在或无权查看"}

      order ->
        # 加载关联数据
        order =
          order
          |> Repo.preload(:product)
          |> Repo.preload(:buyer)
          |> Repo.preload(:seller)

        {:ok, order}
    end
  end

  @doc """
  创建订单
  """
  def create_order(user_id, product_id) do
    # 查询商品信息
    case Repo.get(Product, product_id) do
      nil ->
        {:error, :not_found, "商品不存在"}

      product ->
        # 检查商品是否可购买
        if product.status != :available do
          {:error, :bad_request, "商品不可购买"}
        else
          # 检查是否是自己的商品
          if product.seller_id == user_id do
            {:error, :bad_request, "不能购买自己的商品"}
          else
            # 创建订单
            current_time = :os.system_time(:second)

            order_params = %{
              buyer_id: user_id,
              seller_id: product.seller_id,
              product_id: product.id,
              order_time: current_time,
              price: product.price,
              status: :pending
            }

            changeset = Order.changeset(%Order{}, order_params)

            case Repo.insert(changeset) do
              {:ok, order} ->
                # 通知卖家有新订单

                notify_seller_new_order(order)

                {:ok, order}

              {:error, changeset} ->
                {:error, :validation_error, changeset}
            end
          end
        end
    end
  end

  @doc """
  更新订单状态
  """
  def update_order_status(user_id, order_id, status) do
    # 检查状态值是否合法
    valid_statuses = [
      :pending,
      :payment_pending,
      :paid,
      :shipping,
      :completed,
      :cancelled,
      :refunded
    ]

    new_status_atom =
      if is_atom(status) do
        status
      else
        try do
          String.to_atom(status)
        rescue
          _ -> nil
        end
      end

    if is_nil(new_status_atom) or !Enum.member?(valid_statuses, new_status_atom) do
      {:error, :bad_request, "无效的状态值"}
    else
      # 根据用户角色查询订单
      # 买家只能修改为取消（在支付前）或完成（在发货后）
      # 卖家可以修改为发货（在支付后）、取消（在发货前）或退款
      query =
        if new_status_atom == :cancelled || new_status_atom == :completed do
          from o in Order,
            where: o.order_id == ^order_id and (o.buyer_id == ^user_id or o.seller_id == ^user_id)
        else
          from o in Order,
            where: o.order_id == ^order_id and o.seller_id == ^user_id
        end

      case Repo.one(query) do
        nil ->
          {:error, :not_found, "订单不存在或无权修改"}

        order ->
          # 检查状态变更是否合法
          if !is_valid_status_transition(order.status, new_status_atom, user_id, order) do
            {:error, :bad_request, "无效的状态变更"}
          else
            # 处理订单完成的特殊情况
            result =
              if new_status_atom == :completed do
                complete_order_transaction(order)
              else
                # 更新订单状态
                changeset = Order.changeset(order, %{status: new_status_atom})
                Repo.update(changeset)
              end

            case result do
              {:ok, updated_order} ->
                # 发送状态变更通知
                send_status_change_notification(updated_order, user_id)

                {:ok, updated_order}

              {:error, _failed_operation, failed_value, _changes_so_far} ->
                {:error, :transaction_failed, "处理订单失败: " <> inspect(failed_value)}

              {:error, changeset} ->
                {:error, :validation_error, changeset}
            end
          end
      end
    end
  end

  @doc """
  取消订单
  """
  def cancel_order(user_id, order_id) do
    update_order_status(user_id, order_id, :cancelled)
  end

  # 完成订单事务
  defp complete_order_transaction(order) do
    Repo.transaction(fn ->
      # 1. 获取买家和卖家
      buyer = Repo.get(User, order.buyer_id)
      seller = Repo.get(User, order.seller_id)
      product = Repo.get(Product, order.product_id)

      # 确保所有实体都存在
      if is_nil(buyer) or is_nil(seller) or is_nil(product) do
        Repo.rollback("买家、卖家或商品不存在")
      end

      # 2. 更新卖家余额和已售出商品列表
      # 确保product.id不重复添加到sold_product_ids中
      updated_sold_products =
        if Enum.member?(seller.sold_product_ids || [], product.id) do
          seller.sold_product_ids
        else
          [product.id | seller.sold_product_ids || []]
        end

      seller_changeset =
        User.changeset(seller, %{
          balance: seller.balance + order.price,
          sold_product_ids: updated_sold_products
        })

      {:ok, _updated_seller} = Repo.update(seller_changeset)

      # 3. 更新买家的已购商品列表
      # 确保product.id不重复添加到purchased_product_ids中
      updated_purchased_products =
        if Enum.member?(buyer.purchased_product_ids || [], product.id) do
          buyer.purchased_product_ids
        else
          [product.id | buyer.purchased_product_ids || []]
        end

      buyer_changeset =
        User.changeset(buyer, %{
          purchased_product_ids: updated_purchased_products
        })

      {:ok, _updated_buyer} = Repo.update(buyer_changeset)

      # 4. 更新商品状态为已售出
      product_changeset = Product.changeset(product, %{status: :sold})
      {:ok, _updated_product} = Repo.update(product_changeset)

      # 5. 更新订单状态
      order_changeset = Order.changeset(order, %{status: :completed})
      {:ok, updated_order} = Repo.update(order_changeset)

      # 返回更新后的订单
      updated_order
    end)
  end

  @doc """
  Transactional update of seller's sold product list and buyer's purchased product list.
  This function is intended to be used within an Ecto.Multi chain.
  """
  def update_seller_and_buyer_product_lists_transactional(repo, order) do
    unless order.order_type == "product" && order.product_id do
      # Not a product order, or product_id is missing, so skip.
      # Return {:ok, order} to allow the Multi chain to proceed.
      {:ok, order}
    else
      buyer = repo.get(User, order.buyer_id)
      seller = repo.get(User, order.seller_id)
      product_id = order.product_id

      if is_nil(buyer) or is_nil(seller) do
        {:error, :user_not_found_for_list_update}
      else
        # Update seller's sold_product_ids
        new_seller_sold_ids = [product_id | (seller.sold_product_ids || [])] |> Enum.uniq()
        seller_changeset = User.changeset(seller, %{sold_product_ids: new_seller_sold_ids})

        # Update buyer's purchased_product_ids
        new_buyer_purchased_ids = [product_id | (buyer.purchased_product_ids || [])] |> Enum.uniq()
        buyer_changeset = User.changeset(buyer, %{purchased_product_ids: new_buyer_purchased_ids})

        # Chain the updates within the transaction
        with {:ok, _updated_seller} <- repo.update(seller_changeset),
             {:ok, _updated_buyer} <- repo.update(buyer_changeset) do
          {:ok, order}
        else
          failed_operation_result ->
            failed_operation_result
        end
      end
    end
  end

  # 私有辅助函数：检查状态变更是否合法
  defp is_valid_status_transition(current_status, new_status, user_id, order) do
    cond do
      # 买家可以取消未支付的订单
      user_id == order.buyer_id && current_status in [:pending, :payment_pending] &&
          new_status == :cancelled ->
        true

      # 买家可以确认收货完成订单
      user_id == order.buyer_id && current_status == :shipping && new_status == :completed ->
        true

      # 卖家可以发货已支付的订单
      user_id == order.seller_id && current_status == :paid && new_status == :shipping ->
        true

      # 卖家可以取消未发货的订单
      user_id == order.seller_id && current_status in [:pending, :payment_pending, :paid] &&
          new_status == :cancelled ->
        true

      # 卖家可以退款
      user_id == order.seller_id && current_status in [:paid, :shipping] &&
          new_status == :refunded ->
        true

      # 其他情况不允许变更
      true ->
        false
    end
  end

  # 通知卖家有新订单
  defp notify_seller_new_order(order) do
    seller = Repo.get(User, order.seller_id)
    buyer = Repo.get(User, order.buyer_id)
    product = Repo.get(Product, order.product_id)

    if seller && buyer && product do
      # 构造消息内容
      text_content ="#{buyer.user_name}创建了购买您商品\"#{product.title}\"的订单,订单号:#{order.order_id}"

      # 构造订单消息内容
      content = [
        %{type: "text", payload: text_content},
        %{type: "order", payload: order}
      ]

      # 发送私信
      message_id = "#{buyer.uid}-#{:os.system_time(:millisecond)}"
      Messaging.send_private_message(
        buyer.uid,  # 买家作为发送者
        seller.uid, # 卖家作为接收者
        content,
        message_id
      )
    end
  end

  # 发送状态变更通知
  defp send_status_change_notification(order, change_by_id) do
    seller = Repo.get(User, order.seller_id)
    buyer = Repo.get(User, order.buyer_id)
    product = Repo.get(Product, order.product_id)

    if seller && buyer && product do
      case order.status do
        :shipping when change_by_id == seller.uid ->
          # 卖家发货，通知买家
          text_content = "您购买的商品\"#{product.title}\"已发货,订单号:#{order.order_id}"

          content = [
            %{type: "text", payload: text_content},
            %{type: "order", payload: order}
          ]

          message_id = "#{seller.uid}-#{:os.system_time(:millisecond)}"
          Messaging.send_private_message(
            seller.uid, # 卖家作为发送者
            buyer.uid,  # 买家作为接收者
            content,
            message_id
          )

        :completed when change_by_id == buyer.uid ->
          # 买家确认收货，通知卖家
          text_content = "#{buyer.user_name}已确认收到商品\"#{product.title}\",交易已完成,订单号:#{order.order_id}"

          content = [
            %{type: "text", payload: text_content},
            %{type: "order", payload: order}
          ]

          # 向卖家发送消息
          message_id = "#{buyer.uid}-#{:os.system_time(:millisecond)}"
          Messaging.send_private_message(
            buyer.uid,  # 买家作为发送者
            seller.uid, # 卖家作为接收者
            content,
            message_id
          )

          # 同时通知买家
          buyer_text_content = "您已确认收到商品\"#{product.title}\",交易已完成,订单号:#{order.order_id}"

          buyer_content = [
            %{type: "text", payload: buyer_text_content},
            %{type: "order", payload: order}
          ]

          # 这里是系统向买家确认，可以用系统ID作为发送者
          message_id = "server-#{:os.system_time(:millisecond)}"
          Messaging.send_private_message(
            nil, # 系统消息
            buyer.uid,
            buyer_content,
            message_id
          )

        :cancelled ->
          # 取消订单，通知对方
          if change_by_id == buyer.uid do
            # 买家取消，通知卖家
            text_content = "#{buyer.user_name}已取消购买商品\"#{product.title}\"的订单,订单号:#{order.order_id}"

            content = [
              %{type: "text", payload: text_content},
              %{type: "order", payload: order}
            ]

            message_id = "#{buyer.uid}-#{:os.system_time(:millisecond)}"
            Messaging.send_private_message(
              buyer.uid,  # 买家作为发送者
              seller.uid, # 卖家作为接收者
              content,
              message_id
            )
          else
            # 卖家取消，通知买家
            text_content = "卖家已取消您购买商品\"#{product.title}\"的订单,订单号:#{order.order_id}"

            content = [
              %{type: "text", payload: text_content},
              %{type: "order", payload: order}
            ]

            message_id = "#{seller.uid}-#{:os.system_time(:millisecond)}"
            Messaging.send_private_message(
              seller.uid, # 卖家作为发送者
              buyer.uid,  # 买家作为接收者
              content,
              message_id
            )
          end

        :refunded when change_by_id == seller.uid ->
          # 卖家退款，通知买家
          text_content = "您购买的商品\"#{product.title}\"已被退款,订单号:#{order.order_id}"

          content = [
            %{type: "text", payload: text_content},
            %{type: "order", payload: order}
          ]

          message_id = "#{seller.uid}-#{:os.system_time(:millisecond)}"
          Messaging.send_private_message(
            seller.uid, # 卖家作为发送者
            buyer.uid,  # 买家作为接收者
            content,
            message_id
          )

        # 其他状态变更暂不处理
        _ ->
          nil
      end
    end
  end
end
