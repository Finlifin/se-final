#!/bin/bash

# 设置基础 URL
BASE_URL="http://localhost:4000/api/v1"
CONTENT_TYPE="Content-Type: application/json"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Flix Backend API 测试脚本 ===${NC}\n"

# 1. 测试发送短信验证码
# echo -e "${BLUE}测试 1: 发送短信验证码${NC}"
# curl -s -X POST \
#   "${BASE_URL}/auth/send_sms" \
#   -H "${CONTENT_TYPE}" \
#   -d '{"phone_number": "13800138000"}' | jq .
# echo

# 2. 测试使用短信验证码登录（使用实际生成的验证码替换）
# echo -e "${BLUE}测试 2: 使用短信验证码登录${NC}"
# echo -e "${RED}注意: 请将代码中的 '123456' 替换为实际收到的验证码${NC}"
# curl -s -X POST \
#   "${BASE_URL}/auth/login/sms" \
#   -H "${CONTENT_TYPE}" \
#   -d '{"phone_number": "13800138000", "sms_code": "123456", "user_name": "步惊云"}' | jq .
# echo

# 3. 测试密码登录（使用已设置密码的帐户）
# echo -e "${BLUE}测试 3: 密码登录${NC}"
# curl -s -X POST \
#   "${BASE_URL}/auth/login/password" \
#   -H "${CONTENT_TYPE}" \
#   -d '{"phone_number": "13800138000", "password": "your_password"}' | jq .
# echo

# # 4. 测试错误的密码登录
# echo -e "${BLUE}测试 4: 错误的密码登录${NC}"
# curl -s -X POST \
#   "${BASE_URL}/auth/login/password" \
#   -H "${CONTENT_TYPE}" \
#   -d '{"phone_number": "13800138000", "password": "wrong_password"}' | jq .
# echo

# # 5. 测试不存在的手机号
# echo -e "${BLUE}测试 5: 不存在的手机号${NC}"
# curl -s -X POST \
#   "${BASE_URL}/auth/login/password" \
#   -H "${CONTENT_TYPE}" \
#   -d '{"phone_number": "13900000000", "password": "any_password"}' | jq .
# echo

# 6. 测试无效的手机号格式
# echo -e "${BLUE}测试 6: 无效的手机号格式${NC}"
# curl -s -X POST \
#   "${BASE_URL}/auth/send_sms" \
#   -H "${CONTENT_TYPE}" \
#   -d '{"phone_number": "123456"}' | jq .
# echo

# echo -e "${GREEN}测试完成!${NC}"
