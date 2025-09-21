package com.xiehe.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.xiehe.ai.ui.ChatActivity;
import com.xiehe.ai.ui.VoiceActivity;
import com.xiehe.ai.ui.CameraActivity;
import com.xiehe.ai.utils.PermissionUtils;

public class MainActivity extends AppCompatActivity {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private Button btnChat, btnVoice, btnCamera;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkPermissions();
    }
    
    private void initViews() {
        btnChat = findViewById(R.id.btn_chat);
        btnVoice = findViewById(R.id.btn_voice);
        btnCamera = findViewById(R.id.btn_camera);
        
        btnChat.setOnClickListener(v -> startActivity(new Intent(this, ChatActivity.class)));
        btnVoice.setOnClickListener(v -> startActivity(new Intent(this, VoiceActivity.class)));
        btnCamera.setOnClickListener(v -> startActivity(new Intent(this, CameraActivity.class)));
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
        };
        
        if (!PermissionUtils.hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
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
}