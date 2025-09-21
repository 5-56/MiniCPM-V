# Android本地模型集成方案

## 1. 模型转换

### 转换为TensorFlow Lite格式
```python
# convert_to_tflite.py
import torch
from transformers import AutoModel, AutoTokenizer
import tensorflow as tf

def convert_minicpm_to_tflite():
    # 加载模型
    model = AutoModel.from_pretrained('openbmb/MiniCPM-V-4_5', trust_remote_code=True)
    model.eval()
    
    # 创建示例输入
    dummy_input = torch.randn(1, 3, 224, 224)  # 图像输入
    
    # 转换为ONNX
    torch.onnx.export(
        model,
        dummy_input,
        "minicpm_v.onnx",
        export_params=True,
        opset_version=11,
        do_constant_folding=True,
        input_names=['input'],
        output_names=['output']
    )
    
    # 转换为TensorFlow Lite
    converter = tf.lite.TFLiteConverter.from_onnx_model("minicpm_v.onnx")
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    
    tflite_model = converter.convert()
    
    # 保存模型
    with open("minicpm_v.tflite", "wb") as f:
        f.write(tflite_model)

if __name__ == "__main__":
    convert_minicpm_to_tflite()
```

## 2. Android TensorFlow Lite集成

### 模型加载类
```java
// TensorFlowLiteModel.java
package com.xiehe.ai.ai;

import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TensorFlowLiteModel {
    private Interpreter tflite;
    private Context context;
    
    // 模型输入输出配置
    private static final int INPUT_SIZE = 224;
    private static final int PIXEL_SIZE = 3;
    private static final int IMAGE_MEAN = 128;
    private static final float IMAGE_STD = 128.0f;
    
    public TensorFlowLiteModel(Context context) {
        this.context = context;
        loadModel();
    }
    
    private void loadModel() {
        try {
            tflite = new Interpreter(loadModelFile("minicpm_v.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(context.getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelPath).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    
    public String processImage(Bitmap bitmap) {
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(bitmap);
        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(4 * 1000); // 假设输出1000个token
        outputBuffer.order(ByteOrder.nativeOrder());
        
        tflite.run(inputBuffer, outputBuffer);
        
        return processOutput(outputBuffer);
    }
    
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        
        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / IMAGE_STD);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / IMAGE_STD);
                byteBuffer.putFloat((val & 0xFF) / IMAGE_STD);
            }
        }
        return byteBuffer;
    }
    
    private String processOutput(ByteBuffer outputBuffer) {
        // 处理模型输出，转换为文本
        outputBuffer.rewind();
        // 这里需要根据实际模型输出格式进行解析
        return "模型输出结果";
    }
    
    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
```

## 3. 模型优化

### 量化配置
```python
# quantize_model.py
import tensorflow as tf

def quantize_model():
    # 加载原始模型
    converter = tf.lite.TFLiteConverter.from_saved_model("minicpm_model")
    
    # 量化配置
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    
    # 代表性数据集
    def representative_dataset():
        for _ in range(100):
            data = np.random.random((1, 224, 224, 3)).astype(np.float32)
            yield [data]
    
    converter.representative_dataset = representative_dataset
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    converter.inference_input_type = tf.uint8
    converter.inference_output_type = tf.uint8
    
    # 转换
    quantized_model = converter.convert()
    
    # 保存
    with open("minicpm_v_quantized.tflite", "wb") as f:
        f.write(quantized_model)
```

## 4. 内存管理

### 模型缓存策略
```java
// ModelCache.java
package com.xiehe.ai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ModelCache {
    private static final String PREFS_NAME = "model_cache";
    private static final String MODEL_VERSION_KEY = "model_version";
    private static final String CURRENT_VERSION = "1.0.0";
    
    private Context context;
    private SharedPreferences prefs;
    
    public ModelCache(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean isModelCached() {
        String cachedVersion = prefs.getString(MODEL_VERSION_KEY, "");
        return CURRENT_VERSION.equals(cachedVersion) && 
               new File(getModelPath()).exists();
    }
    
    public void cacheModel(InputStream modelStream) throws IOException {
        File modelFile = new File(getModelPath());
        modelFile.getParentFile().mkdirs();
        
        FileOutputStream fos = new FileOutputStream(modelFile);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = modelStream.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.close();
        
        prefs.edit().putString(MODEL_VERSION_KEY, CURRENT_VERSION).apply();
    }
    
    private String getModelPath() {
        return context.getFilesDir() + "/models/minicpm_v.tflite";
    }
}
```

## 5. 性能监控

### 推理性能监控
```java
// PerformanceMonitor.java
package com.xiehe.ai.utils;

import android.util.Log;
import java.util.concurrent.TimeUnit;

public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    
    public static class Timer {
        private long startTime;
        private String operation;
        
        public Timer(String operation) {
            this.operation = operation;
            this.startTime = System.nanoTime();
        }
        
        public void end() {
            long duration = System.nanoTime() - startTime;
            long millis = TimeUnit.NANOSECONDS.toMillis(duration);
            Log.d(TAG, operation + " took " + millis + "ms");
        }
    }
    
    public static Timer startTimer(String operation) {
        return new Timer(operation);
    }
}
```

## 6. 错误处理

### 模型加载异常处理
```java
// ModelException.java
package com.xiehe.ai.exceptions;

public class ModelException extends Exception {
    public enum ErrorType {
        LOAD_FAILED,
        INFERENCE_FAILED,
        MEMORY_INSUFFICIENT,
        INVALID_INPUT
    }
    
    private ErrorType errorType;
    
    public ModelException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
}
```

## 7. 使用示例

### 在Activity中使用本地模型
```java
// LocalModelActivity.java
public class LocalModelActivity extends AppCompatActivity {
    private TensorFlowLiteModel localModel;
    private PerformanceMonitor.Timer timer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            localModel = new TensorFlowLiteModel(this);
        } catch (Exception e) {
            Log.e("LocalModel", "Failed to load model", e);
            // 回退到服务器模式
            switchToServerMode();
        }
    }
    
    private void processImage(Bitmap image) {
        if (localModel != null) {
            timer = PerformanceMonitor.startTimer("Local Inference");
            try {
                String result = localModel.processImage(image);
                // 处理结果
                handleResult(result);
            } catch (Exception e) {
                Log.e("LocalModel", "Inference failed", e);
                // 回退到服务器模式
                switchToServerMode();
            } finally {
                timer.end();
            }
        }
    }
    
    private void switchToServerMode() {
        // 切换到服务器模式
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("use_server", true);
        startActivity(intent);
    }
}
```

## 注意事项

1. **模型大小**: MiniCPM模型较大，需要确保设备有足够存储空间
2. **内存限制**: Android应用内存限制，需要优化模型加载
3. **性能**: 本地推理可能较慢，建议在后台线程执行
4. **电池消耗**: 本地推理会消耗更多电池
5. **更新**: 模型更新需要重新下载和安装