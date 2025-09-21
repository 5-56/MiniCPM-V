package com.xiehe.ai.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xiehe.ai.models.Message;
import com.xiehe.ai.utils.NetworkUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MiniCPMClient {
    private static final String TAG = "MiniCPMClient";
    private static final String BASE_URL = "http://your-server-ip:32550"; // 替换为你的服务器地址
    private static final String WS_URL = "ws://your-server-ip:32550/ws/api/v1/stream";
    
    private Context context;
    private OkHttpClient httpClient;
    private WebSocket webSocket;
    private ExecutorService executor;
    private Gson gson;
    private String sessionId;
    private List<Message> conversationHistory;
    
    // 回调接口
    public interface ResponseCallback {
        void onTextResponse(String text);
        void onAudioResponse(byte[] audioData);
        void onError(String error);
    }
    
    public MiniCPMClient(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newCachedThreadPool();
        this.gson = new Gson();
        this.conversationHistory = new ArrayList<>();
        this.sessionId = generateSessionId();
    }
    
    // 发送文本消息
    public void sendTextMessage(String text, ResponseCallback callback) {
        executor.execute(() -> {
            try {
                Message message = new Message("user", text, null, null);
                conversationHistory.add(message);
                
                JsonObject request = createTextRequest(text);
                sendWebSocketMessage(request, callback);
            } catch (Exception e) {
                Log.e(TAG, "Error sending text message", e);
                callback.onError("发送消息失败: " + e.getMessage());
            }
        });
    }
    
    // 发送图像消息
    public void sendImageMessage(Bitmap image, String text, ResponseCallback callback) {
        executor.execute(() -> {
            try {
                String base64Image = bitmapToBase64(image);
                Message message = new Message("user", text, base64Image, null);
                conversationHistory.add(message);
                
                JsonObject request = createImageRequest(base64Image, text);
                sendWebSocketMessage(request, callback);
            } catch (Exception e) {
                Log.e(TAG, "Error sending image message", e);
                callback.onError("发送图像失败: " + e.getMessage());
            }
        });
    }
    
    // 发送音频消息
    public void sendAudioMessage(byte[] audioData, ResponseCallback callback) {
        executor.execute(() -> {
            try {
                String base64Audio = byteArrayToBase64(audioData);
                Message message = new Message("user", null, null, base64Audio);
                conversationHistory.add(message);
                
                JsonObject request = createAudioRequest(base64Audio);
                sendWebSocketMessage(request, callback);
            } catch (Exception e) {
                Log.e(TAG, "Error sending audio message", e);
                callback.onError("发送音频失败: " + e.getMessage());
            }
        });
    }
    
    // 发送视频消息
    public void sendVideoMessage(Bitmap[] videoFrames, String text, ResponseCallback callback) {
        executor.execute(() -> {
            try {
                List<String> base64Frames = new ArrayList<>();
                for (Bitmap frame : videoFrames) {
                    base64Frames.add(bitmapToBase64(frame));
                }
                
                Message message = new Message("user", text, base64Frames.toString(), null);
                conversationHistory.add(message);
                
                JsonObject request = createVideoRequest(base64Frames, text);
                sendWebSocketMessage(request, callback);
            } catch (Exception e) {
                Log.e(TAG, "Error sending video message", e);
                callback.onError("发送视频失败: " + e.getMessage());
            }
        });
    }
    
    // 创建WebSocket连接
    private void connectWebSocket(ResponseCallback callback) {
        Request request = new Request.Builder()
                .url(WS_URL + "?uid=" + sessionId)
                .build();
        
        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "Received: " + text);
                handleWebSocketMessage(text, callback);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "Received bytes: " + bytes.size());
                // 处理音频数据
                callback.onAudioResponse(bytes.toByteArray());
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + code + " " + reason);
            }
            
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + code + " " + reason);
            }
            
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error", t);
                callback.onError("连接失败: " + t.getMessage());
            }
        });
    }
    
    // 发送WebSocket消息
    private void sendWebSocketMessage(JsonObject message, ResponseCallback callback) {
        if (webSocket == null) {
            connectWebSocket(callback);
        }
        
        String messageStr = gson.toJson(message);
        webSocket.send(messageStr);
    }
    
    // 处理WebSocket响应
    private void handleWebSocketMessage(String message, ResponseCallback callback) {
        try {
            JsonObject response = gson.fromJson(message, JsonObject.class);
            if (response.has("choices")) {
                JsonObject choices = response.getAsJsonObject("choices");
                if (choices.has("text")) {
                    String text = choices.get("text").getAsString();
                    callback.onTextResponse(text);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing WebSocket message", e);
            callback.onError("解析响应失败: " + e.getMessage());
        }
    }
    
    // 创建文本请求
    private JsonObject createTextRequest(String text) {
        JsonObject request = new JsonObject();
        JsonObject messages = new JsonObject();
        JsonObject content = new JsonObject();
        content.addProperty("type", "text");
        content.addProperty("text", text);
        
        messages.addProperty("role", "user");
        messages.add("content", content);
        request.add("messages", messages);
        
        return request;
    }
    
    // 创建图像请求
    private JsonObject createImageRequest(String base64Image, String text) {
        JsonObject request = new JsonObject();
        JsonObject messages = new JsonObject();
        JsonObject content = new JsonObject();
        content.addProperty("type", "image_data");
        JsonObject imageData = new JsonObject();
        imageData.addProperty("data", base64Image);
        content.add("image_data", imageData);
        
        messages.addProperty("role", "user");
        messages.add("content", content);
        request.add("messages", messages);
        
        return request;
    }
    
    // 创建音频请求
    private JsonObject createAudioRequest(String base64Audio) {
        JsonObject request = new JsonObject();
        JsonObject messages = new JsonObject();
        JsonObject content = new JsonObject();
        content.addProperty("type", "input_audio");
        JsonObject audioData = new JsonObject();
        audioData.addProperty("data", base64Audio);
        audioData.addProperty("format", "wav");
        content.add("input_audio", audioData);
        
        messages.addProperty("role", "user");
        messages.add("content", content);
        request.add("messages", messages);
        
        return request;
    }
    
    // 创建视频请求
    private JsonObject createVideoRequest(List<String> base64Frames, String text) {
        JsonObject request = new JsonObject();
        JsonObject messages = new JsonObject();
        JsonObject content = new JsonObject();
        content.addProperty("type", "video_data");
        JsonObject videoData = new JsonObject();
        videoData.addProperty("frames", gson.toJson(base64Frames));
        videoData.addProperty("text", text);
        content.add("video_data", videoData);
        
        messages.addProperty("role", "user");
        messages.add("content", content);
        request.add("messages", messages);
        
        return request;
    }
    
    // 工具方法
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
    }
    
    private String byteArrayToBase64(byte[] data) {
        return android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
    }
    
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis();
    }
    
    // 清理资源
    public void cleanup() {
        if (webSocket != null) {
            webSocket.close(1000, "Normal closure");
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}