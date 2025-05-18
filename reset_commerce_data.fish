#!/usr/bin/env fish

# 重置电商数据脚本
# - 将所有商品状态设置为available
# - 清空用户的售出列表和购买列表
# - 清空所有订单

echo "=== 开始重置电商数据 ==="

# 进入后端项目目录
cd /home/fin/software_engineering_final/flix_backend

# 将所有商品状态设置为available
echo "=== 重置所有商品状态为available ==="
mix run -e 'Enum.each(FlixBackend.Repo.all(FlixBackend.Data.Product), fn product -> 
  FlixBackend.Data.Product.changeset(product, %{status: :available}) |> FlixBackend.Repo.update() 
end)'

# 清空所有用户的售出列表和购买列表
echo "=== 清空用户的售出列表和购买列表 ==="
mix run -e 'Enum.each(FlixBackend.Repo.all(FlixBackend.Data.User), fn user -> 
  FlixBackend.Data.User.changeset(user, %{sold_product_ids: [], purchased_product_ids: []}) |> FlixBackend.Repo.update() 
end)'

# 清空所有订单
echo "=== 删除所有订单 ==="
mix run -e 'FlixBackend.Repo.delete_all(FlixBackend.Data.Order)'

echo "=== 数据重置完成 ==="