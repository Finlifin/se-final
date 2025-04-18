defmodule FlixBackendWeb.ApiResponse do
  @moduledoc """
  提供统一的API响应格式化
  """

  # 验证错误响应
  def validation_error_response(changeset) do
    errors =
      Ecto.Changeset.traverse_errors(changeset, fn {msg, opts} ->
        Enum.reduce(opts, msg, fn {key, value}, acc ->
          String.replace(acc, "%{#{key}}", to_string(value))
        end)
      end)

    %{
      success: false,
      message: "验证错误",
      errors: errors
    }
  end

  # 单个产品响应
  def product_response(product) do
    %{
      success: true,
      message: "获取产品成功",
      data: product
    }
  end

  # 产品列表响应
  def product_list_response(products, total_count, current_page, total_pages) do
    %{
      success: true,
      message: "获取产品列表成功",
      data: products,
      totalCount: total_count,
      currentPage: current_page,
      totalPages: total_pages
    }
  end

  # --- 学校相关响应 ---

  # 单个学校响应
  def school_response(school, message \\ "获取学校成功") do
    %{
      success: true,
      message: message,
      data: school
    }
  end

  # 学校列表响应
  def school_list_response(schools, total_count, current_page, total_pages) do
    %{
      success: true,
      message: "获取学校列表成功",
      data: schools,
      totalCount: total_count,
      currentPage: current_page,
      totalPages: total_pages
    }
  end

  # --- 校区相关响应 ---

  # 单个校区响应
  def campus_response(campus, message \\ "获取校区成功") do
    %{
      success: true,
      message: message,
      data: campus
    }
  end

  # 校区列表响应
  def campus_list_response(campuses, total_count, current_page, total_pages) do
    %{
      success: true,
      message: "获取校区列表成功",
      data: campuses,
      totalCount: total_count,
      currentPage: current_page,
      totalPages: total_pages
    }
  end

  # --- 认证相关响应 ---

  def login_success_response(token, account, user) do
    %{
      success: true,
      message: "登录成功",
      data: %{
        token: token,
        account: account,
        user: user,
      }
    }
  end

  def token_verified_response(claims, user_abstract) do
    %{
      success: true,
      message: "Token 有效",
      data: %{
        claims: claims,
        user: user_abstract
      }
    }
  end

  def sms_sent_response() do
    %{
      success: true,
      message: "验证码已发送"
    }
  end

  # --- 消息相关响应 ---
  def message_list_response(messages) do
    %{
      success: true,
      message: "获取消息列表成功",
      data: messages
    }
  end

  def message_response(message, message_text \\ "获取消息成功") do
    %{
      success: true,
      message: message_text,
      data: message
    }
  end

  def message_count_response(counts) do
     %{
       success: true,
       message: "获取未读消息数量成功",
       data: counts
     }
  end

  def message_sync_response(messages, sync_time) do
    %{
      success: true,
      message: "消息同步成功",
      data: %{
        messages: messages,
        sync_time: sync_time
      }
    }
  end

  # --- 订单相关响应 ---
  def order_list_response(orders, pagination) do
    %{
      success: true,
      message: "获取订单列表成功",
      data: %{
        orders: orders,
        pagination: pagination
      }
    }
  end

  def order_response(order, message \\ "获取订单成功") do
    %{
      success: true,
      message: message,
      data: order
    }
  end

  # --- 支付相关响应 ---
  def payment_info_response(payment_info) do
    %{
      success: true,
      message: "支付订单创建成功",
      data: payment_info
    }
  end

  def payment_status_response(payment_status) do
    %{
      success: true,
      message: "获取支付状态成功",
      data: payment_status
    }
  end

  def payment_history_response(history) do
    %{
      success: true,
      message: "获取支付历史成功",
      data: history
    }
  end

  def list_response(items, message) do
    %{
      success: true,
      message: message,
      data: items
    }
  end

  # --- 通用响应 ---
  def success_response(message, data \\ nil) do
    base = %{success: true, message: message}
    if data, do: Map.put(base, :data, data), else: base
  end

  # 错误响应
  def error_response(reason, status \\ :bad_request) do
    %{
      success: false,
      error: %{
        code: Atom.to_string(status),
        message: reason_to_string(reason)
      }
    }
  end

  # 未授权响应
  def unauthorized_response(reason \\ "未授权") do
    %{
      success: false,
      error: %{
        code: "unauthorized",
        message: reason
      }
    }
  end

  # 未找到响应
  def not_found_response(reason \\ "资源未找到") do
    %{
      success: false,
      error: %{
        code: "not_found",
        message: reason
      }
    }
  end

  # 禁止访问响应
  def forbidden_response(reason \\ "禁止访问") do
    %{
      success: false,
      error: %{
        code: "forbidden",
        message: reason
      }
    }
  end

  # 内部服务器错误响应
  def internal_server_error_response(reason \\ "内部服务器错误") do
    %{
      success: false,
      error: %{
        code: "internal_server_error",
        message: reason_to_string(reason)
      }
    }
  end

  # 将错误原因转换为字符串
  defp reason_to_string(%Ecto.Changeset{} = changeset) do
    errors =
      Enum.map(changeset.errors, fn {field, {message, _opts}} ->
        "#{field}: #{message}"
      end)

    Enum.join(errors, ", ")
  end

  defp reason_to_string(:not_found), do: "资源未找到"
  defp reason_to_string(:unauthorized), do: "未授权"
  defp reason_to_string(:forbidden), do: "禁止访问"
  defp reason_to_string(reason) when is_binary(reason), do: reason
  defp reason_to_string(reason), do: inspect(reason)
end
