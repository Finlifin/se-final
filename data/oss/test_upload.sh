#!/bin/bash

# 创建测试图片文件
# echo "Creating test image file..."
# convert -size 100x100 xc:blue test_img.png

# 上传图片文件
echo "Uploading image file..."
RESPONSE=$(curl -s -X POST -F "file=@test_img.png" http://localhost:5000/api/v1/images/upload)

# 显示响应
echo "Server response:"
echo $RESPONSE

# 提取URL并下载图片进行验证
URL=$(echo $RESPONSE | grep -o '"url":"[^"]*"' | cut -d'"' -f4)

if [ ! -z "$URL" ]; then
  echo "Downloading uploaded image..."
  curl -s -o downloaded_img.png http://localhost:5000/api/v1/images/$URL
  
  echo "Comparing original and downloaded images..."
  DIFF=$(compare -metric AE test_img.png downloaded_img.png null: 2>&1)
  
  if [ "$DIFF" == "0" ]; then
    echo "Test PASSED: Images are identical"
  else
    echo "Test FAILED: Images are different"
  fi
else
  echo "Test FAILED: Could not extract URL from response"
fi

# 清理
echo "Cleaning up..."
rm -f test_img.png downloaded_img.png
