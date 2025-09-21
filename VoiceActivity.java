package com.xiehe.ai.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.xiehe.ai.R;
import com.xiehe.ai.ai.MiniCPMClient;
import com.xiehe.ai.utils.PermissionUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VoiceActivity extends AppCompatActivity implements MiniCPMClient.ResponseCallback {
    
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private Button btnVoice, btnStop, btnClear;
    private TextView textStatus, textTranscript;
    private ImageView imageVoiceWave;
    
    private MiniCPMClient miniCPMClient;
    private AudioRecord audioRecord;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private Handler handler;
    private Runnable waveAnimation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);
        
        initViews();
        initMiniCPMClient();
        initHandler();
        checkPermissions();
    }
    
    private void initViews() {
        btnVoice = findViewById(R.id.btn_voice_start);
        btnStop = findViewById(R.id.btn_voice_stop);
        btnClear = findViewById(R.id.btn_voice_clear);
        textStatus = findViewById(R.id.text_voice_status);
        textTranscript = findViewById(R.id.text_voice_transcript);
        imageVoiceWave = findViewById(R.id.image_voice_wave);
        
        btnVoice.setOnClickListener(v -> startVoiceRecording());
        btnStop.setOnClickListener(v -> stopVoiceRecording());
        btnClear.setOnClickListener(v -> clearTranscript());
        
        updateUI();
    }
    
    private void initMiniCPMClient() {
        miniCPMClient = new MiniCPMClient(this);
    }
    
    private void initHandler() {
        handler = new Handler(Looper.getMainLooper());
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        };
        
        if (!PermissionUtils.hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }
    
    private void startVoiceRecording() {
        if (isRecording) return;
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "需要麦克风权限", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            audioRecord.startRecording();
            isRecording = true;
            updateUI();
            startWaveAnimation();
            
            // 开始录音线程
            new Thread(this::recordAudio).start();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "录音启动失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopVoiceRecording() {
        if (!isRecording) return;
        
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        
        stopWaveAnimation();
        updateUI();
    }
    
    private void recordAudio() {
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        
        while (isRecording && audioRecord != null) {
            int bytesRead = audioRecord.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                audioBuffer.write(buffer, 0, bytesRead);
            }
        }
        
        if (audioBuffer.size() > 0) {
            byte[] audioData = audioBuffer.toByteArray();
            runOnUiThread(() -> {
                textStatus.setText("正在处理语音...");
                miniCPMClient.sendAudioMessage(audioData, this);
            });
        }
    }
    
    private void startWaveAnimation() {
        waveAnimation = new Runnable() {
            private int frame = 0;
            @Override
            public void run() {
                if (isRecording) {
                    // 简单的波形动画
                    float scale = 1.0f + 0.3f * (float) Math.sin(frame * 0.3);
                    imageVoiceWave.setScaleX(scale);
                    imageVoiceWave.setScaleY(scale);
                    frame++;
                    handler.postDelayed(this, 50);
                }
            }
        };
        handler.post(waveAnimation);
    }
    
    private void stopWaveAnimation() {
        if (waveAnimation != null) {
            handler.removeCallbacks(waveAnimation);
            imageVoiceWave.setScaleX(1.0f);
            imageVoiceWave.setScaleY(1.0f);
        }
    }
    
    private void clearTranscript() {
        textTranscript.setText("");
        textStatus.setText(getString(R.string.voice_ready));
    }
    
    private void updateUI() {
        if (isRecording) {
            btnVoice.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            textStatus.setText(getString(R.string.voice_listening));
            imageVoiceWave.setVisibility(View.VISIBLE);
        } else {
            btnVoice.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            textStatus.setText(getString(R.string.voice_ready));
            imageVoiceWave.setVisibility(View.GONE);
        }
    }
    
    private void playAudio(byte[] audioData) {
        if (isPlaying) return;
        
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            
            // 创建临时文件
            java.io.File tempFile = java.io.File.createTempFile("audio", ".wav", getCacheDir());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            fos.write(audioData);
            fos.close();
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                runOnUiThread(() -> {
                    textStatus.setText(getString(R.string.voice_ready));
                    btnVoice.setEnabled(true);
                });
            });
            
            isPlaying = true;
            textStatus.setText(getString(R.string.voice_speaking));
            btnVoice.setEnabled(false);
            mediaPlayer.start();
            
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "音频播放失败", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "需要麦克风权限才能进行语音交互", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    // MiniCPMClient.ResponseCallback 实现
    @Override
    public void onTextResponse(String text) {
        runOnUiThread(() -> {
            textTranscript.setText(text);
            textStatus.setText("语音识别完成");
        });
    }
    
    @Override
    public void onAudioResponse(byte[] audioData) {
        runOnUiThread(() -> {
            playAudio(audioData);
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            textStatus.setText("错误：" + error);
            Toast.makeText(this, "语音处理失败：" + error, Toast.LENGTH_LONG).show();
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVoiceRecording();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        if (miniCPMClient != null) {
            miniCPMClient.cleanup();
        }
        if (handler != null && waveAnimation != null) {
            handler.removeCallbacks(waveAnimation);
        }
    }
}