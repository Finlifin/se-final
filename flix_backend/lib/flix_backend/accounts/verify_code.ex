defmodule FlixBackend.Accounts.VerifyCode do
  # 模拟验证码登录, mock, 都给他通过
  def verify(_phone_number, _code) do
    # 模拟，90%的概率通过
    if :rand.uniform() < 0.9 do
      {:ok, :verified}
    else
      {:error, :invalid_or_expired_code}
    end
  end

  def generate_and_send(_phone_number) do
    # 模拟发送验证码, mock, 都给他通过
    {:ok, :sent}
  end

  # # 假设使用一个简单的内存存储 (实际应用中应使用更可靠的存储)
  # @store Agent.start_link(fn -> %{} end, name: :verify_code_store)

  # @code_ttl 60_000 # 验证码有效期 (60秒)

  # def generate(phone_number) do
  #     code = :rand.uniform(999_999) |> to_string() |> String.pad_leading(6, "0") # 生成6位随机数字
  #     Agent.update(@store, &Map.put(&1, phone_number, {code, System.monotonic_time(:millisecond)}))
  #     # TODO: 发送短信验证码到 phone_number
  #     {:ok, code}
  # end
  # def verify(phone_number, code) do
  #     case Agent.get(@store, &Map.get(&1, phone_number)) do
  #     {stored_code, timestamp} when stored_code == code and System.monotonic_time(:millisecond) - timestamp <= @code_ttl ->
  #         Agent.update(@store, &Map.delete(&1, phone_number)) # 验证成功后删除验证码
  #         {:ok, code}

  #     _ ->
  #         {:error, :invalid_or_expired_code}
  #     end
  # end
end
