import os
import hashlib
from flask import Flask, request, jsonify, send_from_directory

app = Flask(__name__)

# 确保上传目录存在
UPLOAD_FOLDER = './uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)

def calculate_xor_sum(file_data):
    return hashlib.sha256(file_data).hexdigest()

# 获取文件扩展名
def get_file_extension(filename):
    return os.path.splitext(filename)[1]

@app.route('/api/v1/images/upload', methods=['POST'])
def upload_image():
    if 'file' not in request.files:
        return jsonify({"success": False, "error": "No file part"}), 400
    
    file = request.files['file']
    
    if file.filename == '':
        return jsonify({"success": False, "error": "No selected file"}), 400
    
    # 读取文件数据
    file_data = file.read()
    
    # 计算文件的xor sum
    file_sum = calculate_xor_sum(file_data)
    
    # 获取文件扩展名
    extension = get_file_extension(file.filename)
    
    # 生成新的文件名
    new_filename = f"{file_sum}{extension}"
    file_path = os.path.join(UPLOAD_FOLDER, new_filename)
    
    # 检查文件是否已存在（去重）
    if not os.path.exists(file_path):
        # 保存文件
        with open(file_path, 'wb') as f:
            f.write(file_data)
    
    # 返回成功响应
    return jsonify({"success": True, "url": new_filename})

@app.route('/api/v1/images/<filename>', methods=['GET'])
def get_image(filename):
    # 检查文件是否存在
    file_path = os.path.join(UPLOAD_FOLDER, filename)
    if not os.path.exists(file_path):
        return jsonify({"success": False, "error": "Image not found"}), 404
    
    # 返回图片文件
    return send_from_directory(UPLOAD_FOLDER, filename)

def main():
    # 启动Flask应用
    app.run(host='0.0.0.0', port=5000)

if __name__ == "__main__":
    main()
