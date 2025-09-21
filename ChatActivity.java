package com.xiehe.ai.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.xiehe.ai.R;
import com.xiehe.ai.ai.MiniCPMClient;
import com.xiehe.ai.models.Message;
import com.xiehe.ai.ui.adapters.MessageAdapter;
import com.xiehe.ai.utils.PermissionUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements MiniCPMClient.ResponseCallback {
    
    private static final int REQUEST_IMAGE_CAPTURE = 1001;
    private static final int REQUEST_IMAGE_PICK = 1002;
    private static final int PERMISSION_REQUEST_CODE = 1003;
    
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private Button btnSend, btnVoice, btnCamera, btnGallery;
    private ImageView imagePreview;
    
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private MiniCPMClient miniCPMClient;
    private Bitmap currentImage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        initViews();
        setupRecyclerView();
        initMiniCPMClient();
        checkPermissions();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        btnSend = findViewById(R.id.btn_send);
        btnVoice = findViewById(R.id.btn_voice);
        btnCamera = findViewById(R.id.btn_camera);
        btnGallery = findViewById(R.id.btn_gallery);
        imagePreview = findViewById(R.id.image_preview);
        
        btnSend.setOnClickListener(v -> sendMessage());
        btnVoice.setOnClickListener(v -> startVoiceActivity());
        btnCamera.setOnClickListener(v -> captureImage());
        btnGallery.setOnClickListener(v -> pickImage());
        
        imagePreview.setOnClickListener(v -> clearImage());
    }
    
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);
        
        // 添加欢迎消息
        Message welcomeMessage = new Message("assistant", "你好！我是羲和AI，你的智能助手。我可以帮你处理文本、图像、语音等多种输入。有什么我可以帮助你的吗？", null, null);
        messageList.add(welcomeMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }
    
    private void initMiniCPMClient() {
        miniCPMClient = new MiniCPMClient(this);
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        };
        
        if (!PermissionUtils.hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }
    
    private void sendMessage() {
        String text = editTextMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text) && currentImage == null) {
            Toast.makeText(this, "请输入消息或选择图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 添加用户消息到列表
        Message userMessage = new Message("user", text, 
            currentImage != null ? bitmapToBase64(currentImage) : null, null);
        messageList.add(userMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
        
        // 清空输入
        editTextMessage.setText("");
        clearImage();
        
        // 发送到AI
        if (currentImage != null && !TextUtils.isEmpty(text)) {
            // 图像+文本消息
            miniCPMClient.sendImageMessage(currentImage, text, this);
        } else if (currentImage != null) {
            // 仅图像消息
            miniCPMClient.sendImageMessage(currentImage, "请描述这张图片", this);
        } else {
            // 仅文本消息
            miniCPMClient.sendTextMessage(text, this);
        }
        
        // 显示AI正在思考
        showTypingIndicator();
    }
    
    private void captureImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            return;
        }
        
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    
    private void pickImage() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK);
    }
    
    private void startVoiceActivity() {
        Intent intent = new Intent(this, VoiceActivity.class);
        startActivity(intent);
    }
    
    private void clearImage() {
        currentImage = null;
        imagePreview.setVisibility(View.GONE);
        imagePreview.setImageBitmap(null);
    }
    
    private void showTypingIndicator() {
        Message typingMessage = new Message("assistant", "AI正在思考...", null, null);
        messageList.add(typingMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        scrollToBottom();
    }
    
    private void removeTypingIndicator() {
        if (!messageList.isEmpty() && "AI正在思考...".equals(messageList.get(messageList.size() - 1).getText())) {
            messageList.remove(messageList.size() - 1);
            messageAdapter.notifyItemRemoved(messageList.size());
        }
    }
    
    private void scrollToBottom() {
        recyclerView.post(() -> {
            if (messageAdapter.getItemCount() > 0) {
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            }
        });
    }
    
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_CAPTURE:
                    if (data != null && data.getExtras() != null) {
                        Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                        if (imageBitmap != null) {
                            currentImage = imageBitmap;
                            imagePreview.setImageBitmap(imageBitmap);
                            imagePreview.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                    
                case REQUEST_IMAGE_PICK:
                    if (data != null && data.getData() != null) {
                        try {
                            Uri imageUri = data.getData();
                            Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                            if (imageBitmap != null) {
                                currentImage = imageBitmap;
                                imagePreview.setImageBitmap(imageBitmap);
                                imagePreview.setVisibility(View.VISIBLE);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "需要权限才能使用完整功能", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    // MiniCPMClient.ResponseCallback 实现
    @Override
    public void onTextResponse(String text) {
        runOnUiThread(() -> {
            removeTypingIndicator();
            Message aiMessage = new Message("assistant", text, null, null);
            messageList.add(aiMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom();
        });
    }
    
    @Override
    public void onAudioResponse(byte[] audioData) {
        // 处理音频响应（如果需要）
        runOnUiThread(() -> {
            removeTypingIndicator();
            // 这里可以播放音频或显示音频消息
            Toast.makeText(this, "收到音频响应", Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            removeTypingIndicator();
            Message errorMessage = new Message("assistant", "抱歉，发生了错误：" + error, null, null);
            messageList.add(errorMessage);
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            scrollToBottom();
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (miniCPMClient != null) {
            miniCPMClient.cleanup();
        }
    }
}