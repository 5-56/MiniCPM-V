# MiniCPM服务器端部署指南

## 1. 服务器环境要求

### 硬件要求
- **GPU**: NVIDIA RTX 4090 或 A100 (24GB+ VRAM)
- **CPU**: 16核心以上
- **内存**: 32GB+ RAM
- **存储**: 100GB+ SSD

### 软件环境
```bash
# Python 3.8+
python --version

# CUDA 11.8+
nvidia-smi

# 安装依赖
pip install torch==2.1.2 torchvision==0.16.2
pip install transformers==4.44.2
pip install accelerate==0.30.1
pip install fastapi==0.104.1
pip install uvicorn==0.24.0
pip install websockets==12.0
pip install librosa==0.10.1
pip install soundfile==0.12.1
pip install pillow==10.1.0
pip install opencv-python==4.8.1.78
```

## 2. 模型部署

### 下载模型
```bash
# 创建模型目录
mkdir -p /opt/minicpm/models
cd /opt/minicpm/models

# 下载MiniCPM-o 2.6模型
git lfs install
git clone https://huggingface.co/openbmb/MiniCPM-o-2_6

# 下载MiniCPM-V 4.5模型
git clone https://huggingface.co/openbmb/MiniCPM-V-4_5
```

### 配置服务器
```python
# config.py
import os

# 模型配置
MODEL_CONFIG = {
    "minicpm_o_path": "/opt/minicpm/models/MiniCPM-o-2_6",
    "minicpm_v_path": "/opt/minicpm/models/MiniCPM-V-4_5",
    "device": "cuda:0",
    "torch_dtype": "bfloat16",
    "max_memory": "20GB"
}

# 服务器配置
SERVER_CONFIG = {
    "host": "0.0.0.0",
    "port": 32550,
    "max_workers": 4,
    "timeout": 300
}

# API配置
API_CONFIG = {
    "max_request_size": 100 * 1024 * 1024,  # 100MB
    "rate_limit": 100,  # 每分钟100个请求
    "cors_origins": ["*"]
}
```

### 启动服务器
```bash
# 启动MiniCPM-o服务器
cd /workspace
python web_demos/minicpm-o_2.6/model_server.py --port 32550 --model /opt/minicpm/models/MiniCPM-o-2_6

# 启动MiniCPM-V服务器
python web_demos/web_demo_2.6.py --port 32551 --model /opt/minicpm/models/MiniCPM-V-4_5
```

## 3. Docker部署（可选）

### Dockerfile
```dockerfile
FROM nvidia/cuda:11.8-devel-ubuntu20.04

# 安装Python和依赖
RUN apt-get update && apt-get install -y \
    python3.8 \
    python3.8-pip \
    git \
    wget \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 复制代码
COPY . /app/

# 安装Python依赖
RUN pip3 install -r requirements_o2.6.txt

# 下载模型
RUN python3 -c "from transformers import AutoModel; AutoModel.from_pretrained('openbmb/MiniCPM-o-2_6', trust_remote_code=True)"

# 暴露端口
EXPOSE 32550

# 启动命令
CMD ["python3", "web_demos/minicpm-o_2.6/model_server.py", "--port", "32550"]
```

### 构建和运行
```bash
# 构建镜像
docker build -t minicpm-server .

# 运行容器
docker run --gpus all -p 32550:32550 minicpm-server
```

## 4. 负载均衡配置

### Nginx配置
```nginx
upstream minicpm_backend {
    server 127.0.0.1:32550;
    server 127.0.0.1:32551;
}

server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://minicpm_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 5. 监控和日志

### 系统监控
```bash
# 安装监控工具
pip install prometheus-client
pip install psutil

# 监控脚本
python monitoring.py
```

### 日志配置
```python
# logging_config.py
import logging
import logging.handlers

def setup_logging():
    # 创建日志目录
    os.makedirs('/var/log/minicpm', exist_ok=True)
    
    # 配置日志
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.handlers.RotatingFileHandler(
                '/var/log/minicpm/server.log',
                maxBytes=100*1024*1024,  # 100MB
                backupCount=5
            ),
            logging.StreamHandler()
        ]
    )
```

## 6. 安全配置

### API密钥认证
```python
# auth.py
import hashlib
import time

API_KEYS = {
    "your-api-key-1": "client-1",
    "your-api-key-2": "client-2"
}

def verify_api_key(api_key: str) -> bool:
    return api_key in API_KEYS
```

### 防火墙配置
```bash
# 只允许特定端口
ufw allow 22    # SSH
ufw allow 80    # HTTP
ufw allow 443   # HTTPS
ufw allow 32550 # MiniCPM API
ufw enable
```

## 7. 性能优化

### GPU优化
```python
# 启用混合精度
torch.backends.cuda.matmul.allow_tf32 = True
torch.backends.cudnn.allow_tf32 = True

# 内存优化
torch.cuda.empty_cache()
```

### 模型量化
```python
# 量化模型以节省内存
from transformers import BitsAndBytesConfig

quantization_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_compute_dtype=torch.bfloat16,
    bnb_4bit_use_double_quant=True,
    bnb_4bit_quant_type="nf4"
)
```