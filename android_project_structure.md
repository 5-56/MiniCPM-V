# 羲和AI Android项目结构

## 项目目录结构
```
XiHeAI/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/xiehe/ai/
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── ai/
│   │   │   │   │   ├── MiniCPMClient.java
│   │   │   │   │   ├── AudioProcessor.java
│   │   │   │   │   ├── ImageProcessor.java
│   │   │   │   │   └── VideoProcessor.java
│   │   │   │   ├── ui/
│   │   │   │   │   ├── ChatActivity.java
│   │   │   │   │   ├── VoiceActivity.java
│   │   │   │   │   ├── CameraActivity.java
│   │   │   │   │   └── adapters/
│   │   │   │   │       └── MessageAdapter.java
│   │   │   │   ├── models/
│   │   │   │   │   ├── Message.java
│   │   │   │   │   ├── User.java
│   │   │   │   │   └── Conversation.java
│   │   │   │   ├── utils/
│   │   │   │   │   ├── FileUtils.java
│   │   │   │   │   ├── NetworkUtils.java
│   │   │   │   │   └── PermissionUtils.java
│   │   │   │   └── services/
│   │   │   │       ├── WebSocketService.java
│   │   │   │       └── AudioService.java
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   ├── activity_main.xml
│   │   │   │   │   ├── activity_chat.xml
│   │   │   │   │   ├── activity_voice.xml
│   │   │   │   │   ├── activity_camera.xml
│   │   │   │   │   ├── item_message.xml
│   │   │   │   │   └── fragment_voice_control.xml
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml
│   │   │   │   │   ├── colors.xml
│   │   │   │   │   └── styles.xml
│   │   │   │   ├── drawable/
│   │   │   │   │   ├── ic_voice.xml
│   │   │   │   │   ├── ic_camera.xml
│   │   │   │   │   ├── ic_send.xml
│   │   │   │   │   └── background_gradient.xml
│   │   │   │   └── raw/
│   │   │   │       ├── minicpm_v.tflite
│   │   │   │       └── minicpm_o.tflite
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 核心功能模块

### 1. AI模型集成模块 (ai/)
- **MiniCPMClient.java**: 主要的AI客户端，负责与MiniCPM模型交互
- **AudioProcessor.java**: 音频处理，包括录音、播放、格式转换
- **ImageProcessor.java**: 图像处理，包括拍照、选择、预处理
- **VideoProcessor.java**: 视频处理，包括录制、播放、帧提取

### 2. 用户界面模块 (ui/)
- **ChatActivity.java**: 主聊天界面，支持文本、图像、语音输入
- **VoiceActivity.java**: 语音对话界面，实时语音交互
- **CameraActivity.java**: 相机界面，支持拍照和录像
- **MessageAdapter.java**: 聊天消息适配器

### 3. 数据模型 (models/)
- **Message.java**: 消息数据模型
- **User.java**: 用户信息模型
- **Conversation.java**: 对话会话模型

### 4. 工具类 (utils/)
- **FileUtils.java**: 文件操作工具
- **NetworkUtils.java**: 网络请求工具
- **PermissionUtils.java**: 权限管理工具

### 5. 后台服务 (services/)
- **WebSocketService.java**: WebSocket连接服务
- **AudioService.java**: 音频处理后台服务